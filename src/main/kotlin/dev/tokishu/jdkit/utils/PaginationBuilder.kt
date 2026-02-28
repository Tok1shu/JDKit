package dev.tokishu.jdkit.utils

import dev.tokishu.jdkit.config.JDKitProperties
import dev.tokishu.jdkit.extension.BotExtension
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

/**
 * A utility to easily build and send paginated messages using interactive buttons.
 */
class PaginationBuilder(
    private val pages: List<MessageEmbed>,
    private val userId: String,
    private val timeoutSeconds: Long = 300
) {
    init {
        require(pages.isNotEmpty()) { "PaginationBuilder requires at least one page." }
    }

    /**
     * Sends the paginated message as a reply to an interaction.
     */
    fun send(event: IReplyCallback) {
        if (pages.size == 1) {
            event.replyEmbeds(pages[0]).queue()
            return
        }
        
        val sessionId = java.util.UUID.randomUUID().toString()

        val btnPrev = Button.secondary("core_page_prev:$sessionId", "⬅️").asDisabled()
        val btnNext = Button.secondary("core_page_next:$sessionId", "➡️")

        event.replyEmbeds(pages[0]).addComponents(ActionRow.of(btnPrev, btnNext)).queue { hook: InteractionHook ->
            hook.retrieveOriginal().queue { msg: Message ->
                PaginationListener.register(msg.id, sessionId, userId, pages, timeoutSeconds)
            }
        }
    }
}

object PaginationListener : ListenerAdapter() {
    
    data class Session(
        val messageId: String,
        val userId: String,
        val pages: List<MessageEmbed>,
        var currentPage: Int = 0,
        val expiresAt: Long
    )

    private val sessions = ConcurrentHashMap<String, Session>()

    fun register(messageId: String, sessionId: String, userId: String, pages: List<MessageEmbed>, timeoutSeconds: Long) {
        sessions[sessionId] = Session(
            messageId = messageId,
            userId = userId,
            pages = pages,
            expiresAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds)
        )
        cleanup()
    }

    private fun cleanup() {
        val now = System.currentTimeMillis()
        sessions.entries.removeIf { it.value.expiresAt < now }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val id = event.componentId
        if (!id.startsWith("core_page_prev:") && !id.startsWith("core_page_next:")) return

        val sessionId = id.substringAfter(":")
        val session = sessions[sessionId]

        if (session == null) {
            event.reply("This pagination session has expired.").setEphemeral(true).queue()
            return
        }

        if (event.user.id != session.userId) {
            event.reply("You cannot interact with this pagination.").setEphemeral(true).queue()
            return
        }

        val isNext = id.startsWith("core_page_next:")
        if (isNext) {
            if (session.currentPage < session.pages.size - 1) session.currentPage++
        } else {
            if (session.currentPage > 0) session.currentPage--
        }

        val btnPrev = Button.secondary("core_page_prev:$sessionId", "⬅️")
            .withDisabled(session.currentPage == 0)
        
        val btnNext = Button.secondary("core_page_next:$sessionId", "➡️")
            .withDisabled(session.currentPage == session.pages.size - 1)
            
        event.editMessageEmbeds(session.pages[session.currentPage]).setComponents(ActionRow.of(btnPrev, btnNext)).queue()
    }
}

class PaginationExtension : BotExtension {
    private val logger = LoggerFactory.getLogger(PaginationExtension::class.java)
    
    override fun onEnable(jda: JDA, config: dev.tokishu.jdkit.config.JDKitProperties, basePackage: String) {
        jda.addEventListener(PaginationListener)
        logger.info("Registered PaginationBuilder utility")
    }
}
