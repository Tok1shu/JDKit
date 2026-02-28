package dev.tokishu.jdkit.command.annotation.cooldown

import dev.tokishu.jdkit.command.interceptor.CommandInterceptor
import dev.tokishu.jdkit.command.internal.CommandWrapper
import dev.tokishu.jdkit.locale.LocalizationManager
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.util.concurrent.ConcurrentHashMap
import dev.tokishu.jdkit.config.JDKitProperties

class CooldownInterceptor(private val config: JDKitProperties) : CommandInterceptor {
    
    // Cooldown map: key = "userId_commandName", value = Timestamp of expiry
    private val cooldowns = ConcurrentHashMap<String, Long>()

    override fun preHandle(event: SlashCommandInteractionEvent, wrapper: CommandWrapper): Boolean {
        val method = wrapper.method
        val clazz = wrapper.instance.javaClass
        
        val cdClass = clazz.getAnnotation(Cooldown::class.java)
        val cdMethod = method.getAnnotation(Cooldown::class.java)
        val cooldownSeconds = cdMethod?.seconds ?: cdClass?.seconds ?: return true
        
        val key = "${event.user.id}_${event.commandString}"
        val now = System.currentTimeMillis()
        
        val expiry = cooldowns[key]
        if (expiry != null && now < expiry) {
            val remaining = (expiry - now) / 1000
            
            // Extract base language (e.g. en-US -> en)
            val userLang = event.userLocale.locale.split("-")[0]
            val locale = if (config.locale.preferUser) userLang else config.locale.default
            
            val msg = LocalizationManager.getMessage("error.cooldown", "core", locale, config.locale.default)
                .replace("%s", remaining.toString())
            event.reply(msg).setEphemeral(true).queue()
            return false
        }
        
        cooldowns[key] = now + (cooldownSeconds * 1000)
        return true
    }
}
