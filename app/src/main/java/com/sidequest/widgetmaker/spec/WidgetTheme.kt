package com.sidequest.widgetmaker.spec

/**
 * How every widget is colored, chosen in Settings. The layout always comes
 * from the design; a theme only restyles its colors.
 */
enum class WidgetTheme(val label: String) {
    /**
     * Material 3 Expressive with One UI's order: solid panels pooled from one of the phone's colors, with a second
     * (pop) color for what matters. Hero shapes (cookie, flower, circle...) are bold; cards are soft. Light or dark
     * with the phone.
     */
    expressive("Expressive"),
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
    /** Clear glass: the wallpaper behind, only lightly blurred, with a bright rim and glints at the corners. */
    liquid("Liquid glass"),
    /** Material You's tonal look: solid panels in a rich tone of the phone's colors, light or dark with the phone. */
    tonal("Tonal"),
    /** The panel takes the sky's color through the day: deep blue at night, warm at dawn and dusk, blue by day. */
    timeOfDay("Time of day"),
    /** No panel: the content on the wallpaper itself, inside a thin outline. */
    outline("Outline"),
    /**
     * Soft studio cards: off-white (graphite in dark mode) with gentle depth, near-black text and a single accent
     * from the phone's color palette for what matters, like a well-made watch face.
     */
    studio("Studio"),
    ;

    /** Frosted from a picture (wallpaper or album art), with text that suits it. */
    val frosted: Boolean get() = this == oneui || this == ambient || this == liquid

    /**
     * Expressive draws every design its way, whoever made it: progress bars as thick rounded pills (at least
     * [EXPRESSIVE_BAR] dp) and no divider lines (space alone groups things, as in its cards).
     */
    fun barHeight(height: Int) = if (this == expressive) maxOf(height, EXPRESSIVE_BAR) else height
    val drawsDividers: Boolean get() = this != expressive

    /** Seen through to the wallpaper, so it needs to know where the widget is. */
    val showsWallpaper: Boolean get() = frosted || this == outline

    companion object {
        /** The thinnest progress bar Expressive draws, in dp. */
        const val EXPRESSIVE_BAR = 8

        /**
         * The styles for every widget (Ambient is for music widgets only). Material 3 is retired: Expressive, Google's
         * newer design language built on the same Material You colors, takes its place.
         */
        val everywhere: List<WidgetTheme> get() = entries.filter { it != ambient && it != material }

        /** The styles a music widget can have: every one, and Ambient. */
        val forMusic: List<WidgetTheme> get() = everywhere + ambient

        /** A saved style as it's drawn now: Material 3, retired, is Expressive. */
        fun current(theme: WidgetTheme) = if (theme == material) expressive else theme

        /**
         * Whether a theme takes on the weather's color for weather widgets (Settings → Weather moods). Not the
         * ones whose point is their own look: Material 3 and Tonal's colors, Outline's no panel, Liquid glass's clear glass.
         */
        fun takesMoods(theme: WidgetTheme) = theme != material && theme != tonal && theme != outline && theme != liquid && theme != studio && theme != expressive

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
    /** A [Tone.strong] element (a filled tile, chip or bar), and text sitting on one. */
    Strong, OnStrong,
    /** A [Tone.pop] element (what's happening now), and text sitting on one. */
    Pop, OnPop,
    ;

    companion object {
        /** The role an element with [tone] takes instead of its own (a level color stays as it is). */
        fun toned(role: Role, tone: com.sidequest.widgetmaker.spec.Tone?): Role = when {
            tone == null || role == Level -> role
            tone == com.sidequest.widgetmaker.spec.Tone.strong -> Strong
            else -> Pop
        }

        /** Text on a tile of [tone]. */
        fun on(tone: com.sidequest.widgetmaker.spec.Tone) = if (tone == com.sidequest.widgetmaker.spec.Tone.strong) OnStrong else OnPop
    }
}

/**
 * The Material 3 colors a widget uses. On the home screen each slot is a color
 * resource that the launcher looks up itself, so it always matches the phone's
 * current palette (including Samsung One UI's) and light or dark mode.
 */
enum class MaterialSlot { Surface, Container, Text, Text80, Text65, Primary, Track, Up, Down, Strong, OnStrong, Pop, OnPop }

/**
 * The Expressive style's palette: which tone of the phone's main (1) or pop (3) color each slot takes, as Material
 * You's tone numbers (Android's system_accentN_<tone> colors: 50 is near white, 900 near black). Bold hero shapes
 * sit on a strong tone with light text (dark text in dark mode, where the strong tone is light); soft cards on a
 * pale one (deep in dark mode). The pop color is always in a tone that stands off the panel.
 */
object ExpressivePalette {
    data class Tone(val accent: Int, val tone: Int)

