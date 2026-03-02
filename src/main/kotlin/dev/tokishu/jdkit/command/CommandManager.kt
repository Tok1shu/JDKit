package dev.tokishu.jdkit.command

import dev.tokishu.jdkit.command.annotation.JDKitContextMenu
import dev.tokishu.jdkit.command.annotation.JDKitSubcommand
import dev.tokishu.jdkit.command.annotation.cooldown.CooldownInterceptor
import dev.tokishu.jdkit.command.annotation.owner.OwnerInterceptor
import dev.tokishu.jdkit.command.annotation.permission.BotPermissionInterceptor
import dev.tokishu.jdkit.command.annotation.permission.UserPermissionInterceptor
import dev.tokishu.jdkit.command.annotation.role.UserRoleInterceptor
import dev.tokishu.jdkit.command.exception.ExceptionHandler
import dev.tokishu.jdkit.command.interceptor.CommandInterceptor
import dev.tokishu.jdkit.command.internal.ArgumentMapper
import dev.tokishu.jdkit.command.internal.CommandWrapper
import dev.tokishu.jdkit.command.internal.ContextMenuWrapper
import dev.tokishu.jdkit.config.JDKitProperties
import dev.tokishu.jdkit.di.DependencyContainer
import dev.tokishu.jdkit.extension.BotExtension
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.hooks.SubscribeEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Core manager responsible for command registration and dispatch.
 *
 * ## Command Registration (startup)
 * During [onEnable], all classes annotated with [@JDKitCommand][JDKitCommand] and
 * [@JDKitContextMenu][JDKitContextMenu] in the configured base package are discovered
 * via reflection. Their [CommandData] descriptors are collected into a single list and
 * submitted to Discord in **one bulk request** via [JDA.updateCommands]:
 * ```
 * jda.updateCommands().addCommands(commandDataList).queue()
 * ```
 * This avoids the per-command [JDA.upsertCommand] loop that would trigger Discord API
 * rate-limits (HTTP 429) when a bot has many commands.
 *
 * > **Note:** `updateCommands()` **replaces** the full set of registered commands.
 * > Any command not present in the list will be removed from Discord.
 *
 * ## Command Execution (runtime)
 * Incoming [SlashCommandInteractionEvent], [UserContextInteractionEvent], and
 * [MessageContextInteractionEvent] events are handled on the JDA gateway thread.
 * Argument mapping and interceptor checks run synchronously on that thread. The actual
 * `execute` method of the command is then dispatched off the gateway thread using a
 * Kotlin Coroutine:
 * ```
 * scope.launch { wrapper.method.invoke(wrapper.instance, *args) }
 * ```
 * The [scope] uses [Dispatchers.Default] (a shared, CPU-bounded thread pool) together
 * with a [SupervisorJob], so:
 * - The pool size is bounded (avoids the unbounded thread creation that
 *   `Executors.newCachedThreadPool()` / `CompletableFuture.runAsync()` caused).
 * - A failure in one command coroutine does not cancel other running coroutines.
 */
class CommandManager : ListenerAdapter(), BotExtension {

    private val logger = LoggerFactory.getLogger(CommandManager::class.java)

    private lateinit var jda: JDA
    private lateinit var config: JDKitProperties

    /**
     * Coroutine scope used to execute command handlers off the JDA gateway thread.
     *
     * [Dispatchers.Default] uses a shared thread pool bounded to
     * `max(2, availableProcessors)` threads, preventing thread exhaustion under load.
     * [SupervisorJob] ensures an exception in one child coroutine does not propagate
     * to siblings or cancel the scope.
     */
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val storedCommands = mutableMapOf<String, CommandWrapper>()
    private val subCommands = mutableMapOf<String, MutableMap<String, CommandWrapper>>()
    private val contextCommands = mutableMapOf<String, ContextMenuWrapper>()
    
    // Cooldown map: key = "userId_commandName", value = Timestamp of expiry
    private val cooldowns = ConcurrentHashMap<String, Long>()

