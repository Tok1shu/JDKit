package dev.tokishu.jdkit.command.interceptor

import dev.tokishu.jdkit.command.internal.CommandWrapper
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

interface CommandInterceptor {
    /**
     * Called before a command is executed.
     * @return true if execution should proceed, false if it should be halted.
     */
    fun preHandle(event: SlashCommandInteractionEvent, wrapper: CommandWrapper): Boolean
}
