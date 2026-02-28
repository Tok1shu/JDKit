package dev.tokishu.jdkit.schedule.annotation

import java.util.concurrent.TimeUnit

/**
 * Annotation to schedule a repeating task on a method.
 * 
 * @param every Initial delay and subsequent interval.
 * @param unit The TimeUnit for the interval.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class JDKitScheduled(
    val every: Long,
    val unit: TimeUnit = TimeUnit.SECONDS
)
