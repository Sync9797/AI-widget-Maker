package com.sidequest.widgetmaker.widget

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import com.sidequest.widgetmaker.spec.Fitter
import com.sidequest.widgetmaker.spec.Weight

/**
 * Measures text the way the home screen will draw it: in the phone's own font (the one widgets' text uses,
 * like Samsung's) at the phone's font size setting, so text is fitted to its real width.
 */
class PhoneText(context: Context) : Fitter.TextMeasure {
    val fontScale: Float = context.resources.configuration.fontScale

    private val paints = Weight.entries.associateWith { weight ->
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = when {
                weight == Weight.bold -> Typeface.DEFAULT_BOLD
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && weight == Weight.medium -> Typeface.create(Typeface.DEFAULT, 500, false)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && weight == Weight.light -> Typeface.create(Typeface.DEFAULT, 300, false)
                else -> Typeface.DEFAULT
            }
        }
    }

    override fun width(text: String, sizeSp: Float, weight: Weight): Float {
        val paint = paints.getValue(weight)
        // In dp: the size in sp times the font size setting, measured in the same units.
        paint.textSize = sizeSp * fontScale
        return paint.measureText(text)
    }
}
