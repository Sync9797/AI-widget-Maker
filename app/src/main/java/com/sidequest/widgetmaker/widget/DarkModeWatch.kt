package com.sidequest.widgetmaker.widget

import android.content.Context
import android.provider.Settings
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration

/**
 * Redraws the widgets when the phone switches between light and dark mode, so the frosted glass follows it
 * (Settings → Widget look → Glass tone). The glass is a picture the app draws, so the launcher can't swap it
 * by itself. Android wakes the app when the dark mode setting changes, even if the app isn't running: the
 * standard setting, and Samsung's own. (A dark mode on a schedule changes no setting when it flips, so then
 * the widgets catch up at their next refresh.)
 */
class DarkModeWatch(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        AiWidget.refreshAll(applicationContext)
        watch(applicationContext, again = true) // A content trigger fires once; set it again for the next switch.
        return Result.success()
    }

    companion object {
        private const val NAME = "dark-mode-watch"

        /** Starts watching (again); harmless to call often. */
        fun watch(context: Context, again: Boolean = false) {
            val constraints = Constraints.Builder()
                .addContentUriTrigger(Settings.Secure.getUriFor("ui_night_mode"), false)
                .addContentUriTrigger(Settings.System.getUriFor("display_night_theme"), false) // Samsung
                // Give the phone a moment to finish switching, so the redraw sees the new mode.
                .setTriggerContentUpdateDelay(Duration.ofSeconds(1))
                .setTriggerContentMaxDelay(Duration.ofSeconds(5))
                .build()
            val request = OneTimeWorkRequestBuilder<DarkModeWatch>().setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, if (again) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP, request)
        }
    }
}
