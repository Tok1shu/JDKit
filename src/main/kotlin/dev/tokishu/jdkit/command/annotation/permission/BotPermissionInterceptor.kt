package dev.tokishu.jdkit.command.annotation.permission

import dev.tokishu.jdkit.command.interceptor.CommandInterceptor
import dev.tokishu.jdkit.command.internal.CommandWrapper
import dev.tokishu.jdkit.config.JDKitProperties
import dev.tokishu.jdkit.locale.LocalizationManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class BotPermissionInterceptor(private val config: JDKitProperties) : CommandInterceptor {

    override fun preHandle(event: SlashCommandInteractionEvent, wrapper: CommandWrapper): Boolean {
        if (!event.isFromGuild) return true
        
        val method = wrapper.method
        val clazz = wrapper.instance.javaClass
        
        val reqBotClass = clazz.getAnnotation(RequiresBotPermission::class.java)
        val reqBotMethod = method.getAnnotation(RequiresBotPermission::class.java)
        val reqBotPerms = mutableListOf<Permission>()
        reqBotClass?.permissions?.let { reqBotPerms.addAll(it) }
        reqBotMethod?.permissions?.let { reqBotPerms.addAll(it) }
        
        val guild = event.guild ?: return true
        val selfMember = guild.selfMember

        if (reqBotPerms.isNotEmpty() && !selfMember.hasPermission(reqBotPerms)) {
            val userLang = event.userLocale.locale.split("-")[0]
            val locale = if (config.locale.preferUser) userLang else config.locale.default
            
            val msg = LocalizationManager.getMessage("error.no_bot_permission", "core", locale, config.locale.default)
            event.reply(msg).setEphemeral(true).queue()
            return false
        }
        
        return true
    }
}
