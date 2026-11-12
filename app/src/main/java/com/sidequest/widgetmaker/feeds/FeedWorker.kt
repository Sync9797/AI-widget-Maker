package com.sidequest.widgetmaker.feeds

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sidequest.widgetmaker.widget.AiWidget
import java.util.concurrent.TimeUnit

/** Refreshes the data sources in the background and redraws the widgets. */
class FeedWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val feeds = Feeds(applicationContext)
        val s = feeds.settings
        if (!(s.weatherReady || s.cryptoReady || s.trafficReady || s.newsReady || s.eventsReady || s.calendarReady || s.healthReady || s.monsterQuest)) {
            return Result.success()
        }
        feeds.refresh(inputData.getStringArray(ONLY)?.toSet())
        AiWidget.refreshAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "feeds-periodic"
        private const val NOW = "feeds-now"
        private const val ONLY = "only"

        /** Refreshes just some feeds soon (like Monster Quest when the game saves). */
        fun refreshSoon(context: Context, only: Set<String>) {
            val request = OneTimeWorkRequestBuilder<FeedWorker>()
                .setInputData(androidx.work.workDataOf(ONLY to only.toTypedArray()))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("$NOW-${only.sorted().joinToString("-")}", ExistingWorkPolicy.REPLACE, request)
        }
        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Every 30 minutes while online (Android allows 15 at the most often). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FeedWorker>(30, TimeUnit.MINUTES)
                .setConstraints(online)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun refreshSoon(context: Context) {
            val request = OneTimeWorkRequestBuilder<FeedWorker>().setConstraints(online).build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
