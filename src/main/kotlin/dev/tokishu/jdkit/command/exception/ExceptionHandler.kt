package dev.tokishu.jdkit.command.exception

import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

/**
 * Global exception handler for the DiscordBotCore framework.
 * You can register your custom implementation in the DependencyContainer
 * to handle all uncaught exceptions during command or component execution.
 */
interface ExceptionHandler {
    
    /**
     * Called when an unhandled exception occurs during interaction processing.
     * 
     * @param e The exception that was thrown.
     * @param event The interaction event. You can use it to reply to the user.
     */
    fun handle(e: Exception, event: IReplyCallback)
}
