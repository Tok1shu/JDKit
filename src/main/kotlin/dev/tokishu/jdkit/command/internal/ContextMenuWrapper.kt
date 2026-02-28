package dev.tokishu.jdkit.command.internal

import java.lang.reflect.Method

/**
 * Wrapper for a context menu command instance and method.
 */
data class ContextMenuWrapper(
    val instance: Any,
    val method: Method,
    val type: net.dv8tion.jda.api.interactions.commands.Command.Type
)
