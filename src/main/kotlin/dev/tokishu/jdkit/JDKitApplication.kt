package dev.tokishu.jdkit

import dev.tokishu.jdkit.annotation.RunJDKit
import dev.tokishu.jdkit.config.ConfigLoader
import dev.tokishu.jdkit.di.DependencyContainer
import dev.tokishu.jdkit.extension.BotExtension
import dev.tokishu.jdkit.locale.LocalizationManager
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

/**
 * Main launcher for the JDKit Framework.
 * Dynamically switches between Native mode and Spring Boot mode.
 */
object JDKitApplication {
    private val logger = LoggerFactory.getLogger(JDKitApplication::class.java)

    /**
     * Starts the JDKit Application. Call this from your main() function.
     */
    @JvmStatic
    fun run(args: Array<String> = emptyArray()) {
        val callerClass = getCallerClass()
        val basePackage = getBasePackage(callerClass)

        if (isSpringAvailable()) {
            logger.info("JDKit: Spring Boot detected on classpath. Bootstrapping via Spring Boot Lifecycle...")
            SpringLauncher.launch(callerClass, args, basePackage)
        } else {
            logger.info("JDKit: Spring Boot not found. Bootstrapping via Native JDKit Lifecycle...")
            launchNative(basePackage)
        }
    }

    private fun isSpringAvailable(): Boolean {
        return try {
            Class.forName("org.springframework.boot.SpringApplication")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun getCallerClass(): Class<*> {
        val stackTrace = Thread.currentThread().stackTrace
        // Index 0: Thread.getStackTrace
        // Index 1: JDKitApplication.getCallerClass
        // Index 2: JDKitApplication.run
        // Index 3: The actual caller (main method or object)
        val callerName = stackTrace[3].className
        return Class.forName(callerName)
    }

    private fun getBasePackage(callerClass: Class<*>): String {
        // If the caller is annotated with @RunJDKit, read its package override
        val runAnnotation = callerClass.getAnnotation(RunJDKit::class.java)
        return if (runAnnotation != null && runAnnotation.basePackage.isNotBlank()) {
            runAnnotation.basePackage
        } else {
            callerClass.packageName.takeIf { it.isNotBlank() } ?: "dev"
        }
    }

    private fun launchNative(basePackage: String) {
        val config = ConfigLoader.loadConfig()
        
        val envToken = System.getenv("DISCORD_TOKEN")
        val token = if (config.token.isNotBlank()) config.token else if (!envToken.isNullOrBlank()) envToken else {
            logger.error("JDKit: Discord bot token is missing! Provide it via jdkit.token config or DISCORD_TOKEN.")
            exitProcess(1)
        }

        logger.info("JDKit: Starting JDA...")
        val builder = JDABuilder.createDefault(token)
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS)
        builder.setEventManager(net.dv8tion.jda.api.hooks.AnnotatedEventManager())

        val jda = builder.build()
        jda.awaitReady()
        logger.info("JDKit: JDA Ready. Logged in as ${jda.selfUser.name}")

        LocalizationManager.loadLocales()

        val reflections = Reflections(basePackage)
        val extensionClasses = reflections.getSubTypesOf(BotExtension::class.java)

        logger.info("JDKit: Found ${extensionClasses.size} BotExtensions via Native Scanner. Enabling...")
        for (clazz in extensionClasses) {
            if (clazz.isInterface || java.lang.reflect.Modifier.isAbstract(clazz.modifiers)) continue
            try {
                // Instantiate extension natively
                val instance = clazz.kotlin.objectInstance ?: clazz.getDeclaredConstructor().newInstance()
                
                // Register in our native DependencyContainer so @Inject works
                DependencyContainer.register(instance as Any)
                
                instance.onEnable(jda, config, basePackage)
                logger.info("JDKit: Enabled Extension - ${clazz.simpleName}")
            } catch (e: Exception) {
                logger.error("JDKit: Error enabling ${clazz.simpleName}", e)
            }
        }
        logger.info("JDKit: Native Initialization complete!")
    }
}
