package dev.tokishu.jdkit.command.annotation.owner

/**
 * Indicates that the command can only be executed by the bot owner.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresOwner
