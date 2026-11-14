package com.sidequest.widgetmaker.feeds

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * The Nearby traffic map: TomTom's map with its live traffic laid over it, centered on your home (or your
 * town), with a dot there. Built from map tiles each time the data refreshes and kept as a picture; the
 * home screen reads it through a file link (a picture this big sent with the widget would be too big for
 * Android to deliver).
 */
object TrafficMap {
    const val ZOOM = 14
    const val WIDTH = 960
    const val HEIGHT = 600

    private const val NAME = "traffic.png"

    fun file(context: Context) = File(File(context.filesDir, "maps").apply { mkdirs() }, NAME)

    /** Where the map centers: home, else your town. */
    fun center(s: FeedSettings): Place? = s.home ?: s.place

    fun ready(s: FeedSettings) = s.tomtomKey.isNotBlank() && center(s) != null

    /** Fetches the tiles and saves the map. Returns the values for widgets ("map.traffic": when it was made). */
    fun refresh(context: Context, s: FeedSettings, http: OkHttpClient): Map<String, String> {
        val place = center(s) ?: throw IOException("Set your home address or town first.")
        val key = s.tomtomKey.trim()
        val (cx, cy) = MapMath.pixel(place.lat, place.lon, ZOOM)
        val out = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(0xFFE8E8E8.toInt())
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val tiles = MapMath.tiles(cx, cy, WIDTH, HEIGHT, ZOOM)
        for (layer in listOf("map", "traffic")) {
            tiles.forEach { t ->
                val url = HttpUrl.Builder().scheme("https").host("api.tomtom.com").apply {
                    if (layer == "map") addPathSegments("map/1/tile/basic/main/$ZOOM/${t.x}/${t.y}.png")
                    else addPathSegments("traffic/map/4/tile/flow/relative0/$ZOOM/${t.x}/${t.y}.png")
                }.addQueryParameter("key", key).build()
                val bytes = get(http, url) ?: if (layer == "map") throw IOException("TomTom's map didn't load") else return@forEach
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { canvas.drawBitmap(it, t.left.toFloat(), t.top.toFloat(), paint) }
            }
        }
        // You are here: a blue dot with a white ring, in the middle.
        val dot = Paint(Paint.ANTI_ALIAS_FLAG)
        dot.color = 0x40000000; canvas.drawCircle(WIDTH / 2f, HEIGHT / 2f + 2, 21f, dot)
        dot.color = 0xFFFFFFFF.toInt(); canvas.drawCircle(WIDTH / 2f, HEIGHT / 2f, 20f, dot)
        dot.color = 0xFF4285F4.toInt(); canvas.drawCircle(WIDTH / 2f, HEIGHT / 2f, 15f, dot)
        // TomTom's attribution, as its terms ask.
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB3000000.toInt(); textSize = 18f }.let {
            canvas.drawText("© TomTom", 12f, HEIGHT - 12f, it)
        }
        val file = file(context)
        val temp = File(file.parentFile, "$NAME.tmp")
        temp.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        temp.renameTo(file)
        return mapOf("map.traffic" to System.currentTimeMillis().toString())
    }

    private fun get(http: OkHttpClient, url: HttpUrl): ByteArray? {
        val request = Request.Builder().url(url).header("User-Agent", "AIWidgetMaker/0.2 (Android)").build()
        http.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) throw IOException("The TomTom key wasn't accepted for maps")
            if (!response.isSuccessful) return null
            return response.body?.bytes()
        }
    }

    /**
     * A link to the map the home screen can read, or null when there's none yet. Each refresh gets a link of
     * its own ([version]), so the launcher doesn't show an old picture it kept.
     */
    fun uri(context: Context, version: String): Uri? {
        val file = file(context)
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            .buildUpon().appendQueryParameter("v", version).build()
        // Home screens read it with this app's leave.
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY).forEach {
            context.grantUriPermission(it.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return uri
    }
}