    fun tone(slot: MaterialSlot, dark: Boolean, bold: Boolean): Tone? = when (slot) {
        MaterialSlot.Surface -> Tone(1, when { bold && !dark -> 600; bold -> 300; !dark -> 50; else -> 900 })
        MaterialSlot.Container -> Tone(1, when { bold && !dark -> 700; bold -> 200; !dark -> 100; else -> 800 })
        MaterialSlot.Text, MaterialSlot.Text80, MaterialSlot.Text65 -> Tone(1, if (bold != dark) 50 else 900)
        MaterialSlot.Primary -> Tone(3, when { bold && !dark -> 200; bold -> 700; !dark -> 600; else -> 200 })
        MaterialSlot.Track -> Tone(1, when { bold && !dark -> 400; bold -> 400; !dark -> 200; else -> 700 })
        MaterialSlot.Strong -> Tone(1, when { bold && !dark -> 800; bold -> 700; !dark -> 600; else -> 200 })
        MaterialSlot.OnStrong -> Tone(1, if (!bold && dark) 900 else 50)
        MaterialSlot.Pop -> Tone(3, if (bold && dark) 600 else 200)
        MaterialSlot.OnPop -> Tone(3, if (bold && dark) 50 else 900)
        MaterialSlot.Up, MaterialSlot.Down -> null
    }

    /** Material's baseline tones (phones before Android 12, and tests). */
    fun baseline(t: Tone): Int = (if (t.accent == 3) when (t.tone) {
        50 -> 0xFFFFECF1; 200 -> 0xFFEFB8C8; 600 -> 0xFF7D5260; 900 -> 0xFF31111D; else -> 0xFF633B48
    } else when (t.tone) {
        50 -> 0xFFF6EDFF; 100 -> 0xFFEADDFF; 200 -> 0xFFD0BCFF; 300 -> 0xFFB69DF8; 400 -> 0xFF9A82DB
        600 -> 0xFF6750A4; 700 -> 0xFF4F378B; 800 -> 0xFF381E72; else -> 0xFF21005D
    }).toInt()

