package com.sidequest.widgetmaker.ui

import android.content.Context
import com.sidequest.widgetmaker.spec.Tokens
import com.sidequest.widgetmaker.widget.AiWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime

/**
 * The live values every preview in the app draws with, worked out once in the background and shared.
 *
 * Working them out reads the saved feeds, the phone's battery and storage, what's playing, and
 * asks not-NOOP for its snapshot (which can start not-NOOP and read its database). Each preview
 * used to do all of that itself, on the main thread, so a screen of previews froze the app.
 * They refresh every 30 seconds while anything shows them, and stop 5 seconds after nothing does.
 */
object PreviewTokens {
    private const val REFRESH_MS = 30_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var shared: StateFlow<Tokens>? = null

    /** Shown until the first values are ready: the time, with nothing from the data sources yet. */
    private fun placeholder() = Tokens(now = LocalDateTime.now(), battery = null, charging = false)

    fun state(context: Context): StateFlow<Tokens> = shared ?: synchronized(this) {
        shared ?: flow {
            val app = context.applicationContext
            while (true) {
                emit(runCatching { AiWidget.currentTokens(app) }.getOrElse { placeholder() })
                delay(REFRESH_MS)
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), placeholder()).also { shared = it }
    }
}
