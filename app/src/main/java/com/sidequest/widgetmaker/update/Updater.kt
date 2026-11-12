package com.sidequest.widgetmaker.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import com.sidequest.widgetmaker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Checks GitHub for a newer build, downloads it, and hands it to Android's installer. */
object Updater {
    private const val LATEST = "https://api.github.com/repos/Sync9797/AI-widget-Maker/releases/latest"
    private const val PREFS = "updates"
    private const val FILE = "AIWidgetMaker-update.apk"
    private val http = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()

    sealed interface Result {
        data class Available(val release: Release) : Result
        data object UpToDate : Result
        data class Failed(val why: String) : Result
    }

    val currentVersion: String get() = BuildConfig.VERSION_NAME

    /** Whether to check each time the app opens (Settings). On unless turned off. */
    fun checkOnStart(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("on_start", true)

    fun setCheckOnStart(context: Context, on: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("on_start", on).apply()

    /** Asks GitHub for the newest build and compares it with this one. */
    suspend fun check(): Result = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(LATEST)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "AIWidgetMaker")
                .build()
            http.newCall(request).execute().use { response ->
                when {
                    // Briefly missing while a new build is being published.
                    response.code == 404 -> Result.Failed("No build is published right now. Try again in a few minutes.")
                    !response.isSuccessful -> Result.Failed("GitHub said ${response.code}.")
                    else -> {
                        val release = UpdateMath.parse(response.body?.string().orEmpty())
                            ?: return@use Result.Failed("The latest build has no APK yet. Try again in a few minutes.")
                        if (UpdateMath.isNewer(release, BuildConfig.GIT_SHA, BuildConfig.VERSION_NAME)) Result.Available(release) else Result.UpToDate
                    }
                }
            }
        } catch (e: IOException) {
            Result.Failed("Couldn't reach GitHub. Check your internet connection.")
        }
    }

    /** Whether Android lets this app open the installer ("Install unknown apps"). */
    fun canInstall(context: Context) = context.packageManager.canRequestPackageInstalls()

    /** The screen to allow installing updates from this app. */
    fun allowInstallIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))

    /**
     * Downloads the new APK (reporting progress 0-100) and returns the intent
     * that opens Android's installer for it, or null if the download failed.
     */
    suspend fun download(context: Context, release: Release, progress: (Int) -> Unit): Intent? {
        val manager = context.getSystemService(DownloadManager::class.java)
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FILE).delete()
        val id = manager.enqueue(
            DownloadManager.Request(Uri.parse(release.apkUrl))
                .setTitle("AI Widget Maker ${release.version.orEmpty()}".trim())
                .setMimeType("application/vnd.android.package-archive")
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, FILE),
        )
        while (true) {
            delay(500)
            val status = manager.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                if (!c.moveToFirst()) return null
                val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                if (total > 0) progress((done * 100 / total).toInt())
                c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            } ?: return null
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val uri = manager.getUriForDownloadedFile(id) ?: return null
                    return Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                DownloadManager.STATUS_FAILED -> return null
            }
        }
    }
}
