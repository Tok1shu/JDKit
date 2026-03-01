package dev.tokishu.jdkit.utils

import dev.tokishu.jdkit.config.JDKitProperties
import dev.tokishu.jdkit.extension.BotExtension
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

object EventWaiter : ListenerAdapter() {

    private val messageWaiters = ConcurrentHashMap<String, CompletableFuture<MessageReceivedEvent>>()
    private val buttonWaiters = ConcurrentHashMap<String, CompletableFuture<ButtonInteractionEvent>>()
    private val stringSelectWaiters = ConcurrentHashMap<String, CompletableFuture<StringSelectInteractionEvent>>()
    private val entitySelectWaiters = ConcurrentHashMap<String, CompletableFuture<EntitySelectInteractionEvent>>()
    private val modalWaiters = ConcurrentHashMap<String, CompletableFuture<ModalInteractionEvent>>()

    private fun key(userId: String, channelId: String?) = if (channelId != null) "${userId}_${channelId}" else userId

    private fun <T> scheduleTimeout(future: CompletableFuture<T>, map: ConcurrentHashMap<String, CompletableFuture<T>>, key: String, timeoutMillis: Long) {
        val executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
        executor.schedule({
            if (!future.isDone) {
                map.remove(key)
                future.completeExceptionally(java.util.concurrent.TimeoutException("Wait timed out after $timeoutMillis ms"))
            }
        }, timeoutMillis, TimeUnit.MILLISECONDS)
    }

    fun waitForMessage(user: User, channel: MessageChannel? = null, timeoutMillis: Long = 60000): CompletableFuture<MessageReceivedEvent> {
        val future = CompletableFuture<MessageReceivedEvent>()
        val k = key(user.id, channel?.id)
        messageWaiters[k] = future
        scheduleTimeout(future, messageWaiters, k, timeoutMillis)
        return future
    }

    fun waitForButton(user: User, channel: MessageChannel? = null, timeoutMillis: Long = 60000): CompletableFuture<ButtonInteractionEvent> {
        val future = CompletableFuture<ButtonInteractionEvent>()
        val k = key(user.id, channel?.id)
        buttonWaiters[k] = future
        scheduleTimeout(future, buttonWaiters, k, timeoutMillis)
        return future
    }

    fun waitForStringSelect(user: User, channel: MessageChannel? = null, timeoutMillis: Long = 60000): CompletableFuture<StringSelectInteractionEvent> {
        val future = CompletableFuture<StringSelectInteractionEvent>()
        val k = key(user.id, channel?.id)
        stringSelectWaiters[k] = future
        scheduleTimeout(future, stringSelectWaiters, k, timeoutMillis)
        return future
    }

    fun waitForEntitySelect(user: User, channel: MessageChannel? = null, timeoutMillis: Long = 60000): CompletableFuture<EntitySelectInteractionEvent> {
        val future = CompletableFuture<EntitySelectInteractionEvent>()
        val k = key(user.id, channel?.id)
        entitySelectWaiters[k] = future
        scheduleTimeout(future, entitySelectWaiters, k, timeoutMillis)
        return future
    }

    fun waitForModal(user: User, timeoutMillis: Long = 60000): CompletableFuture<ModalInteractionEvent> {
        val future = CompletableFuture<ModalInteractionEvent>()
        val k = user.id
        modalWaiters[k] = future
        scheduleTimeout(future, modalWaiters, k, timeoutMillis)
        return future
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        
        val strictKey = key(event.author.id, event.channel.id)
        messageWaiters.remove(strictKey)?.let {
            it.complete(event)
            return
        }
        
        val looseKey = key(event.author.id, null)
        messageWaiters.remove(looseKey)?.let {
            it.complete(event)
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val strictKey = key(event.user.id, event.channel?.id)
        buttonWaiters.remove(strictKey)?.let {
            it.complete(event)
            return
        }
        
        val looseKey = key(event.user.id, null)
        buttonWaiters.remove(looseKey)?.let {
            it.complete(event)
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        val strictKey = key(event.user.id, event.channel?.id)
        stringSelectWaiters.remove(strictKey)?.let {
            it.complete(event)
            return
        }
        
        val looseKey = key(event.user.id, null)
        stringSelectWaiters.remove(looseKey)?.let {
            it.complete(event)
        }
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        val strictKey = key(event.user.id, event.channel?.id)
        entitySelectWaiters.remove(strictKey)?.let {
            it.complete(event)
            return
        }
        
        val looseKey = key(event.user.id, null)
        entitySelectWaiters.remove(looseKey)?.let {
            it.complete(event)
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        modalWaiters.remove(event.user.id)?.let {
            it.complete(event)
        }
    }

    class GenericWaiter(
        val future: CompletableFuture<net.dv8tion.jda.api.events.GenericEvent>,
        val expectedType: Class<out net.dv8tion.jda.api.events.GenericEvent>,
        val filter: (net.dv8tion.jda.api.events.GenericEvent) -> Boolean
    )

    @PublishedApi
    internal val genericWaiters = java.util.concurrent.ConcurrentLinkedQueue<GenericWaiter>()

    inline fun <reified T : net.dv8tion.jda.api.events.GenericEvent> waitForEvent(
        timeoutMillis: Long = 60000,
        crossinline filter: (T) -> Boolean = { true }
    ): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        val wrapperFuture = CompletableFuture<net.dv8tion.jda.api.events.GenericEvent>()
        
        wrapperFuture.thenAccept { event ->
            future.complete(event as T)
        }.exceptionally { ex ->
            future.completeExceptionally(ex)
            null
        }

        val waiter = GenericWaiter(wrapperFuture, T::class.java) { filter(it as T) }
        genericWaiters.add(waiter)
        
        val executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
        executor.schedule({
            if (!future.isDone) {
                genericWaiters.remove(waiter)
                future.completeExceptionally(java.util.concurrent.TimeoutException("Wait timed out after $timeoutMillis ms"))
            }
        }, timeoutMillis, TimeUnit.MILLISECONDS)
        
        return future
    }

    override fun onGenericEvent(event: net.dv8tion.jda.api.events.GenericEvent) {
        val iterator = genericWaiters.iterator()
        while (iterator.hasNext()) {
            val waiter = iterator.next()
            if (waiter.expectedType.isInstance(event)) {
                try {
                    if (waiter.filter(event)) {
                        iterator.remove()
                        waiter.future.complete(event)
                    }
                } catch (e: Exception) {
                    // Ignore filter errors
                }
            }
        }
        super.onGenericEvent(event)
    }
}

class EventWaiterExtension : BotExtension {
    private val logger = LoggerFactory.getLogger(EventWaiterExtension::class.java)

    override fun onEnable(jda: JDA, config: JDKitProperties, basePackage: String) {
        jda.addEventListener(EventWaiter)
        logger.info("Registered EventWaiter utility")
    }
}
