package dev.tokishu.jdkit.command.annotation.owner

import dev.tokishu.jdkit.command.interceptor.CommandInterceptor
import dev.tokishu.jdkit.command.internal.CommandWrapper
import dev.tokishu.jdkit.config.JDKitProperties
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class OwnerInterceptor(private val config: JDKitProperties) : CommandInterceptor {

    override fun preHandle(event: SlashCommandInteractionEvent, wrapper: CommandWrapper): Boolean {
        val method = wrapper.method
        val clazz = wrapper.instance.javaClass
        
        val reqOwnerClass = clazz.getAnnotation(RequiresOwner::class.java)
        val reqOwnerMethod = method.getAnnotation(RequiresOwner::class.java)
        
        if (reqOwnerClass != null || reqOwnerMethod != null) {
            val guild = event.guild
            if (guild == null || event.user.id != guild.ownerId) {
                 event.reply("This command is restricted to the Server Owner and cannot be used in DMs.").setEphemeral(true).queue()
                 return false
            }
        }

        return true
    }
}