    override fun onEnable(jda: JDA, config: JDKitProperties, basePackage: String) {
        this.jda = jda
        this.config = config
        
        // Register default interceptors
        addInterceptor(CooldownInterceptor(config))
        addInterceptor(BotPermissionInterceptor(config))
        addInterceptor(UserPermissionInterceptor(config))
        addInterceptor(UserRoleInterceptor(config))
        addInterceptor(OwnerInterceptor(config))

        jda.addEventListener(this)
        registerCommands(basePackage)
    }

    /**
     * Scans the given package for classes annotated with [@JDKitCommand][JDKitCommand],
     * builds their [CommandData] descriptors (including subcommands and options),
     * then delegates to [registerContextMenus] before sending everything in a single
     * bulk request via [bulkRegisterCommands].
     */
    private fun registerCommands(packageName: String) {
        val reflections = Reflections(packageName, Scanners.TypesAnnotated, Scanners.MethodsAnnotated)
        val classes = reflections.getTypesAnnotatedWith(JDKitCommand::class.java)
        val subCommandClasses = reflections.getTypesAnnotatedWith(JDKitSubcommand::class.java)

        val commandDataList = mutableListOf<CommandData>()

        for (clazz in classes) {
            val jdaCommand = clazz.getAnnotation(JDKitCommand::class.java)
            val baseName = jdaCommand.name
            
            val cmdData = Commands.slash(baseName, jdaCommand.description)
            if (jdaCommand.isGuildOnly) {
                cmdData.setContexts(InteractionContextType.GUILD)
            }

            val instance = clazz.getDeclaredConstructor().newInstance()
            DependencyContainer.injectJda(instance, jda)
            DependencyContainer.injectInto(instance)
            
            val subCmdMap = mutableMapOf<String, CommandWrapper>()
            
            // 1. Find Subcommands from methods inside the parent class
            val subCommandMethods = clazz.declaredMethods.filter { it.isAnnotationPresent(JDKitSubcommand::class.java) }
            for (method in subCommandMethods) {
                val subAnno = method.getAnnotation(JDKitSubcommand::class.java)
                val subData = SubcommandData(subAnno.name, subAnno.description)

                applyOptions(method, subData, "$baseName.${subAnno.name}")
                cmdData.addSubcommands(subData)
                subCmdMap[subAnno.name] = CommandWrapper(instance, method)
            }
            
            // 2. Find Standalone Subcommand classes
            registerStandaloneSubcommands(baseName, cmdData, subCmdMap, subCommandClasses)
            
            if (subCmdMap.isNotEmpty()) {
                subCommands[baseName] = subCmdMap
                logger.info("Registered command group /{} with {} subcommands", baseName, subCmdMap.size)
            } else {
                // Classic Command
                val executeMethod = clazz.declaredMethods.find { it.name == "execute" }
                if (executeMethod != null) {
                    applyOptions(executeMethod, cmdData, baseName)
                    storedCommands[baseName] = CommandWrapper(instance, executeMethod)
                    logger.info("Registered global command /{}", baseName)
                }
            }

            // Options from class level
            clazz.getAnnotationsByType(JDKitOption::class.java).forEach {
                val opt = OptionData(it.type, it.name, it.description, it.required, it.autoComplete)
                cmdData.addOptions(opt)
            }

            commandDataList.add(cmdData)
        }

        registerContextMenus(reflections, commandDataList)
        bulkRegisterCommands(commandDataList)
    }

