package com.sidequest.widgetmaker.music

/** What's playing, as the music widget shows it. */
data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String,
    val packageName: String,
    val appName: String,
    val playing: Boolean,
)

/** Turns what's playing into widget values. Free of Android, so it can be tested. */
object MusicMath {
    fun values(now: NowPlaying?, hasAccess: Boolean): Map<String, String> {
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
        )
    }
}
