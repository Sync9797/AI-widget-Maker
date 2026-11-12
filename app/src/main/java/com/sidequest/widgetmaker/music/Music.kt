package com.sidequest.widgetmaker.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import com.sidequest.widgetmaker.widget.AiWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * What's playing on the phone, from any music, podcast or video app (Android's
 * media sessions, which need notification access), and its controls.
 */
object Music {
    private const val PREFS = "music"
    private const val ART = "music_art.png"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pending: Job? = null

    fun hasAccess(context: Context) = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /** Opens the screen to allow notification access for this app. */
    fun accessIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener(context).flattenToString())
        } else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    /** The app's info screen, where Android 13+ hides "Allow restricted settings" for apps not from a store. */
    fun appInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + context.packageName))

    fun listener(context: Context) = ComponentName(context, MusicListener::class.java)

    fun current(context: Context): NowPlaying? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pkg = p.getString("package", null) ?: return null
        return NowPlaying(
            title = p.getString("title", "").orEmpty(),
            artist = p.getString("artist", "").orEmpty(),
            album = p.getString("album", "").orEmpty(),
            packageName = pkg,
            appName = p.getString("app", "").orEmpty(),
            playing = p.getBoolean("playing", false),
            positionMs = p.getLong("position", 0),
            updatedAt = p.getLong("updatedAt", 0),
            durationMs = p.getLong("duration", 0),
        )
    }

    fun values(context: Context): Map<String, String> =
        MusicMath.values(current(context), hasAccess(context), SystemClock.elapsedRealtime())

    /** The album art, if what's playing has one. */
    fun art(context: Context): Bitmap? {
        val file = File(context.filesDir, ART)
        if (current(context) == null || !file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()
    }

    /** The session to show and control: the one playing, else the last one used. */
    fun controller(context: Context): MediaController? {
        val sessions = runCatching {
            context.getSystemService(MediaSessionManager::class.java).getActiveSessions(listener(context))
        }.getOrNull().orEmpty()
        val last = current(context)?.packageName
        return sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull { it.packageName == last }
            ?: sessions.firstOrNull()
    }

    /** Saves what [controller] is playing (or that nothing is) and redraws the widgets if it changed. */
    fun save(context: Context, controller: MediaController?) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val meta = controller?.metadata
        val now = controller?.let {
            NowPlaying(
                title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                artist = (meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty(),
                album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
                packageName = it.packageName,
                appName = label(context, it.packageName),
                playing = it.playbackState?.state in setOf(PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING),
                positionMs = it.playbackState?.position?.coerceAtLeast(0) ?: 0,
                updatedAt = it.playbackState?.lastPositionUpdateTime?.takeIf { t -> t > 0 } ?: SystemClock.elapsedRealtime(),
                durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0) ?: 0,
            )
        }
        if (now == current(context)) return
        val trackChanged = now?.title != current(context)?.title || now?.packageName != current(context)?.packageName
        if (now == null) p.edit().clear().apply() else p.edit()
            .putString("title", now.title).putString("artist", now.artist).putString("album", now.album)
            .putString("package", now.packageName).putString("app", now.appName).putBoolean("playing", now.playing)
            .putLong("position", now.positionMs).putLong("updatedAt", now.updatedAt).putLong("duration", now.durationMs)
            .apply()
        tick(context, now?.playing == true)
        if (trackChanged) saveArt(context, meta)
        redrawSoon(context)
    }

    private fun saveArt(context: Context, meta: MediaMetadata?) {
        val file = File(context.filesDir, ART)
        val art = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (art == null) { file.delete(); return }
        // Small enough for a widget update (they're limited in size), sharp enough for a big tile.
        val side = 300
        val scaled = Bitmap.createScaledBitmap(art, side, side * art.height / maxOf(1, art.width), true)
        runCatching { file.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }

    /** Waits for a burst of changes (a new track sends several) to settle, then redraws once. */
    private fun redrawSoon(context: Context) {
        val app = context.applicationContext
        pending?.cancel()
        pending = scope.launch {
            delay(400)
            AiWidget.refreshAll(app)
        }
    }

    private var ticker: Job? = null

    /**
     * While music plays, moves music progress bars along every 20 seconds
     * (only widgets that show one; a timer element counts by itself).
     */
    private fun tick(context: Context, playing: Boolean) {
        ticker?.cancel()
        if (!playing) return
        val app = context.applicationContext
        ticker = scope.launch {
            while (true) {
                delay(20_000)
                val store = com.sidequest.widgetmaker.data.DesignStore(app)
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(app)
                manager.getGlanceIds(AiWidget::class.java).forEach { id ->
                    val design = store.designFor(manager.getAppWidgetId(id))?.design ?: return@forEach
                    if (com.sidequest.widgetmaker.spec.DesignParser.encode(design).contains("music.progress")) AiWidget.refresh(app, id)
                }
            }
        }
    }

    private fun label(context: Context, pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    /** Play/pause, next or previous: on the session directly, or as a media key if there's no access. */
    fun control(context: Context, command: String) {
        val c = controller(context)
        if (c != null) {
            val t = c.transportControls
            when (command) {
                "toggle" -> if (c.playbackState?.state == PlaybackState.STATE_PLAYING) t.pause() else t.play()
                "next" -> t.skipToNext()
                "prev" -> t.skipToPrevious()
            }
            return
        }
        val code = when (command) {
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        val audio = context.getSystemService(AudioManager::class.java)
        val time = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, code, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_UP, code, 0))
    }
}
