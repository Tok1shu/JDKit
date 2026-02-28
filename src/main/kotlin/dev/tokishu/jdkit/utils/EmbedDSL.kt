package dev.tokishu.jdkit.utils

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color
import java.time.temporal.TemporalAccessor

/**
 * Creates a Discord MessageEmbed using a Kotlin DSL.
 * 
 * Example:
 * ```kotlin
 * val msg = embed {
 *     title = "User Profile"
 *     description = "Details about the user"
 *     color = Color.CYAN
 *     field("Level", "42", inline = true)
 *     footer("Requested by Admin")
 * }
 * ```
 */
inline fun embed(builder: InlineEmbedBuilder.() -> Unit): MessageEmbed {
    val b = InlineEmbedBuilder()
    b.builder()
    return b.build()
}

/**
 * A wrapper around JDA's EmbedBuilder that exposes properties for a cleaner DSL.
 */
class InlineEmbedBuilder {
    private val builder = EmbedBuilder()

    var title: String? = null
        set(value) {
            field = value
            builder.setTitle(value)
        }

    var titleUrl: String? = null
        set(value) {
            field = value
            builder.setTitle(title, value)
        }

    var description: String? = null
        set(value) {
            field = value
            builder.setDescription(value)
        }

    var color: Color? = null
        set(value) {
            field = value
            builder.setColor(value)
        }

    var image: String? = null
        set(value) {
            field = value
            builder.setImage(value)
        }

    var thumbnail: String? = null
        set(value) {
            field = value
            builder.setThumbnail(value)
        }

    var timestamp: TemporalAccessor? = null
        set(value) {
            field = value
            builder.setTimestamp(value)
        }

    fun field(name: String, value: String, inline: Boolean = false) {
        builder.addField(name, value, inline)
    }

    fun blankField(inline: Boolean = false) {
        builder.addBlankField(inline)
    }

    fun author(name: String, url: String? = null, iconUrl: String? = null) {
        builder.setAuthor(name, url, iconUrl)
    }

    fun footer(text: String, iconUrl: String? = null) {
        builder.setFooter(text, iconUrl)
    }

    fun build(): MessageEmbed = builder.build()
}
