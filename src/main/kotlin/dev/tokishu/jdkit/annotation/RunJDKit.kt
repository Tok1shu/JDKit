package dev.tokishu.jdkit.annotation

/**
 * Marks the main class or main function of a JDKit application.
 * If Spring Boot is present on the classpath, this will trigger the Spring Boot lifecycle.
 * Otherwise, the native native JDKit lifecycle will be used.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RunJDKit(
    val basePackage: String = ""
)
