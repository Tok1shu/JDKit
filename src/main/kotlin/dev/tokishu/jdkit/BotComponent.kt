package dev.tokishu.jdkit

import net.dv8tion.jda.api.JDA

/**
 * Base interface for components that require access to the JDA instance.
 */
interface BotComponent {
    val jda: JDA
}
