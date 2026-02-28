package dev.tokishu.jdkit.extension

import dev.tokishu.jdkit.config.JDKitProperties
import net.dv8tion.jda.api.JDA

/**
 * Base interface for all JDKit Extensions and Managers.
 * Extensions are automatically discovered and initialized during startup.
 */
interface BotExtension {

    /**
     * Called when the extension is being loaded and initialized.
     * 
     * @param jda The JDA instance.
     * @param config The loaded JDKitProperties.
     * @param basePackage The base package configured via @RunJDKit or "dev" fallback.
     */
    fun onEnable(jda: JDA, config: JDKitProperties, basePackage: String)
}
