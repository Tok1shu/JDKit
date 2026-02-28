package dev.tokishu.jdkit.config

import dev.tokishu.jdkit.button.ButtonManager
import dev.tokishu.jdkit.command.AutocompleteManager
import dev.tokishu.jdkit.command.CommandManager
import dev.tokishu.jdkit.event.EventManager
import dev.tokishu.jdkit.extension.BotExtension
import dev.tokishu.jdkit.locale.LocalizationManager
import dev.tokishu.jdkit.modal.ModalManager
import dev.tokishu.jdkit.schedule.TaskScheduler
import dev.tokishu.jdkit.select.SelectMenuManager
import dev.tokishu.jdkit.utils.EventWaiterExtension
import dev.tokishu.jdkit.utils.PaginationExtension
import dev.tokishu.jdkit.di.DependencyContainer
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(JDKitProperties::class)
open class JDKitAutoConfiguration {
    private val logger = LoggerFactory.getLogger(JDKitAutoConfiguration::class.java)

    @Bean
    @ConditionalOnMissingBean
    open fun buttonManager() = ButtonManager()

    @Bean
    @ConditionalOnMissingBean
    open fun selectMenuManager() = SelectMenuManager()

    @Bean
    @ConditionalOnMissingBean
    open fun commandManager() = CommandManager()

    @Bean
    @ConditionalOnMissingBean
    open fun autocompleteManager() = AutocompleteManager()

    @Bean
    @ConditionalOnMissingBean
    open fun taskScheduler() = TaskScheduler()

    @Bean
    @ConditionalOnMissingBean
    open fun modalManager() = ModalManager()

    @Bean
    @ConditionalOnMissingBean
    open fun eventManager() = EventManager()

    @Bean
    @ConditionalOnMissingBean
    open fun paginationExtension() = PaginationExtension()

    @Bean
    @ConditionalOnMissingBean
    open fun eventWaiterExtension() = EventWaiterExtension()

    @Bean
    @ConditionalOnMissingBean
    open fun jda(
        properties: JDKitProperties,
        extensions: List<BotExtension>,
        applicationContext: ApplicationContext
    ): JDA {
        val token = properties.token.takeIf { it.isNotBlank() }
            ?: System.getenv("DISCORD_TOKEN")
            ?: run {
                logger.error("JDKit: Discord bot token is missing! Provide it via jdkit.token config or DISCORD_TOKEN.")
                throw IllegalArgumentException("Discord bot token is missing!")
            }

        logger.info("JDKit: Starting JDA...")
        val builder = JDABuilder.createDefault(token)
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS)
        builder.setEventManager(net.dv8tion.jda.api.hooks.AnnotatedEventManager())

        val jda = builder.build()
        jda.awaitReady()
        logger.info("JDKit: JDA Ready. Logged in as ${jda.selfUser.name}")

        LocalizationManager.loadLocales()

        val basePackage = applicationContext.environment.getProperty("jdkit.base-package", "dev") ?: "dev"
        
        // Feed the Spring ApplicationContext into our legacy DependencyContainer
        // so that @Inject fields still work smoothly within Spring!
        DependencyContainer.applicationContext = applicationContext

        logger.info("JDKit: Found ${extensions.size} BotExtensions in Spring Context. Enabling...")
        for (extension in extensions) {
            try {
                extension.onEnable(jda, properties, basePackage)
                logger.info("JDKit: Enabled Extension - ${extension.javaClass.simpleName}")
            } catch (e: Exception) {
                logger.error("JDKit: Error enabling ${extension.javaClass.simpleName}", e)
            }
        }
        logger.info("JDKit: Initialization complete!")
        return jda
    }
}
