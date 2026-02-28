package dev.tokishu.jdkit.command.annotation.role

import dev.tokishu.jdkit.command.interceptor.CommandInterceptor
import dev.tokishu.jdkit.command.internal.CommandWrapper
import dev.tokishu.jdkit.config.JDKitProperties
import dev.tokishu.jdkit.locale.LocalizationManager
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class UserRoleInterceptor(private val config: JDKitProperties) : CommandInterceptor {

    override fun preHandle(event: SlashCommandInteractionEvent, wrapper: CommandWrapper): Boolean {
        if (!event.isFromGuild) return true
        
        val method = wrapper.method
        val clazz = wrapper.instance.javaClass
        
        val reqRoleClass = clazz.getAnnotation(RequiresRole::class.java)
        val reqRoleMethod = method.getAnnotation(RequiresRole::class.java)
        val rolesToCheck = listOfNotNull(reqRoleClass, reqRoleMethod)
        
        val member = event.member ?: return true
        
        if (rolesToCheck.isNotEmpty()) {
            val hasRole = rolesToCheck.any { reqRole ->
                member.roles.any { 
                    (reqRole.roleId.isNotEmpty() && it.id == reqRole.roleId) || 
                    (reqRole.roleName.isNotEmpty() && it.name == reqRole.roleName) 
                }
            }
            if (!hasRole) {
                val userLang = event.userLocale.locale.split("-")[0]
                val locale = if (config.locale.preferUser) userLang else config.locale.default
                
                val msg = LocalizationManager.getMessage("error.no_role", "core", locale, config.locale.default)
                event.reply(msg).setEphemeral(true).queue()
                return false
            }
        }
        
        return true
    }
}
