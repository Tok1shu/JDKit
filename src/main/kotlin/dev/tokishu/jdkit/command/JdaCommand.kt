package dev.tokishu.jdkit.command

/**
 * Marks a class as a Discord slash command.
 * 
 * @property name The name of the command.
 * @property description The description of the command.
 * @property isGuildOnly Whether the command can only be used in guilds.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class JDKitCommand(
    val name: String,
    val description: String,
    val isGuildOnly: Boolean = false
)