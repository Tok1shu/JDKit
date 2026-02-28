package dev.tokishu.jdkit.command

import dev.tokishu.jdkit.BotComponent
import net.dv8tion.jda.api.JDA

/**
 * Base class for all Discord commands.
 * Provides access to the [JDA] instance.
 * 
 * Commands should inherit from this class and declare an `execute` function
 * which will be invoked dynamically based on its parameters.
 */
abstract class BaseCommand : BotComponent {
    
    // Internal property to allow the core to set JDA, while exposing only the getter through BotComponent
    internal lateinit var _jda: JDA
    
    override val jda: JDA
        get() = _jda
}