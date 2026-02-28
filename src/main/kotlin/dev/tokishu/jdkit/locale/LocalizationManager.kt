package dev.tokishu.jdkit.locale

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.dv8tion.jda.api.interactions.DiscordLocale
import java.io.InputStream

import org.slf4j.LoggerFactory

/**
 * Manages localization strings loaded from JSON files in the resources/locales folder.
 */
object LocalizationManager {

    private val logger = LoggerFactory.getLogger(LocalizationManager::class.java)

    private val mapper = ObjectMapper()
    
    // Structure: map[localeString][scope][key] = value
    // Scope can be "core" or "app"
    private val translations = mutableMapOf<String, Map<String, Map<String, String>>>()

    /**
     * Tries to load locales from the classpath (resources/locales).
     */
    fun loadLocales() {
        logger.info("Loading translations from resources/locales...")
        
        val supportedLocales = listOf("en", "ru")
        
        for (lang in supportedLocales) {
            val resourcePath = "/locales/$lang.json"
            val stream: InputStream? = javaClass.getResourceAsStream(resourcePath)
            if (stream != null) {
                try {
                    val typeRef = object : TypeReference<Map<String, Map<String, String>>>() {}
                    val map = mapper.readValue(stream, typeRef)
                    translations[lang] = map
                    logger.info("Loaded locale '{}' with scopes: {}", lang, map.keys)
                } catch (e: Exception) {
                    logger.error("Failed to parse locale {}", resourcePath, e)
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Gets a translated string for the given locale.
     * Falls back to the default locale (defined in config) if not found.
     */
    fun getMessage(key: String, scope: String = "core", locale: String = "en", defaultLocale: String = "en"): String {
        return translations[locale]?.get(scope)?.get(key) 
            ?: translations[defaultLocale]?.get(scope)?.get(key) 
            ?: key
    }

    /**
     * Builds a map of translations for a specific key across all loaded DiscordLocales.
     * Useful for setting localizations on JDA SlashCommandData.
     */
    fun getLocalizationsForKey(key: String, scope: String = "app"): Map<DiscordLocale, String> {
        val result = mutableMapOf<DiscordLocale, String>()
        
        translations.forEach { (lang, scopesMap) ->
            val discordLocale = DiscordLocale.from(lang)
            if (discordLocale != DiscordLocale.UNKNOWN) {
                scopesMap[scope]?.get(key)?.let { translation ->
                    result[discordLocale] = translation
                }
            }
        }
        
        return result
    }
}
