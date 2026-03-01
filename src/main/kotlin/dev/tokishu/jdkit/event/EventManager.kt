package dev.tokishu.jdkit.event

import dev.tokishu.jdkit.BotComponent
import dev.tokishu.jdkit.config.JDKitProperties
import dev.tokishu.jdkit.di.DependencyContainer
import dev.tokishu.jdkit.event.annotation.JDKitEvent
import dev.tokishu.jdkit.extension.BotExtension
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.hooks.AnnotatedEventManager
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory

class EventManager : BotExtension {

    private val logger = LoggerFactory.getLogger(EventManager::class.java)

    private lateinit var jda: JDA

    /**
     * Scans the packages for classes annotated with @JDKitEvent,
     * instantiates them, injects JDA if applicable, 
     * and registers them dynamically with the JDA AnnotatedEventManager.
     */
    override fun onEnable(jda: JDA, config: JDKitProperties, basePackage: String) {
        this.jda = jda
        registerEvents(basePackage)
    }

    private fun registerEvents(packageName: String) {
        val reflections = Reflections(packageName, Scanners.TypesAnnotated)
        val classes = reflections.getTypesAnnotatedWith(JDKitEvent::class.java)

        for (clazz in classes) {
            try {
                val instance = clazz.getDeclaredConstructor().newInstance()
                
                DependencyContainer.injectJda(instance, jda)

                jda.addEventListener(instance)
                logger.info("Registered event listener class {}", clazz.simpleName)
            } catch (e: Exception) {
                logger.error("Failed to register event listener {}", clazz.simpleName, e)
                e.printStackTrace()
            }
        }
    }
}
