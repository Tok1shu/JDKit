package dev.tokishu.jdkit.event.annotation

/**
 * Marks a class as an Event listener container.
 * The EventManager will scan for classes with this annotation,
 * instantiate them, and register them with JDA's AnnotatedEventManager.
 * Inside such class, use JDA's @SubscribeEvent on methods taking Event parameters.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class JDKitEvent
