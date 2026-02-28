package dev.tokishu.jdkit.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.cdimascio.dotenv.dotenv
import java.io.InputStream
import java.util.regex.Pattern

import com.fasterxml.jackson.databind.PropertyNamingStrategies

import org.slf4j.LoggerFactory

/**
 * Utility to load the bot configuration from application.yml and .env variables.
 */
object ConfigLoader {

    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

    private val mapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
    
    // Pattern to find placeholders like ${DISCORD_TOKEN}
    private val ENV_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}")

    /**
     * Loads the JDKitProperties from the given resource path.
     * Replaces variable Placeholders (e.g. ${DISCORD_TOKEN}) with values from the .env file or environment.
     */
    fun loadConfig(resourcePath: String = "/application.yml"): dev.tokishu.jdkit.config.JDKitProperties {
        // Load the dotenv file (silently ignores if not found in root dir)
        val dotenv = dotenv {
            ignoreIfMissing = true
        }
        
        val stream: InputStream? = dev.tokishu.jdkit.config.JDKitProperties::class.java.getResourceAsStream(resourcePath)
        
        return if (stream != null) {
            // Read YAML to string to process ENV replacements before parsing with Jackson
            val yamlString = stream.reader().readText()
            var processedYaml = yamlString

            val matcher = ENV_PATTERN.matcher(yamlString)
            while (matcher.find()) {
                val envVarName = matcher.group(1)
                // First try getting from .env file, then fallback to System Environment
                val envValue = dotenv[envVarName] ?: System.getenv(envVarName) ?: ""
                
                processedYaml = processedYaml.replace("\${$envVarName}", envValue)
            }
            
            val rootNode = mapper.readTree(processedYaml)
            val jdkitNode = rootNode.get("jdkit")
            
            if (jdkitNode != null) {
                mapper.treeToValue(jdkitNode, dev.tokishu.jdkit.config.JDKitProperties::class.java)
            } else {
                mapper.treeToValue(rootNode, dev.tokishu.jdkit.config.JDKitProperties::class.java)
            }
        } else {
            logger.warn("Configuration file {} not found! Using default empty config.", resourcePath)
            dev.tokishu.jdkit.config.JDKitProperties()
        }
    }
}