    private fun registerStandaloneSubcommands(
        baseName: String,
        cmdData: SlashCommandData,
        subCmdMap: MutableMap<String, CommandWrapper>,
        subCommandClasses: Set<Class<*>>
    ) {
        val standaloneSubClasses = subCommandClasses.filter { it.getAnnotation(JDKitSubcommand::class.java).parent == baseName }
        for (subClass in standaloneSubClasses) {
            val subAnno = subClass.getAnnotation(JDKitSubcommand::class.java)
            val executeMethod = subClass.declaredMethods.find { it.name == "execute" }
            if (executeMethod != null) {
                val subInstance = subClass.getDeclaredConstructor().newInstance()
                DependencyContainer.injectJda(subInstance, jda)
                DependencyContainer.injectInto(subInstance)
                
                val subData = SubcommandData(subAnno.name, subAnno.description)

                // Grab options from both class AND method for standalone subcommands
                subClass.getAnnotationsByType(JDKitOption::class.java).forEach {
                    val opt = OptionData(it.type, it.name, it.description, it.required, it.autoComplete)
                    subData.addOptions(opt)
                }
                applyOptions(executeMethod, subData, "$baseName.${subAnno.name}")
                cmdData.addSubcommands(subData)
                subCmdMap[subAnno.name] = CommandWrapper(subInstance, executeMethod)
            }
        }
    }

    private fun registerContextMenus(reflections: Reflections, commandDataList: MutableList<CommandData>) {
        val contextMenuClasses = reflections.getTypesAnnotatedWith(JDKitContextMenu::class.java)
        for (clazz in contextMenuClasses) {
            val anno = clazz.getAnnotation(JDKitContextMenu::class.java)
            val name = anno.name
            
            val cmdData = Commands.context(anno.type, name)
            if (anno.isGuildOnly) {
                cmdData.setContexts(InteractionContextType.GUILD)
            }

            val executeMethod = clazz.declaredMethods.find { it.name == "execute" }
            if (executeMethod != null) {
                val instance = clazz.getDeclaredConstructor().newInstance()
                DependencyContainer.injectJda(instance, jda)
                DependencyContainer.injectInto(instance)
                
                contextCommands[name] = ContextMenuWrapper(instance, executeMethod, anno.type)
                logger.info("Registered Context Menu /{} ({})", name, anno.type)
                
                commandDataList.add(cmdData)
            }
        }
    }

    /**
     * Registers all collected [CommandData] (slash commands + context menus) with Discord
     * in a single API request, avoiding per-command rate-limits.
     *
     * When [JDKitProperties.guild.onlyMainGuild] is `true`, commands are registered only
     * for the configured main guild (propagation is instant). Otherwise they are
     * registered globally (propagation can take up to one hour).
     *
     * > **Important:** [JDA.updateCommands] replaces the **entire** command set. Any
     * > command not included in [commandDataList] will be removed from Discord.
     */
    private fun bulkRegisterCommands(commandDataList: List<CommandData>) {
        logger.info("Bulk registering {} command(s) in a single request", commandDataList.size)
        if (config.guild.onlyMainGuild && config.guild.main.isNotBlank()) {
            val guild = jda.getGuildById(config.guild.main)
            guild?.updateCommands()?.addCommands(commandDataList)?.queue()
        } else {
            jda.updateCommands().addCommands(commandDataList).queue()
        }
    }

    private fun applyOptions(method: Method, cmdData: Any, localePrefix: String) {
        val addOptFunc = { type: OptionType, name: String, desc: String, req: Boolean, autoComp: Boolean ->
            val opt = OptionData(type, name, desc, req, autoComp)

            if (cmdData is SlashCommandData) cmdData.addOptions(opt)
            if (cmdData is SubcommandData) cmdData.addOptions(opt)
        }

        method.getAnnotationsByType(JDKitOption::class.java).forEach {
            addOptFunc(it.type, it.name, it.description, it.required, it.autoComplete)
        }

        method.parameters.forEach { param ->
            if (param.isAnnotationPresent(JDKitOption::class.java)) {
                val opt = param.getAnnotation(JDKitOption::class.java)
                addOptFunc(opt.type, opt.name, opt.description, opt.required, opt.autoComplete)
            }
        }
    }

    private val interceptors = mutableListOf<CommandInterceptor>()

    fun addInterceptor(interceptor: CommandInterceptor) {
        interceptors.add(interceptor)
    }

    @SubscribeEvent
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val baseName = event.name
        val subName = event.subcommandName
        
