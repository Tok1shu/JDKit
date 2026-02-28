package dev.tokishu.jdkit.presence

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity

/**
 * Manager for handling the bot's presence (Status and Activity).
 */
class PresenceManager(private val jda: JDA) {

    /**
     * Updates the online status of the bot.
     */
    fun setStatus(status: OnlineStatus) {
        jda.presence.setStatus(status)
    }

    /**
     * Updates the activity (playing, watching, listening) of the bot.
     */
    fun setActivity(activity: Activity?) {
        jda.presence.activity = activity
    }
}
