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
    /**
     * Samsung One UI's look: the wallpaper behind the widget, blurred and
     * frosted, big rounded corners, text that's dark or light to suit what's
     * behind it, and accents in the phone's color palette.
     */
    oneui("One UI"),
    /**
     * Music widgets only: like One UI, but frosted from the album art of
     * what's playing, so the widget takes on the song's colors (the
     * wallpaper, as One UI, when nothing is playing).
     */
    ambient("Ambient"),
    ;

    /** Frosted from a picture (wallpaper or album art), with text that suits it. */
    val frosted: Boolean get() = this == oneui || this == ambient

    companion object {
        /** The styles for every widget (Ambient is for music widgets only). */
        val everywhere: List<WidgetTheme> get() = entries.filter { it != ambient }

        /** Whether a design shows music, so it can use the Ambient style. */
        fun playsMusic(design: WidgetDesign): Boolean {
            val json = DesignParser.encode(design)
            return json.contains("{music.") || json.contains("\"type\":\"art\"")
        }
    }
}

/** What a color is used for, so a theme knows what to replace it with. */
enum class Role {
    Background, Container, Text, Accent, Track, TrendUp, TrendDown,
    /** A color that means something (Charge red/yellow/green, low battery): kept in every theme. */
    Level,
}

/**
 * The Material 3 colors a widget uses. On the home screen each slot is a color
 * resource that the launcher looks up itself, so it always matches the phone's
 * current palette (including Samsung One UI's) and light or dark mode.
 */
enum class MaterialSlot { Surface, Container, Text, Text80, Text65, Primary, Track, Up, Down }

/** Material 3's baseline palette: phones before Android 12, and tests. Mirrors res/color and res/color-night. */
object Baseline {
    fun color(slot: MaterialSlot, dark: Boolean): Int = when (slot) {
        MaterialSlot.Surface -> if (dark) 0xFF141218 else 0xFFFEF7FF
        MaterialSlot.Container -> if (dark) 0xFF4A4458 else 0xFFE8DEF8
        MaterialSlot.Text -> if (dark) 0xFFE6E0E9 else 0xFF1D1B20
        MaterialSlot.Text80 -> if (dark) 0xCCE6E0E9 else 0xCC1D1B20
        MaterialSlot.Text65 -> if (dark) 0xA6E6E0E9 else 0xA61D1B20
        MaterialSlot.Primary -> if (dark) 0xFFD0BCFF else 0xFF6750A4
        MaterialSlot.Track -> if (dark) 0x3DD0BCFF else 0x3D6750A4
        MaterialSlot.Up -> if (dark) 0xFF6EE7A0 else 0xFF1B873F
        MaterialSlot.Down -> if (dark) 0xFFFF8A80 else 0xFFC62828
    }.toInt()
}

/**
 * Works out each color under the chosen theme. Pure, so the widget and the
 * preview agree. [material] gives a Material 3 slot's color (the phone's, in
 * the app; the baseline in tests).
 */
class Styler(
    val theme: WidgetTheme,
    private val material: (MaterialSlot, Boolean) -> Int = Baseline::color,
    /** One UI: whether the frosted wallpaper behind this widget is light (so text is dark). */
    val backdropLight: Boolean = false,
) {

    /** Whether light and dark mode can give different colors (only Material 3 follows the phone). */
    val followsDarkMode get() = theme == WidgetTheme.material

    /** Which Material 3 slot a design color becomes. Text keeps roughly the design's fade. */
    fun materialSlot(role: Role, design: String?): MaterialSlot = when (role) {
        Role.Background -> MaterialSlot.Surface
        Role.Container -> MaterialSlot.Container
        Role.Text -> when (alphaOf(Colors.argb(design, fallback = 0xFFFFFFFF.toInt()))) {
            in 0xE6..0xFF -> MaterialSlot.Text
            in 0xBF..0xE5 -> MaterialSlot.Text80
            else -> MaterialSlot.Text65
        }
        Role.Accent, Role.Level -> MaterialSlot.Primary
        Role.Track -> MaterialSlot.Track
        Role.TrendUp -> MaterialSlot.Up
        Role.TrendDown -> MaterialSlot.Down
    }

    /** The color to draw for a design color with this [role], as ARGB. */
    fun argb(role: Role, design: String?, dark: Boolean): Int {
        val original = Colors.argb(design, fallback = 0)
        if (role == Role.Level) return original
        return when (theme) {
            WidgetTheme.original -> original
            WidgetTheme.glass -> when (role) {
                Role.Background -> GLASS_TINT
                Role.Container -> 0x1FFFFFFF
                // White, keeping the design's opacity so faded text stays faded.
                Role.Text -> withAlpha(0xFFFFFF, alphaOf(original).coerceAtLeast(0x99))
                Role.Track -> 0x2EFFFFFF
                Role.Accent, Role.TrendUp, Role.TrendDown, Role.Level -> original
            }
            WidgetTheme.material -> material(materialSlot(role, design), dark)
            WidgetTheme.oneui, WidgetTheme.ambient -> when (role) {
                Role.Background -> if (backdropLight) 0x8CF4F4F6.toInt() else 0x73202024
                Role.Container -> if (backdropLight) 0x47FFFFFF else 0x24FFFFFF
                // Text suits what's behind it, keeping the design's fade.
                Role.Text -> withAlpha(if (backdropLight) 0x111114 else 0xFFFFFF, alphaOf(original).coerceAtLeast(0xA6))
                // Accents in the phone's palette (One UI's color palette): darker over light, lighter over dark.
                Role.Accent -> material(MaterialSlot.Primary, !backdropLight)
                Role.Track -> withAlpha(material(MaterialSlot.Primary, !backdropLight), 0x40)
                Role.TrendUp -> material(MaterialSlot.Up, !backdropLight)
                Role.TrendDown -> material(MaterialSlot.Down, !backdropLight)
                Role.Level -> original
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
