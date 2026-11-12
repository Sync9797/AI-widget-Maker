package com.sidequest.widgetmaker.music

/** What's playing, as the music widget shows it. */
data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String,
    val packageName: String,
    val appName: String,
    val playing: Boolean,
    /** Where the song was, and when (SystemClock.elapsedRealtime), so the position can be worked out later. */
    val positionMs: Long = 0,
    val updatedAt: Long = 0,
    val durationMs: Long = 0,
)

/** Turns what's playing into widget values. Free of Android, so it can be tested. */
object MusicMath {
    /** How far into the song it is at [elapsedNow] (SystemClock.elapsedRealtime). */
    fun position(now: NowPlaying, elapsedNow: Long): Long {
        val moved = if (now.playing && now.updatedAt > 0) (elapsedNow - now.updatedAt).coerceAtLeast(0) else 0
        val at = now.positionMs + moved
        return if (now.durationMs > 0) at.coerceAtMost(now.durationMs) else at
    }

    fun clock(ms: Long): String {
        val s = ms / 1000
        return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
    }

    fun values(now: NowPlaying?, hasAccess: Boolean, elapsedNow: Long = now?.updatedAt ?: 0): Map<String, String> {
        if (now == null) {
            val text = if (hasAccess) "Nothing playing" else "Allow music access"
            return mapOf(
                "music.title" to text,
                "music.artist" to if (hasAccess) "" else "In this app's Settings",
                "music.album" to "",
                "music.app" to "",
                "music.state" to "",
                "music.line" to text,
                "music.playIcon" to "play",
            )
        }
        return mapOf(
            "music.title" to now.title.ifBlank { now.appName },
            "music.artist" to now.artist,
            "music.album" to now.album,
            "music.app" to now.appName,
            "music.state" to if (now.playing) "Playing" else "Paused",
            "music.line" to listOf(now.title, now.artist).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { now.appName },
            "music.playIcon" to if (now.playing) "pause" else "play",
            // Set only while something can be controlled, for "show":{"ifSet":"{music.playing}"}.
            "music.playing" to if (now.playing) "yes" else "",
        ) + if (now.durationMs > 0) {
            val at = position(now, elapsedNow)
            mapOf(
                "music.length" to clock(now.durationMs),
                "music.elapsed" to clock(at),
                "music.progress" to (at * 100 / now.durationMs).coerceIn(0, 100).toString(),
            )
        } else emptyMap()
    }
}
