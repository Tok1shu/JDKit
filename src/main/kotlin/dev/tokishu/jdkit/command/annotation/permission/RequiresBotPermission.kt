package dev.tokishu.jdkit.command.annotation.permission

import net.dv8tion.jda.api.Permission

/**
 * Indicates that the command requires the *BOT* to have specific permissions in the guild/channel.
 * 
 * @property permissions The required JDA permissions for the bot.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresBotPermission(
    vararg val permissions: Permission
)
