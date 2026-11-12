package com.sidequest.widgetmaker.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

/**
 * Notification access lets the app see the phone's media sessions: this
 * follows them and keeps the music widgets up to date. It doesn't read
 * notifications themselves.
 */
class MusicListener : NotificationListenerService() {
    private var sessions: MediaSessionManager? = null
    private var watched: MediaController? = null

    private val onSessions = MediaSessionManager.OnActiveSessionsChangedListener { follow() }

    private val onChange = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = follow()
        override fun onMetadataChanged(metadata: MediaMetadata?) = follow()
        override fun onSessionDestroyed() = follow()
    }

    override fun onListenerConnected() {
        val manager = getSystemService(MediaSessionManager::class.java)
        sessions = manager
        runCatching { manager.addOnActiveSessionsChangedListener(onSessions, Music.listener(this)) }
        follow()
    }

    override fun onListenerDisconnected() {
        runCatching { sessions?.removeOnActiveSessionsChangedListener(onSessions) }
        watched?.unregisterCallback(onChange)
        watched = null
    }

    /** Watches the session that matters now and saves what it's playing. */
    private fun follow() {
        val c = Music.controller(this)
        if (c?.sessionToken != watched?.sessionToken) {
            watched?.unregisterCallback(onChange)
            c?.registerCallback(onChange)
            watched = c
        }
        Music.save(this, c)
    }
}

/** The widget's play/pause, next and previous buttons. */
class MusicControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intent.getStringExtra(COMMAND)?.let { Music.control(context, it) }
    }

    companion object {
        const val COMMAND = "command"

        fun intent(context: Context, command: String): Intent =
            Intent(context, MusicControlReceiver::class.java)
                .putExtra(COMMAND, command)
                .setData(android.net.Uri.parse("aiwidget://music/$command"))
    }
}
