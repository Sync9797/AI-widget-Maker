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
        /** A lit edge reads brighter at the top than a plain one, so its color starts stronger. */
        private fun brighter(color: Int): Int = (minOf(255, (color ushr 24) * 2) shl 24) or (color and 0xFFFFFF)

        fun build(context: Context, theme: WidgetTheme, panel: Panel, look: Look, onScreen: Backdrop.Box?): PanelArt {
            val density = context.resources.displayMetrics.density
            val pxW = (panel.width * density).roundToInt()
            val pxH = (panel.height * density).roundToInt()
            val (outW, outH) = ShapePainter.outSize(pxW, pxH)
            val scale = outW.toFloat() / pxW
            val radius = panel.radius * density * scale
            val edgePx = look.edge * density * scale
            /** The edge over the fill: the premium finish (sheen and a lit edge), or a plain even edge. */
            fun edge(color: Int, sheen: Int): Bitmap? =
                if (look.premium) ShapePainter.finish(panel.shape, outW, outH, radius, maxOf(edgePx, density * scale), brighter(color), sheen)
                else ShapePainter.edge(panel.shape, outW, outH, radius, edgePx, color)
            return when (theme) {
                WidgetTheme.glass -> PanelArt(
                    ShapePainter.glass(panel.shape, outW, outH, radius), false,
                    edge(0x4DFFFFFF, sheen = 0x1A),
                    AiWidget.styler(context, theme),
                )
                WidgetTheme.oneui, WidgetTheme.ambient -> {
                    // Ambient: frosted album art while something plays; otherwise the wallpaper, as One UI.
                    val cover = if (theme == WidgetTheme.ambient) com.sidequest.widgetmaker.music.Music.art(context) else null
                    val frost = cover?.let { OneUiBackdrop.renderArt(it, pxW, pxH, outW, outH) }
                        ?: OneUiBackdrop.render(context, pxW, pxH, onScreen, outW, outH)
                    PanelArt(
                        ShapePainter.clip(frost.bitmap, panel.shape, radius), false,
                        edge(if (frost.light) 0x73FFFFFF else 0x33FFFFFF, sheen = 0x12),
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
