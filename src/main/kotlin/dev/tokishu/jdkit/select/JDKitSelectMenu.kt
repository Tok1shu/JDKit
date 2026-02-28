package dev.tokishu.jdkit.select

/**
 * Annotation to handle Select Menu interactions.
 * Place this on a method with `StringSelectInteractionEvent`, `EntitySelectInteractionEvent`, or `RoleSelectInteractionEvent`.
 * 
 * @param id The exact ID or regex pattern of the select menu.
 * @param regex True if the id acts as a regular expression pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class JDKitSelectMenu(
    val id: String,
    val regex: Boolean = false
)
