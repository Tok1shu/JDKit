package dev.tokishu.jdkit.command

import net.dv8tion.jda.api.interactions.commands.OptionType

/**
 * Defines an option for a Discord slash command.
 * Can be applied to the parameters of an execute function or on the class/function.
 *
 * @property name The name of the option.
 * @property description The description of the option.
 * @property type The type of the option.
 * @property required Whether the option is required. Defaults to true.
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class JDKitOption(
    val name: String,
    val description: String,
    val type: OptionType,
    val required: Boolean = true,
    val autoComplete: Boolean = false
)