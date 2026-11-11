package com.sidequest.widgetmaker.spec

/**
 * How every widget is colored, chosen in Settings. The layout always comes
 * from the design; a theme only restyles its colors.
 */
enum class WidgetTheme(val label: String) {
    /** Smoky translucent glass with a bright edge and white text. Accent colors stay as designed. */
    glass("Glass"),
    /** Material You colors from the wallpaper, light or dark with the phone. */
    material("Material 3"),
    /** The colors Claude chose for each design. */
    original("Original colors"),
}

/** What a color is used for, so a theme knows what to replace it with. */
enum class Role { Background, Container, Text, Accent, Track, TrendUp, TrendDown }

/** The Material 3 colors a widget uses, for light and dark mode. */
data class Material3Colors(
    val surfaceDay: Int,
    val surfaceNight: Int,
    val containerDay: Int,
    val containerNight: Int,
    val onSurfaceDay: Int,
    val onSurfaceNight: Int,
    val primaryDay: Int,
    val primaryNight: Int,
) {
    companion object {
        /** Material 3's baseline palette, for phones before Android 12 (no wallpaper colors). */
        val BASELINE = Material3Colors(
            surfaceDay = 0xFFFEF7FF.toInt(), surfaceNight = 0xFF141218.toInt(),
            containerDay = 0xFFE8DEF8.toInt(), containerNight = 0xFF4A4458.toInt(),
            onSurfaceDay = 0xFF1D1B20.toInt(), onSurfaceNight = 0xFFE6E0E9.toInt(),
            primaryDay = 0xFF6750A4.toInt(), primaryNight = 0xFFD0BCFF.toInt(),
        )
    }
}

/** Works out each color under the chosen theme. Pure, so the widget and the preview agree. */
class Styler(val theme: WidgetTheme, private val m3: Material3Colors = Material3Colors.BASELINE) {

    /** Whether light and dark mode can give different colors (only Material 3 follows the phone). */
    val followsDarkMode get() = theme == WidgetTheme.material

    /** The color to draw for a design color with this [role], as ARGB. */
    fun argb(role: Role, design: String?, dark: Boolean): Int {
        val original = Colors.argb(design, fallback = 0)
        return when (theme) {
            WidgetTheme.original -> original
            WidgetTheme.glass -> when (role) {
                Role.Background -> GLASS_TINT
                Role.Container -> 0x1FFFFFFF
                // White, keeping the design's opacity so faded text stays faded.
                Role.Text -> withAlpha(0xFFFFFF, alphaOf(original).coerceAtLeast(0x99))
                Role.Track -> 0x2EFFFFFF
                Role.Accent, Role.TrendUp, Role.TrendDown -> original
            }
            WidgetTheme.material -> when (role) {
                Role.Background -> if (dark) m3.surfaceNight else m3.surfaceDay
                Role.Container -> if (dark) m3.containerNight else m3.containerDay
                Role.Text -> withAlpha(if (dark) m3.onSurfaceNight else m3.onSurfaceDay, alphaOf(original).coerceAtLeast(0xAA))
                Role.Accent -> if (dark) m3.primaryNight else m3.primaryDay
                Role.Track -> withAlpha(if (dark) m3.primaryNight else m3.primaryDay, 0x3D)
                // Greens and reds that read well on light and dark surfaces.
                Role.TrendUp -> if (dark) 0xFF6EE7A0.toInt() else 0xFF1B873F.toInt()
                Role.TrendDown -> if (dark) 0xFFFF8A80.toInt() else 0xFFC62828.toInt()
            }
        }
    }

    companion object {
        /** Glass's base tint, under its highlight and edge (used where the glass drawable can't be). */
        const val GLASS_TINT = 0x66101826

        fun alphaOf(argb: Int) = (argb ushr 24) and 0xFF
        fun withAlpha(rgb: Int, alpha: Int) = (alpha shl 24) or (rgb and 0xFFFFFF)
    }
}
