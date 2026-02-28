package dev.tokishu.jdkit.modal

import dev.tokishu.jdkit.BotComponent
import dev.tokishu.jdkit.modal.internal.ModalWrapper
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.util.regex.Pattern
import dev.tokishu.jdkit.di.DependencyContainer
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import dev.tokishu.jdkit.component.AbstractComponentManager

class ModalManager : AbstractComponentManager<dev.tokishu.jdkit.modal.internal.ModalWrapper>() {

    override val log = LoggerFactory.getLogger(ModalManager::class.java)
    private lateinit var jda: JDA

    override fun onEnable(jda: JDA, config: dev.tokishu.jdkit.config.JDKitProperties, basePackage: String) {
        this.jda = jda
        jda.addEventListener(this)
        registerHandlers(basePackage)
    }

    private fun registerHandlers(packageName: String) {
        val reflections = Reflections(packageName, Scanners.MethodsAnnotated)
        val methods = reflections.getMethodsAnnotatedWith(JDKitModal::class.java)

        val instances = mutableMapOf<Class<*>, Any>()

        for (method in methods) {
            val handler = method.getAnnotation(JDKitModal::class.java)
            val clazz = method.declaringClass
            
            val instance = instances.getOrPut(clazz) {
                val obj = clazz.getDeclaredConstructor().newInstance()
                DependencyContainer.injectJda(obj, jda)
                DependencyContainer.injectInto(obj)
                obj
            }

            val wrapper = ModalWrapper(instance, method)
            
            if (handler.regex) {
                regexHandlers.add(Pair(Pattern.compile(handler.id), wrapper))
            } else {
                exactHandlers[handler.id] = wrapper
            }
            
            log.info("Registered JDKitModal '{}' in method {}", handler.id, method.name)
        }
    }

    @net.dv8tion.jda.api.hooks.SubscribeEvent
    override fun onModalInteraction(event: ModalInteractionEvent) {
        executeInteraction(event.modalId, event)
    }
}
