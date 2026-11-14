package com.sidequest.widgetmaker.widget

import android.content.Context
import android.graphics.Bitmap
import com.sidequest.widgetmaker.spec.Backdrop
import com.sidequest.widgetmaker.spec.Look
import com.sidequest.widgetmaker.spec.Panel
import com.sidequest.widgetmaker.spec.Styler
import com.sidequest.widgetmaker.spec.WidgetTheme
import kotlin.math.roundToInt

/**
 * A panel's background in its theme and shape, the same for the home screen
 * widget and the in-app preview.
 */
class PanelArt(
    /** The fill: a white silhouette to tint with the theme's background color when [tinted], else the finished image. */
    val fill: Bitmap,
    val tinted: Boolean,
    /** The soft edge drawn over the fill, if any. */
    val edge: Bitmap?,
    /** Colors for the content (for One UI, chosen to suit the frosted wallpaper behind it). */
    val styler: Styler,
) {
    companion object {
        /** A gradient from [top] to [bottom], over [under] (the frosted wallpaper) when there is one. */
        private fun gradientFill(top: Int, bottom: Int, w: Int, h: Int, under: Bitmap?): Bitmap {
            val out = under?.copy(Bitmap.Config.ARGB_8888, true) ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val alpha = if (under != null) 0xE0 else 0xFF
            fun a(c: Int) = (alpha shl 24) or (c and 0xFFFFFF)
            android.graphics.Canvas(out).drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), android.graphics.Paint().apply {
                shader = android.graphics.LinearGradient(0f, 0f, 0f, out.height.toFloat(), a(top), a(bottom), android.graphics.Shader.TileMode.CLAMP)
            })
            return out
        }

        private fun solid(color: Int, w: Int, h: Int): Bitmap =
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

        /** A lit edge reads brighter at the top than a plain one, so its color starts stronger. */
        private fun brighter(color: Int): Int = (minOf(255, (color ushr 24) * 2) shl 24) or (color and 0xFFFFFF)

        /**
         * [mood]: a weather widget's color for the weather now (see WeatherMood), laid over the blur or
         * in place of the plain fill, with light text on it; null keeps the theme's own look.
         */
        fun build(
            context: Context,
            theme: WidgetTheme,
            panel: Panel,
            look: Look,
            onScreen: Backdrop.Box?,
            mood: com.sidequest.widgetmaker.spec.WeatherMood? = null,
            /** The live values (Time of day reads today's sunrise and sunset from them). */
            feeds: Map<String, String> = emptyMap(),
        ): PanelArt {
            val density = context.resources.displayMetrics.density
            val pxW = (panel.width * density).roundToInt()
            val pxH = (panel.height * density).roundToInt()
            val (outW, outH) = ShapePainter.outSize(pxW, pxH)
            val scale = outW.toFloat() / pxW
            val radius = panel.radius * density * scale
            val edgePx = look.edge * density * scale
            // The glass's tone: like the phone's dark mode unless Settings says otherwise (null: by the wallpaper).
            val phoneDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            val tone = look.glassTone.light(phoneDark)
            /** The edge over the fill: the premium finish (sheen and a lit edge), or a plain even edge. */
            fun edge(color: Int, sheen: Int): Bitmap? =
                if (look.premium) ShapePainter.finish(panel.shape, outW, outH, radius, maxOf(edgePx, density * scale), brighter(color), sheen)
                else ShapePainter.edge(panel.shape, outW, outH, radius, edgePx, color)
            if (mood != null && WidgetTheme.takesMoods(theme)) {
                // Over the frosted wallpaper it's nearly solid, so a hint of the blur still shows through.
                val under = if (theme.frosted) OneUiBackdrop.render(context, pxW, pxH, onScreen, outW, outH, tone).bitmap else null
                return PanelArt(
                    ShapePainter.clip(gradientFill(mood.top, mood.bottom, outW, outH, under), panel.shape, radius), false,
                    edge(0x40FFFFFF, sheen = 0x14),
                    AiWidget.styler(context, if (theme == WidgetTheme.original) WidgetTheme.glass else theme, backdropLight = false),
                )
            }
            return when (theme) {
                WidgetTheme.glass -> PanelArt(
                    ShapePainter.glass(panel.shape, outW, outH, radius), false,
                    edge(0x4DFFFFFF, sheen = 0x1A),
                    AiWidget.styler(context, theme),
                )
                WidgetTheme.liquid -> {
                    // Clear glass: the wallpaper barely blurred, with a bright rim and corner glints.
                    val frost = OneUiBackdrop.render(context, pxW, pxH, onScreen, outW, outH, tone, clear = true)
                    PanelArt(
                        ShapePainter.clip(frost.bitmap, panel.shape, radius), false,
                        ShapePainter.liquidRim(panel.shape, outW, outH, radius, maxOf(edgePx, density * scale) * 1.4f),
                        AiWidget.styler(context, theme, backdropLight = frost.light),
                    )
                }
                WidgetTheme.tonal -> PanelArt(
                    // Solid, so it's drawn in its colors now (it redraws when the phone switches dark mode).
                    ShapePainter.clip(solid(AiWidget.styler(context, theme, backdropLight = !phoneDark).argb(com.sidequest.widgetmaker.spec.Role.Background, null, false), outW, outH), panel.shape, radius), false,
                    edge(0x14FFFFFF, sheen = 0x0A),
                    AiWidget.styler(context, theme, backdropLight = !phoneDark),
                )
                WidgetTheme.timeOfDay -> {
                    val sky = com.sidequest.widgetmaker.spec.Sky.of(feeds, System.currentTimeMillis())
                    PanelArt(
                        ShapePainter.clip(gradientFill(sky.top, sky.bottom, outW, outH, null), panel.shape, radius), false,
                        edge(0x33FFFFFF, sheen = 0x14),
                        AiWidget.styler(context, theme),
                    )
                }
                WidgetTheme.outline -> {
                    // No panel: just a thin outline. The text is light or dark to suit the wallpaper behind it.
                    val light = tone ?: OneUiBackdrop.render(context, pxW, pxH, onScreen, 8, 8).light
                    PanelArt(
                        Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888), false,
                        ShapePainter.edge(panel.shape, outW, outH, radius, maxOf(edgePx, density * scale * 1.5f), if (light) 0x8C111114.toInt() else 0x99FFFFFF.toInt()),
                        AiWidget.styler(context, theme, backdropLight = light),
                    )
                }
                WidgetTheme.oneui, WidgetTheme.ambient -> {
                    // Ambient: frosted album art while something plays; otherwise the wallpaper, as One UI.
                    val cover = if (theme == WidgetTheme.ambient) com.sidequest.widgetmaker.music.Music.art(context) else null
                    val frost = cover?.let { OneUiBackdrop.renderArt(it, pxW, pxH, outW, outH, tone) }
                        ?: OneUiBackdrop.render(context, pxW, pxH, onScreen, outW, outH, tone)
                    PanelArt(
                        // One UI's own widgets have no visible rim, so the edge is only a whisper.
                        ShapePainter.clip(frost.bitmap, panel.shape, radius), false,
                        edge(if (frost.light) 0x2EFFFFFF else 0x1AFFFFFF, sheen = 0x08),
                        AiWidget.styler(context, theme, backdropLight = frost.light),
                    )
                }
                WidgetTheme.material, WidgetTheme.original -> PanelArt(
                    ShapePainter.mask(panel.shape, outW, outH, radius), true,
                    edge(0x26FFFFFF, sheen = 0x14),
                    AiWidget.styler(context, theme),
                )
            }
        }
    }
}
