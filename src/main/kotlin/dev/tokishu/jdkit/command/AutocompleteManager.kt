package dev.tokishu.jdkit.command

import dev.tokishu.jdkit.BotComponent
import dev.tokishu.jdkit.di.DependencyContainer
import dev.tokishu.jdkit.command.annotation.JDKitAutocomplete
import dev.tokishu.jdkit.command.internal.AutocompleteWrapper
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.hooks.SubscribeEvent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class AutocompleteManager : ListenerAdapter(), dev.tokishu.jdkit.extension.BotExtension {

    private val logger = LoggerFactory.getLogger(AutocompleteManager::class.java)

    private lateinit var jda: JDA
    private val executor = Executors.newCachedThreadPool()

    // Key: target optionName
    private val handlers = mutableMapOf<String, AutocompleteWrapper>()

    override fun onEnable(jda: JDA, config: dev.tokishu.jdkit.config.JDKitProperties, basePackage: String) {
        this.jda = jda
        jda.addEventListener(this)
        registerHandlers(basePackage)
    }

    private fun registerHandlers(packageName: String) {
        val reflections = Reflections(packageName, Scanners.MethodsAnnotated)
        val methods = reflections.getMethodsAnnotatedWith(JDKitAutocomplete::class.java)

        val instances = mutableMapOf<Class<*>, Any>()

        for (method in methods) {
            val anno = method.getAnnotation(JDKitAutocomplete::class.java)
            val clazz = method.declaringClass

            val instance = instances.getOrPut(clazz) {
                val obj = clazz.getDeclaredConstructor().newInstance()
                DependencyContainer.injectJda(obj, jda)
                obj
            }

            handlers[anno.optionName] = AutocompleteWrapper(instance, method)
            logger.info("Registered JDKitAutocomplete for option '{}' in method {}", anno.optionName, method.name)
        }
    }

    @SubscribeEvent
    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        val optionName = event.focusedOption.name
        val wrapper = handlers[optionName] ?: return

        CompletableFuture.runAsync({
            try {
                val args = wrapper.method.parameters.map { param ->
                    when {
                        param.type == CommandAutoCompleteInteractionEvent::class.java -> event
                        else -> null
                    }
                }.toTypedArray()
    
                wrapper.method.invoke(wrapper.instance, *args)
            } catch (e: Exception) {
                logger.error("Error executing JDKitAutocomplete for {}", optionName, e)
                e.printStackTrace()
            }
        }, executor)
    }
}
