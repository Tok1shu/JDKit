package dev.tokishu.jdkit.command.annotation.permission

import net.dv8tion.jda.api.Permission

/**
 * Indicates that the command requires the user to have specific permissions.
 * 
 * @property permissions The required JDA permissions.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresPermission(
    vararg val permissions: Permission
)
