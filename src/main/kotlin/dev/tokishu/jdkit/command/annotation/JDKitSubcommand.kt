package dev.tokishu.jdkit.command.annotation

/**
 * Marks a method or class as a Discord slash subcommand.
 * If used on a class, the 'parent' field must specify the parent @JDKitCommand name.
 * 
 * @property name The name of the subcommand.
 * @property description The description of the subcommand.
 * @property parent The name of the parent command (required if used on a class).
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class JDKitSubcommand(
    val name: String,
    val description: String,
    val parent: String = ""
)
