package com.sidequest.widgetmaker.widget

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import com.sidequest.widgetmaker.spec.Backdrop
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The One UI theme's frosted background. Launchers don't let widgets blur
 * what's behind them, so the app does it itself: it takes the part of your
 * wallpaper behind the widget, blurs and frosts it, and draws it as the
 * widget's background, with One UI's big rounded corners and soft edge.
 *
 * Where the wallpaper comes from, best first:
 * 1. the picture you picked in Settings (no permission needed),
 * 2. the wallpaper itself, if you allowed wallpaper access,
 * 3. otherwise a frosted gradient in your wallpaper's colors.
 */
object OneUiBackdrop {
    private const val PICKED = "oneui_wallpaper.jpg"

    /** The finished background, and whether it's light (dark text) or dark (light text). */
    class Result(val bitmap: Bitmap, val light: Boolean)

    enum class Source(val description: String) {
        Picked("Blurring the wallpaper picture you picked"),
        System("Blurring your wallpaper"),
        Colors("Using your wallpaper's colors (pick your wallpaper picture for real blur)"),
    }

    @Volatile private var cached: Pair<Long, Bitmap?>? = null

    /**
     * The wallpaper itself comes first when Android lets the app read it: it's exactly what the home
     * screen shows, zoomed and placed as you set it, and it follows when you change it. A picked
     * picture is the original, which lines up only if the wallpaper wasn't zoomed or moved.
     */
    fun source(context: Context): Source = when {
        systemWallpaper(context) != null -> Source.System
        File(context.filesDir, PICKED).exists() -> Source.Picked
        else -> Source.Colors
    }

