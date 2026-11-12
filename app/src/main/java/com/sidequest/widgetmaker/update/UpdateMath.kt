package com.sidequest.widgetmaker.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** The newest build on GitHub. Free of Android, so it can be tested. */
data class Release(
    /** The commit it was built from. */
    val sha: String,
    /** Like "0.9.3", when the release title has one. */
    val version: String?,
    val apkUrl: String,
)

object UpdateMath {
    private val json = Json { ignoreUnknownKeys = true }
    private val VERSION = Regex("\\b(\\d+\\.\\d+(?:\\.\\d+)?)\\b")

    /** GitHub's "latest release" response to a [Release], or null if it has no APK. */
    fun parse(body: String): Release? {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return null
        fun str(o: JsonObject, key: String) = o[key]?.jsonPrimitive?.contentOrNull
        val apk = (root["assets"] as? JsonArray)?.mapNotNull { it as? JsonObject }
            ?.firstOrNull { str(it, "name")?.endsWith(".apk") == true }
            ?.let { str(it, "browser_download_url") } ?: return null
        val sha = str(root, "target_commitish").orEmpty()
        val version = str(root, "name")?.let { VERSION.find(it)?.value }
        return Release(sha, version, apk)
    }

    /**
     * Whether [release] is newer than this build: a different commit when this
     * build knows its own, else a higher version number.
     */
    fun isNewer(release: Release, mySha: String, myVersion: String): Boolean {
        if (mySha.isNotBlank() && mySha != "dev" && release.sha.isNotBlank()) return release.sha != mySha
        val theirs = release.version ?: return false
        return compare(theirs, myVersion) > 0
    }

    fun compare(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }
}
