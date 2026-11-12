package com.sidequest.widgetmaker.widget

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/** Album art cropped square with rounded corners (widgets can't clip images themselves on older Android). */
object ArtCorners {
    /** [radius] is a share of the side: 0.5 makes a circle. */
    fun round(source: Bitmap, radius: Float): Bitmap {
        val side = minOf(source.width, source.height)
        val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(android.graphics.Matrix().apply { setTranslate(-(source.width - side) / 2f, -(source.height - side) / 2f) })
        }
        val r = side * radius.coerceIn(0f, 0.5f)
        Canvas(out).drawRoundRect(RectF(0f, 0f, side.toFloat(), side.toFloat()), r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        return out
    }
}
