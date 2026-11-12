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
        fun build(context: Context, theme: WidgetTheme, panel: Panel, look: Look, onScreen: Backdrop.Box?): PanelArt {
            val density = context.resources.displayMetrics.density
            val pxW = (panel.width * density).roundToInt()
            val pxH = (panel.height * density).roundToInt()
            val (outW, outH) = ShapePainter.outSize(pxW, pxH)
            val scale = outW.toFloat() / pxW
            val radius = panel.radius * density * scale
            val edgePx = look.edge * density * scale
            return when (theme) {
                WidgetTheme.glass -> PanelArt(
                    ShapePainter.glass(panel.shape, outW, outH, radius), false,
                    ShapePainter.edge(panel.shape, outW, outH, radius, edgePx, 0x4DFFFFFF),
                    AiWidget.styler(context, theme),
                )
                WidgetTheme.oneui -> {
                    val frost = OneUiBackdrop.render(context, pxW, pxH, onScreen, outW, outH)
                    PanelArt(
                        ShapePainter.clip(frost.bitmap, panel.shape, radius), false,
                        ShapePainter.edge(panel.shape, outW, outH, radius, edgePx, if (frost.light) 0x73FFFFFF else 0x33FFFFFF),
                        AiWidget.styler(context, theme, backdropLight = frost.light),
                    )
                }
                WidgetTheme.material, WidgetTheme.original -> PanelArt(
                    ShapePainter.mask(panel.shape, outW, outH, radius), true,
                    ShapePainter.edge(panel.shape, outW, outH, radius, edgePx, 0x26FFFFFF),
                    AiWidget.styler(context, theme),
                )
            }
        }
    }
}
