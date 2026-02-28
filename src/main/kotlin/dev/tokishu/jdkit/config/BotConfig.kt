package dev.tokishu.jdkit.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Spring Boot Configuration Properties representing jdkit: prefix from application.yml.
 */
@ConfigurationProperties(prefix = "jdkit")
class JDKitProperties {
    var token: String = ""
    var guild: GuildProperties = GuildProperties()
    var locale: LocaleProperties = LocaleProperties()

    class LocaleProperties {
        var default: String = "ru"
        var preferUser: Boolean = true
    }

    class GuildProperties {
        var main: String = ""
        var onlyMainGuild: Boolean = false
    }
}
