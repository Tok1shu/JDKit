package dev.tokishu.jdkit.di

import org.slf4j.LoggerFactory

/**
 * Annotation to mark fields that should be automatically injected by the framework.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Inject

/**
 * A lightweight IoC container for the DiscordBotCore framework.
 * Allows extensions to register services, databases, and configuration objects,
 * which will then be injected into Commands and Events.
 */
object DependencyContainer {
    
    private val logger = LoggerFactory.getLogger(DependencyContainer::class.java)
    
    /**
     * Bridges JDKit's internal DI with Spring Boot's ApplicationContext.
     * This is automatically set by the JDKitAutoConfiguration during startup.
     */
    var applicationContext: org.springframework.context.ApplicationContext? = null
    
    @PublishedApi
    internal val fallbackDependencies = mutableMapOf<Class<*>, Any>()
    
    /**
     * Registers a dependency manually if not using Spring Beans.
     */
    fun <T : Any> register(clazz: Class<T>, instance: T) {
        fallbackDependencies[clazz] = instance
    }

    inline fun <reified T : Any> register(instance: T) {
        fallbackDependencies[T::class.java] = instance
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: Class<T>): T? {
        return try {
            applicationContext?.getBean(clazz) ?: fallbackDependencies[clazz] as T?
        } catch (e: Exception) {
            fallbackDependencies[clazz] as T?
        }
    }
    
    inline fun <reified T : Any> get(): T? {
        return get(T::class.java)
    }

    /**
     * Scans the given instance for @Inject fields and populates them natively
     * using the injected Spring ApplicationContext.
     */
    fun injectInto(instance: Any) {
        if (applicationContext == null) return
        
        var currentClass: Class<*>? = instance.javaClass
        while (currentClass != null && currentClass != Any::class.java) {
            for (field in currentClass.declaredFields) {
                if (field.isAnnotationPresent(Inject::class.java)) {
                    try {
                        // Attempt to fetch from Spring First
                        val dependency = get(field.type)
                        if (dependency != null) {
                            field.isAccessible = true
                            field.set(instance, dependency)
                        } else {
                            logger.warn("Spring bean for type {} not found for {}", field.type.name, instance.javaClass.simpleName)
                        }
                    } catch (e: Exception) {
                        logger.error("Error injecting {} into {}", field.type.simpleName, instance.javaClass.simpleName, e)
                    }
                }
            }
            currentClass = currentClass.superclass
        }
    }

    /**
     * Checks if the given instance is a BotComponent and dynamically injects
     * the provided JDA instance into its protected `_jda` base field.
     */
    fun injectJda(instance: Any, jda: net.dv8tion.jda.api.JDA) {
        if (instance is dev.tokishu.jdkit.BotComponent) {
            var currentClass: Class<*>? = instance.javaClass
            var field: java.lang.reflect.Field? = null
            while (currentClass != null) {
                try {
                    field = currentClass.getDeclaredField("_jda")
                    break
                } catch (e: NoSuchFieldException) {
                    currentClass = currentClass.superclass
                }
            }
            if (field != null) {
                field.isAccessible = true
                field.set(instance, jda)
            } else {
                logger.warn("Could not find '_jda' field in BotComponent {}", instance.javaClass.name)
            }
        }
    }
}
