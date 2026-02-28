package dev.tokishu.jdkit.command.annotation.role

/**
 * Indicates that the command requires the user to have a specific role.
 * 
 * @property roleId The required role ID.
 * @property roleName The required role name (checked if ID is empty).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresRole(
    val roleId: String = "",
    val roleName: String = ""
)