    /** Saves a copy of the picked picture, scaled to about the screen's size. */
    fun savePicked(context: Context, uri: Uri): Boolean = runCatching {
        val (sw, sh) = screenSize(context)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= sw && bounds.outHeight / (sample * 2) >= sh) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return false
        File(context.filesDir, PICKED).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        cached = null
        true
    }.getOrDefault(false)

    fun forgetPicked(context: Context) {
        File(context.filesDir, PICKED).delete()
        cached = null
    }

    /** The wallpaper picture to blur, or null to use the wallpaper's colors. */
    private fun wallpaper(context: Context): Bitmap? {
        val now = System.currentTimeMillis()
        // Reused for a few minutes, unless the wallpaper has been changed since.
        val id = wallpaperId(context)
        cached?.let { (at, bitmap) -> if (now - at < 5 * 60_000 && id == cachedId) return bitmap }
        val picked = File(context.filesDir, PICKED)
        val bitmap = systemWallpaper(context) ?: if (picked.exists()) BitmapFactory.decodeFile(picked.path) else null
        cached = now to bitmap
        cachedId = id
        return bitmap
    }

    @Volatile private var cachedId: Int = -1

    /** Which wallpaper is set: it changes when you set a new one. -1 when Android won't say. */
    private fun wallpaperId(context: Context): Int =
        runCatching { WallpaperManager.getInstance(context).getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrDefault(-1)

    /**
     * The wallpaper as the screen shows it (cropped to the screen's shape, the way the blur reads it),
     * at most [maxWidth] pixels wide; null when it can't be read.
     */
    fun screenPicture(context: Context, maxWidth: Int = 720): Bitmap? {
        val wall = wallpaper(context) ?: return null
        val (sw, sh) = screenSize(context)
        val area = Backdrop.wallpaperArea(wall.width, wall.height, sw, sh, Backdrop.Box(0, 0, sw, sh))
        val crop = Bitmap.createBitmap(wall, area.left, area.top, area.width, area.height)
        val w = min(maxWidth, crop.width)
        return Bitmap.createScaledBitmap(crop, w, max(1, (crop.height.toLong() * w / crop.width).toInt()), true)
    }

    /** Forgets the wallpaper read last, so the next drawing reads it again (after access is allowed). */
    fun forgetCached() {
        cached = null
    }

    /** The home screen wallpaper, when Android lets the app read it (wallpaper access allowed). */
    private fun systemWallpaper(context: Context): Bitmap? = runCatching {
        (WallpaperManager.getInstance(context).drawable as? BitmapDrawable)?.bitmap
    }.getOrNull()

    /**
     * The frosted wallpaper behind a panel of [widthPx]×[heightPx] at
     * [onScreen] (its place on the home screen, once known; otherwise the
     * middle is assumed), [outW]×[outH] pixels, not yet cut to shape.
     */
    fun render(context: Context, widthPx: Int, heightPx: Int, onScreen: Backdrop.Box?, outW: Int, outH: Int): Result {
        val (sw, sh) = screenSize(context)
        val w = max(8, widthPx)
        val h = max(8, heightPx)
        val place = onScreen ?: Backdrop.guess(sw, sh, w, h)

        // 1. A small copy of what's behind the widget (small = fast to blur, and blurs smoothly when scaled up).
        val smallW = max(8, w / 10)
        val smallH = max(8, h / 10)
        val small = wallpaper(context)?.let { wall ->
            val area = Backdrop.wallpaperArea(wall.width, wall.height, sw, sh, place)
            val crop = Bitmap.createBitmap(wall, area.left, area.top, area.width, area.height)
            Bitmap.createScaledBitmap(crop, smallW, smallH, true)
        } ?: colorsBitmap(context, smallW, smallH, place, sw, sh)

        return frost(small, outW, outH)
    }

    /**
     * Ambient (music widgets): the album art, cut to the panel's shape like a
     * cover filling it, blurred and frosted the same way.
     */
    fun renderArt(art: Bitmap, widthPx: Int, heightPx: Int, outW: Int, outH: Int): Result {
        val w = max(8, widthPx)
        val h = max(8, heightPx)
        // Crop the middle of the art to the panel's shape, then shrink it (small blurs smoothly).
        val scale = max(w.toFloat() / art.width, h.toFloat() / art.height)
        val cropW = (w / scale).roundToInt().coerceIn(1, art.width)
        val cropH = (h / scale).roundToInt().coerceIn(1, art.height)
        val crop = Bitmap.createBitmap(art, (art.width - cropW) / 2, (art.height - cropH) / 2, cropW, cropH)
        val small = Bitmap.createScaledBitmap(crop, max(8, w / 10), max(8, h / 10), true)
        return frost(small, outW, outH, saturation = 1.35)
    }

    /** Blurs [small], sees how bright it is, frosts it like One UI, and scales it up with a soft highlight. */
    private fun frost(small: Bitmap, outW: Int, outH: Int, saturation: Double = 1.2): Result {
        val smallW = small.width
        val smallH = small.height
        // 2. Blur it, see how bright it is, and frost it like One UI does.
        val pixels = IntArray(smallW * smallH)
        small.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)
        Backdrop.blur(pixels, smallW, smallH, radius = max(2, min(smallW, smallH) / 8))
        Backdrop.saturate(pixels, saturation)
        val light = Backdrop.luminance(pixels) > Backdrop.LIGHT_ABOVE
        // A light veil, so the wallpaper's colors still show through (One UI's blur is see-through, not gray).
        if (light) Backdrop.tint(pixels, 0xF6F6F8, 96) else Backdrop.tint(pixels, 0x1A1A1E, 64)
        val frosted = Bitmap.createBitmap(pixels, smallW, smallH, Bitmap.Config.ARGB_8888)

        // 3. Scale up smoothly, with a faint top highlight like light catching glass.
        val out = Bitmap.createScaledBitmap(frosted, max(8, outW), max(8, outH), true).copy(Bitmap.Config.ARGB_8888, true)
        Canvas(out).drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, out.height * 0.45f, 0x1FFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        return Result(out, light)
    }

    /** A soft gradient in the wallpaper's own colors, for when the picture itself can't be read. */
    private fun colorsBitmap(context: Context, w: Int, h: Int, place: Backdrop.Box, sw: Int, sh: Int): Bitmap {
        val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching { WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
        } else null
        val a = colors?.primaryColor?.toArgb() ?: 0xFF3A4A6B.toInt()
        val b = colors?.secondaryColor?.toArgb() ?: a
        val c = colors?.tertiaryColor?.toArgb() ?: b
        // Follow the gradient across the whole screen, so widgets in different places differ like real blur would.
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val sx = w.toFloat() / max(1, place.width)
        val sy = h.toFloat() / max(1, place.height)
        Canvas(bitmap).drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(
                -place.left * sx, -place.top * sy, (sw - place.left) * sx, (sh - place.top) * sy,
                intArrayOf(a or 0xFF000000.toInt(), b or 0xFF000000.toInt(), c or 0xFF000000.toInt()), null, Shader.TileMode.CLAMP,
            )
        })
        return bitmap
    }

    /** Which screen this is, like "1080x2400" (a foldable's cover and inner screens differ). */
    fun screenKey(context: Context): String = screenSize(context).let { (w, h) -> "${w}x$h" }

    fun screenSize(context: Context): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        val m = context.resources.displayMetrics
        return m.widthPixels to m.heightPixels
    }
}
