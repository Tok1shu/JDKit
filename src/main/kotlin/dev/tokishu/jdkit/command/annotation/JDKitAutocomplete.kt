package dev.tokishu.jdkit.command.annotation

/**
 * Marks a method as an Autocomplete handler for a user-input option in a slash command.
 * 
 * @property optionName The name of the option triggering the autocomplete.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class JDKitAutocomplete(
    val optionName: String
)
