package dev.tokishu.jdkit.command.annotation.cooldown

/**
 * Applies a cooldown to a command or subcommand.
 * 
 * @property seconds The duration of the cooldown in seconds.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cooldown(
    val seconds: Int
)
