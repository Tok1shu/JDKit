package dev.tokishu.jdkit.command.annotation

import net.dv8tion.jda.api.interactions.commands.Command

/**
 * Marks a class or method as a Context Menu command (User or Message).
 * 
 * @property name The name of the context menu. Must be between 1 and 32 characters.
 * @property type The type of context menu (USER or MESSAGE).
 * @property isGuildOnly Whether this context menu should only be registered in the main guild.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class JDKitContextMenu(
    val name: String,
    val type: Command.Type,
    val isGuildOnly: Boolean = false
)
