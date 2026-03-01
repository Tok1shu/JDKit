package dev.tokishu.jdkit

import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import java.util.Properties

/**
 * Isolated class to launch Spring Boot.
 * This is kept completely separate from JDKitApplication so the JVM classloader
 * doesn't try to link Spring imports when Spring is absent (CompileOnly safety).
 */
object SpringLauncher {
    @JvmStatic
    fun launch(callerClass: Class<*>, args: Array<String>, basePackage: String) {
        val app = SpringApplication(callerClass)
        app.setWebApplicationType(WebApplicationType.NONE)

        // Pass the base package down to the AutoConfiguration
        val props = Properties()
        props.setProperty("jdkit.base-package", basePackage)
        app.setDefaultProperties(props)
        
        app.run(*args)
    }
}
