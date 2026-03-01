package dev.tokishu.jdkit.command.internal

import dev.tokishu.jdkit.command.JDKitOption
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.lang.reflect.Method

/**
 * Utility purely responsible for mapping Discord SlashCommandInteractionEvent Options
 * to the Kotlin method parameters defined by the bot developer.
 */
object ArgumentMapper {

    fun mapArguments(method: Method, event: SlashCommandInteractionEvent): Array<Any?> {
        return method.parameters.map { param ->
            when {
                param.type == SlashCommandInteractionEvent::class.java -> event

                // Auto-mapping Discord Options specified by parameter annotation
                param.isAnnotationPresent(JDKitOption::class.java) -> {
                    val optAnno = param.getAnnotation(JDKitOption::class.java)
                    val option = event.getOption(optAnno.name)

                    when (param.type) {
                        String::class.java -> option?.asString
                        Int::class.java, Integer::class.java -> option?.asInt
                        Long::class.java, java.lang.Long::class.java -> option?.asLong
                        Boolean::class.java, java.lang.Boolean::class.java -> option?.asBoolean
                        User::class.java -> option?.asUser
                        Role::class.java -> option?.asRole
                        GuildChannel::class.java -> option?.asChannel
                        else -> null
                    }
                }
                else -> null
            }
        }.toTypedArray()
    }
}
