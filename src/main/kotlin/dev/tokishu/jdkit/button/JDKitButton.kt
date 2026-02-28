package dev.tokishu.jdkit.button

/**
 * Marks a method as a button click handler.
 * 
 * @property id The exact Button ID to match. 
 * @property regex If true, [id] will be treated as a Regex pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class JDKitButton(
    val id: String,
    val regex: Boolean = false
)
