package dev.tokishu.jdkit.button

import dev.tokishu.jdkit.BotComponent
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import dev.tokishu.jdkit.button.internal.ButtonWrapper
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.util.regex.Pattern
import dev.tokishu.jdkit.di.DependencyContainer
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import dev.tokishu.jdkit.component.AbstractComponentManager

class ButtonManager : AbstractComponentManager<dev.tokishu.jdkit.button.internal.ButtonWrapper>() {

    override val log = LoggerFactory.getLogger(ButtonManager::class.java)
    private lateinit var jda: JDA

    override fun onEnable(jda: JDA, config: dev.tokishu.jdkit.config.JDKitProperties, basePackage: String) {
        this.jda = jda
        jda.addEventListener(this)
        registerHandlers(basePackage)
    }

    private fun registerHandlers(packageName: String) {
        val reflections = Reflections(packageName, Scanners.MethodsAnnotated)
        val methods = reflections.getMethodsAnnotatedWith(JDKitButton::class.java)

        // Map to keep track of created instances per class
        val instances = mutableMapOf<Class<*>, Any>()

        for (method in methods) {
            val handler = method.getAnnotation(JDKitButton::class.java)
            val clazz = method.declaringClass
            
            val instance = instances.getOrPut(clazz) {
                val obj = clazz.getDeclaredConstructor().newInstance()
                // Inject JDA if it's a BotComponent (e.g. BaseEvent or BaseCommand)
                DependencyContainer.injectJda(obj, jda)
                DependencyContainer.injectInto(obj)
                obj
            }

            val wrapper = ButtonWrapper(instance, method)
            
            if (handler.regex) {
                regexHandlers.add(Pair(Pattern.compile(handler.id), wrapper))
            } else {
                exactHandlers[handler.id] = wrapper
            }
            
            log.info("Registered JDKitButton '{}' in method {}", handler.id, method.name)
        }
    }

    @net.dv8tion.jda.api.hooks.SubscribeEvent
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        executeInteraction(event.componentId, event)
    }
}