        logger.debug("Received slash command /{} {}", baseName, subName ?: "")
        
        val wrapper = if (subName != null) {
            subCommands[baseName]?.get(subName)
        } else {
            storedCommands[baseName]
        }
        
        if (wrapper == null) {
            logger.warn("Could not find wrapper for command /{} {}", baseName, subName ?: "")
            return
        }

        try {
            // Run all interceptors
            logger.debug("Running interceptors for command /{} {}", baseName, subName ?: "")
            val passed = interceptors.all { 
                val result = it.preHandle(event, wrapper)
                if (!result) logger.debug("Interceptor {} failed for /{} {}", it.javaClass.simpleName, baseName, subName ?: "")
                result
            }
            if (!passed) return

            logger.debug("Interceptors passed. Mapping arguments for /{} {}", baseName, subName ?: "")
            // Dynamically map arguments
            val args = ArgumentMapper.mapArguments(wrapper.method, event)

            scope.launch {
                try {
                    wrapper.method.invoke(wrapper.instance, *args)
                } catch (e: Exception) {
                    logger.error("Error executing slash command handler for /{} {}", baseName, subName ?: "", e)
                    e.printStackTrace()
                    DependencyContainer.get(ExceptionHandler::class.java)?.handle(e, event) ?: run {
                        if (!event.isAcknowledged) {
                            event.reply("Произошла ошибка при выполнении команды.").setEphemeral(true).queue()
                        }
                    }
                }
            }

        } catch (e: Exception) {
            println("Core: Error mapping slash command arguments for /${baseName} ${subName ?: ""}")
            e.printStackTrace()
            DependencyContainer.get(ExceptionHandler::class.java)?.handle(e, event)
        }
    }

    private fun mapContextArguments(wrapper: ContextMenuWrapper, event: net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent<*>): Array<Any?> {
        return wrapper.method.parameters.map { param ->
            when {
                param.type.isAssignableFrom(event.javaClass) -> event
                param.type == User::class.java && event is UserContextInteractionEvent -> event.target
                param.type == Member::class.java && event is UserContextInteractionEvent -> event.targetMember
                param.type == Message::class.java && event is MessageContextInteractionEvent -> event.target
                else -> null
            }
        }.toTypedArray()
    }

    @SubscribeEvent
    override fun onUserContextInteraction(event: UserContextInteractionEvent) {
        val name = event.name
        val wrapper = contextCommands[name] ?: return
        
        if (wrapper.type != Command.Type.USER) return

        try {
            val args = mapContextArguments(wrapper, event)
            scope.launch {
                try {
                    wrapper.method.invoke(wrapper.instance, *args)
                } catch (e: Exception) {
                    logger.error("Error executing User Context Menu {}", name, e)
                    e.printStackTrace()
                    DependencyContainer.get(ExceptionHandler::class.java)?.handle(e, event) ?: run {
                        if (!event.isAcknowledged) {
                            event.reply("An error occurred.").setEphemeral(true).queue()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error mapping arguments for User Context Menu {}", name, e)
            e.printStackTrace()
            DependencyContainer.get(ExceptionHandler::class.java)?.handle(e, event)
        }
    }

    @SubscribeEvent
    override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
        val name = event.name
        val wrapper = contextCommands[name] ?: return
        
        if (wrapper.type != Command.Type.MESSAGE) return

        try {
            val args = mapContextArguments(wrapper, event)
            scope.launch {
                try {
                    wrapper.method.invoke(wrapper.instance, *args)
                } catch (e: Exception) {
                    logger.error("Error executing Message Context Menu {}", name, e)
                    e.printStackTrace()
                    DependencyContainer.get(ExceptionHandler::class.java)?.handle(e, event) ?: run {
                        if (!event.isAcknowledged) {
                            event.reply("An error occurred.").setEphemeral(true).queue()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error mapping arguments for Message Context Menu {}", name, e)
            e.printStackTrace()
            DependencyContainer.get(ExceptionHandler::class.java)?.handle(e, event)
        }
    }
}