    fun baseline(slot: MaterialSlot, dark: Boolean, bold: Boolean): Int = tone(slot, dark, bold)?.let(::baseline) ?: Baseline.color(slot, dark)
}

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
        MaterialSlot.Strong -> if (dark) 0xFFD0BCFF else 0xFF6750A4
        MaterialSlot.OnStrong -> if (dark) 0xFF381E72 else 0xFFFFFFFF
        MaterialSlot.Pop -> if (dark) 0xFF633B48 else 0xFFFFD8E4
        MaterialSlot.OnPop -> if (dark) 0xFFFFD8E4 else 0xFF31111D
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
    /** Drawing inside a tile of this tone: its text takes the color that reads on it. */
    val on: com.sidequest.widgetmaker.spec.Tone? = null,
    /** Material 3: a hero shape (cookie, flower...), drawn in the strong tone with text that reads on it. */
    val bold: Boolean = false,
) {
    /** This styler inside a tile of [tone] (itself when there's none). */
    fun inside(tone: com.sidequest.widgetmaker.spec.Tone?) = if (tone == null) this else Styler(theme, material, backdropLight, tone, bold)

    /** [role] as it applies here: text, icons and accents inside a toned tile read on that tile; a bold panel is strong. */
    private fun here(role: Role): Role {
        if (on != null && (role == Role.Text || role == Role.Accent)) return Role.on(on)
        if (bold && theme == WidgetTheme.material) return when (role) {
            Role.Background -> Role.Strong
            Role.Text -> Role.OnStrong
            else -> role
        }
        return role
    }


    /** Whether light and dark mode can give different colors (only Material 3 follows the phone). */
    val followsDarkMode get() = theme == WidgetTheme.material

    /** Which Material 3 slot a design color becomes. Text keeps roughly the design's fade. */
    fun materialSlot(role: Role, design: String?): MaterialSlot = when (here(role)) {
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
        Role.Strong -> MaterialSlot.Strong
        Role.OnStrong -> MaterialSlot.OnStrong
        Role.Pop -> MaterialSlot.Pop
        Role.OnPop -> MaterialSlot.OnPop
    }

    /** The color to draw for a design color with this [role], as ARGB. */
    fun argb(role: Role, design: String?, dark: Boolean): Int = argbHere(here(role), design, dark)

    private fun argbHere(role: Role, design: String?, dark: Boolean): Int {
        val original = Colors.argb(design, fallback = 0)
        if (role == Role.Level) return original
        // Tones: Expressive and Material 3 have their own; elsewhere a tone is the theme's accent, its text black
        // or white to suit.
        if (role == Role.Strong || role == Role.Pop || role == Role.OnStrong || role == Role.OnPop) {
            if (theme == WidgetTheme.material) return material(materialSlot(role, design), dark)
            if (theme == WidgetTheme.expressive) {
                val slot = when (role) {
                    Role.Strong -> MaterialSlot.Strong; Role.Pop -> MaterialSlot.Pop
                    Role.OnStrong -> MaterialSlot.OnStrong; else -> MaterialSlot.OnPop
                }
                val c = material(slot, !backdropLight)
                return if (role == Role.OnStrong || role == Role.OnPop) withAlpha(c, alphaOf(original).coerceAtLeast(0xA6)) else c
            }
            val fill = argbHere(Role.Accent, design, dark)
            return when (role) {
                Role.Strong, Role.Pop -> fill
                else -> withAlpha(contrast(fill), alphaOf(original).coerceAtLeast(0xA6))
            }
        }
        return when (theme) {
            WidgetTheme.original -> original
            WidgetTheme.glass -> when (role) {
                Role.Strong, Role.OnStrong, Role.Pop, Role.OnPop -> original // Tones are worked out above.
                Role.Background -> GLASS_TINT
                Role.Container -> 0x1FFFFFFF
                // White, keeping the design's opacity so faded text stays faded.
                Role.Text -> withAlpha(0xFFFFFF, alphaOf(original).coerceAtLeast(0x99))
                Role.Track -> 0x2EFFFFFF
                Role.Accent, Role.TrendUp, Role.TrendDown, Role.Level -> original
            }
            WidgetTheme.material -> material(materialSlot(role, design), dark)
            // Expressive: [backdropLight] is the phone's light mode. The slots come from its pooled palette (bold or
            // soft for the panel's shape), the accent being the pop color.
            WidgetTheme.expressive -> {
                val dark = !backdropLight
                when (role) {
                    Role.Strong, Role.OnStrong, Role.Pop, Role.OnPop -> original // Tones are worked out above.
                    Role.Background -> material(MaterialSlot.Surface, dark)
                    Role.Container -> material(MaterialSlot.Container, dark)
                    Role.Text -> withAlpha(material(MaterialSlot.Text, dark), alphaOf(original).coerceAtLeast(0xA6))
                    Role.Accent -> material(MaterialSlot.Primary, dark)
                    Role.Track -> material(MaterialSlot.Track, dark)
                    Role.TrendUp -> material(MaterialSlot.Up, dark)
                    Role.TrendDown -> material(MaterialSlot.Down, dark)
                    Role.Level -> original
                }
            }
            WidgetTheme.tonal -> {
                // [backdropLight] here is the phone's light mode; the slots come from the tonal palette.
                val dark = !backdropLight
                when (role) {
                    Role.Strong, Role.OnStrong, Role.Pop, Role.OnPop -> original // Tones are worked out above.
                    Role.Background -> material(MaterialSlot.Surface, dark)
                    Role.Container -> material(MaterialSlot.Container, dark)
                    Role.Text -> withAlpha(material(MaterialSlot.Text, dark), alphaOf(original).coerceAtLeast(0xA6))
                    Role.Accent -> material(MaterialSlot.Primary, dark)
                    Role.Track -> withAlpha(material(MaterialSlot.Primary, dark), 0x40)
                    Role.TrendUp -> material(MaterialSlot.Up, dark)
                    Role.TrendDown -> material(MaterialSlot.Down, dark)
                    Role.Level -> original
                }
            }
            // Studio: [backdropLight] is the phone's light mode. One accent, from the phone's own palette;
            // everything else ink or paper.
            WidgetTheme.studio -> {
                val light = backdropLight
                val ink = if (light) 0x15181B else 0xF2F4F5
                val accent = material(MaterialSlot.Primary, !light)
                when (role) {
                    Role.Strong, Role.OnStrong, Role.Pop, Role.OnPop -> original // Tones are worked out above.
                    Role.Background -> if (light) 0xFFF2F4F5.toInt() else 0xFF1D2023.toInt()
                    Role.Container -> withAlpha(accent, if (light) 0x26 else 0x33)
                    Role.Text -> withAlpha(ink, alphaOf(original).coerceAtLeast(0x8C))
                    Role.Accent -> accent
                    Role.Track -> withAlpha(ink, if (light) 0x1F else 0x29)
                    Role.TrendUp -> if (light) 0xFF1B873F.toInt() else 0xFF6EE7A0.toInt()
                    Role.TrendDown -> if (light) 0xFFD32F2F.toInt() else 0xFFFF8A80.toInt()
                    Role.Level -> original
                }
            }
            // Over the sky, or straight on the wallpaper: light text (dark over a light wallpaper, for Outline).
            WidgetTheme.timeOfDay, WidgetTheme.outline -> {
                val ink = if (theme == WidgetTheme.outline && backdropLight) 0x111114 else 0xFFFFFF
                when (role) {
                    Role.Strong, Role.OnStrong, Role.Pop, Role.OnPop -> original // Tones are worked out above.
                    Role.Background -> if (theme == WidgetTheme.outline) 0 else GLASS_TINT
                    Role.Container -> withAlpha(ink, 0x24)
                    Role.Text -> withAlpha(ink, alphaOf(original).coerceAtLeast(0xA6))
                    Role.Track -> withAlpha(ink, 0x33)
                    Role.Accent, Role.TrendUp, Role.TrendDown, Role.Level -> original
                }
            }
            WidgetTheme.oneui, WidgetTheme.ambient, WidgetTheme.liquid -> when (role) {
                Role.Strong, Role.OnStrong, Role.Pop, Role.OnPop -> original // Tones are worked out above.
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

        /** Near-black on a light [argb], white on a dark one: for text cut out of a block of that color. */
        fun contrast(argb: Int): Int {
            val r = (argb shr 16) and 0xFF; val g = (argb shr 8) and 0xFF; val b = argb and 0xFF
            return if (0.299 * r + 0.587 * g + 0.114 * b > 150) 0xFF111114.toInt() else 0xFFFFFFFF.toInt()
        }
        fun withAlpha(rgb: Int, alpha: Int) = (alpha shl 24) or (rgb and 0xFFFFFF)
    }
}
