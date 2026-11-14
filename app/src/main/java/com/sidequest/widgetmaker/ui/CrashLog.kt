package com.sidequest.widgetmaker.ui

import android.content.Context
import java.io.File

/**
 * Keeps what went wrong when the app crashes (on this phone only), so the next start can offer it to
 * copy and send: a crash on one phone, with one person's designs and data, can't otherwise be traced.
 */
object CrashLog {
    private const val FILE = "last_crash.txt"

    /** Records an uncaught crash, then lets Android handle it as before (the app still closes). */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is Handler) return
        Thread.setDefaultUncaughtExceptionHandler(Handler(app, previous))
    }

    /** The last crash's details, or null when there's none to show. */
    fun last(context: Context): String? = File(context.filesDir, FILE).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        File(context.filesDir, FILE).delete()
    }

    private class Handler(val app: Context, val previous: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, error: Throwable) {
            runCatching {
                val version = com.sidequest.widgetmaker.update.Updater.currentVersion
                File(app.filesDir, FILE).writeText(
                    "AI Widget Maker $version, Android ${android.os.Build.VERSION.RELEASE} (${android.os.Build.MODEL})\n" +
                        error.stackTraceToString().take(12_000),
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }
}
