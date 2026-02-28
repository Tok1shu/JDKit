package dev.tokishu.jdkit.select

import dev.tokishu.jdkit.BotComponent
import dev.tokishu.jdkit.select.internal.SelectMenuWrapper
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.util.regex.Pattern
import dev.tokishu.jdkit.di.DependencyContainer
import org.slf4j.LoggerFactory
import dev.tokishu.jdkit.component.AbstractComponentManager

class SelectMenuManager : AbstractComponentManager<dev.tokishu.jdkit.select.internal.SelectMenuWrapper>() {

    override val log = LoggerFactory.getLogger(SelectMenuManager::class.java)
    private lateinit var jda: JDA

    override fun onEnable(jda: JDA, config: dev.tokishu.jdkit.config.JDKitProperties, basePackage: String) {
        this.jda = jda
        jda.addEventListener(this)
        registerHandlers(basePackage)
    }

    private fun registerHandlers(packageName: String) {
        val reflections = Reflections(packageName, Scanners.MethodsAnnotated)
        val methods = reflections.getMethodsAnnotatedWith(JDKitSelectMenu::class.java)

        val instances = mutableMapOf<Class<*>, Any>()

        for (method in methods) {
            val handler = method.getAnnotation(JDKitSelectMenu::class.java)
            val clazz = method.declaringClass
            
            val instance = instances.getOrPut(clazz) {
                val obj = clazz.getDeclaredConstructor().newInstance()
                DependencyContainer.injectJda(obj, jda)
                DependencyContainer.injectInto(obj)
                obj
            }

            val wrapper = SelectMenuWrapper(instance, method)
            
            if (handler.regex) {
                regexHandlers.add(Pair(Pattern.compile(handler.id), wrapper))
            } else {
                exactHandlers[handler.id] = wrapper
            }
            
            log.info("Registered JDKitSelectMenu '{}' in method {}", handler.id, method.name)
        }
    }

    @net.dv8tion.jda.api.hooks.SubscribeEvent
    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        handleSelectMenu(event.componentId, event)
    }

    @net.dv8tion.jda.api.hooks.SubscribeEvent
    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        handleSelectMenu(event.componentId, event)
    }

    private fun handleSelectMenu(menuId: String, event: net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent<*, *>) {
        executeInteraction(menuId, event)
    }
}
