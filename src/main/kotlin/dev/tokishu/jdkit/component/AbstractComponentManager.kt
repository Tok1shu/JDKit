package dev.tokishu.jdkit.component

import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * A base manager class that abstracts away the repetitive logic of matching exact component IDs
 * or Regex patterns, and executing them asynchronously with a thread pool.
 * Used by UI extensions like ButtonManager, ModalManager, and SelectMenuManager.
 */
abstract class AbstractComponentManager<W : dev.tokishu.jdkit.component.ComponentWrapper> : ListenerAdapter(), dev.tokishu.jdkit.extension.BotExtension {

    protected val executor = Executors.newCachedThreadPool()

    protected val exactHandlers = mutableMapOf<String, W>()
    protected val regexHandlers = mutableListOf<Pair<Pattern, W>>()
    
    protected abstract val log: Logger

    /**
     * Finds a matching wrapper for the given component ID and executes it.
     */
    protected fun executeInteraction(componentId: String, event: net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent) {
        var wrapper = exactHandlers[componentId]
        
        if (wrapper == null) {
            wrapper = regexHandlers.firstOrNull { it.first.matcher(componentId).matches() }?.second
        }

        if (wrapper != null) {
            CompletableFuture.runAsync({
                try {
                    // Dynamically map arguments
                    val args = wrapper.method.parameters.map { param ->
                        when {
                            param.type.isAssignableFrom(event::class.java) -> event
                            else -> null
                        }
                    }.toTypedArray()
                    
                    wrapper.method.invoke(wrapper.instance, *args)
                } catch (e: Exception) {
                    log.error("Error executing handler for {}", componentId, e)
                    e.printStackTrace()
                    if (event is net.dv8tion.jda.api.interactions.callbacks.IReplyCallback) {
                        dev.tokishu.jdkit.di.DependencyContainer.get(dev.tokishu.jdkit.command.exception.ExceptionHandler::class.java)?.handle(e, event) ?: run {
                            if (!event.isAcknowledged) {
                                event.reply("An error occurred while handling this component.").setEphemeral(true).queue()
                            }
                        }
                    }
                }
            }, executor)
        }
    }
}

/**
 * Base wrapper for UI components associating the instantiated class logic to its method.
 */
interface ComponentWrapper {
    val instance: Any
    val method: java.lang.reflect.Method
}
