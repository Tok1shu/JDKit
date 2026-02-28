package dev.tokishu.jdkit.command.annotation.permission

import dev.tokishu.jdkit.command.interceptor.CommandInterceptor
import dev.tokishu.jdkit.command.internal.CommandWrapper
import dev.tokishu.jdkit.config.JDKitProperties
import dev.tokishu.jdkit.locale.LocalizationManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class UserPermissionInterceptor(private val config: JDKitProperties) : CommandInterceptor {

    override fun preHandle(event: SlashCommandInteractionEvent, wrapper: CommandWrapper): Boolean {
        if (!event.isFromGuild) return true
        
        val method = wrapper.method
        val clazz = wrapper.instance.javaClass
        
        val reqPermClass = clazz.getAnnotation(RequiresPermission::class.java)
        val reqPermMethod = method.getAnnotation(RequiresPermission::class.java)
        val perms = mutableListOf<Permission>()
        reqPermClass?.permissions?.let { perms.addAll(it) }
        reqPermMethod?.permissions?.let { perms.addAll(it) }
        
        val member = event.member ?: return true
        
        if (perms.isNotEmpty() && !member.hasPermission(perms)) {
            val userLang = event.userLocale.locale.split("-")[0]
            val locale = if (config.locale.preferUser) userLang else config.locale.default
            
            val msg = LocalizationManager.getMessage("error.no_permission", "core", locale, config.locale.default)
            event.reply(msg).setEphemeral(true).queue()
            return false
        }
        
        return true
    }
}
