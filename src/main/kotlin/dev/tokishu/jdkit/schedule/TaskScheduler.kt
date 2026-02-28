package dev.tokishu.jdkit.schedule

import dev.tokishu.jdkit.BotComponent
import dev.tokishu.jdkit.di.DependencyContainer
import dev.tokishu.jdkit.schedule.annotation.JDKitScheduled
import net.dv8tion.jda.api.JDA
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

import org.slf4j.LoggerFactory

class TaskScheduler : dev.tokishu.jdkit.extension.BotExtension {

    private val logger = LoggerFactory.getLogger(TaskScheduler::class.java)

    private lateinit var jda: JDA

    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    override fun onEnable(jda: JDA, config: dev.tokishu.jdkit.config.JDKitProperties, basePackage: String) {
        this.jda = jda
        registerTasks(basePackage)
    }

    private fun registerTasks(packageName: String) {
        val reflections = Reflections(packageName, Scanners.MethodsAnnotated)
        val methods = reflections.getMethodsAnnotatedWith(JDKitScheduled::class.java)

        val instances = mutableMapOf<Class<*>, Any>()

        for (method in methods) {
            val annotation = method.getAnnotation(JDKitScheduled::class.java)
            val clazz = method.declaringClass

            val instance = instances.getOrPut(clazz) {
                val obj = clazz.getDeclaredConstructor().newInstance()
                DependencyContainer.injectJda(obj, jda)
                obj
            }

            scheduler.scheduleAtFixedRate({
                try {
                    method.invoke(instance)
                } catch (e: Exception) {
                    logger.error("Error executing JDKitScheduled task in {}.{}", clazz.simpleName, method.name, e)
                    e.printStackTrace()
                }
            }, annotation.every, annotation.every, annotation.unit)

            logger.info("Registered @JDKitScheduled task {}.{} running every {} {}", clazz.simpleName, method.name, annotation.every, annotation.unit)
        }
    }
    
    fun shutdown() {
        scheduler.shutdown()
    }
}
