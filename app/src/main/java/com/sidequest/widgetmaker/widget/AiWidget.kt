package com.sidequest.widgetmaker.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import com.sidequest.widgetmaker.music.MusicControlReceiver
import com.sidequest.widgetmaker.spec.ArtNode
import com.sidequest.widgetmaker.spec.Colors
import com.sidequest.widgetmaker.spec.SleepNode
import com.sidequest.widgetmaker.spec.ChartNode
import com.sidequest.widgetmaker.spec.RingNode
import com.sidequest.widgetmaker.spec.TimerNode
import com.sidequest.widgetmaker.spec.levelColor
import com.sidequest.widgetmaker.spec.TapAction
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.compose.runtime.remember
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.width
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sidequest.widgetmaker.R
import com.sidequest.widgetmaker.data.DesignStore
import com.sidequest.widgetmaker.feeds.Feeds
import com.sidequest.widgetmaker.spec.ClockNode
import com.sidequest.widgetmaker.spec.ColumnNode
import com.sidequest.widgetmaker.spec.HAlign
import com.sidequest.widgetmaker.spec.IconName
import com.sidequest.widgetmaker.spec.IconNode
import com.sidequest.widgetmaker.spec.Node
import com.sidequest.widgetmaker.spec.ProgressNode
import com.sidequest.widgetmaker.spec.RowNode
import com.sidequest.widgetmaker.spec.SpacerNode
import com.sidequest.widgetmaker.spec.DividerNode
import com.sidequest.widgetmaker.spec.TextNode
import com.sidequest.widgetmaker.spec.Tokens
import com.sidequest.widgetmaker.spec.VAlign
import com.sidequest.widgetmaker.spec.MaterialSlot
import com.sidequest.widgetmaker.spec.Role
import com.sidequest.widgetmaker.spec.Styler
import com.sidequest.widgetmaker.spec.Weight
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.spec.visible
import com.sidequest.widgetmaker.spec.WidgetDesign
import com.sidequest.widgetmaker.ui.MainActivity
import com.sidequest.widgetmaker.ui.TapIntents
import com.sidequest.widgetmaker.spec.Taps
import com.sidequest.widgetmaker.spec.Backdrop
import com.sidequest.widgetmaker.spec.Baseline
import com.sidequest.widgetmaker.spec.Look
import com.sidequest.widgetmaker.spec.Shapes
import com.sidequest.widgetmaker.spec.WidgetShape
import androidx.glance.layout.ContentScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/** The widget: draws whichever saved design was picked for it. Tapping it opens the app. */
class AiWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            // Glance keeps a widget's drawing alive for a while and reuses it for
            // updates, so everything is (re)loaded here, whenever a refresh bumps
            // this widget's revision: a new shape, look, theme or data shows at once.
            val rev = currentState(REV) ?: 0L
            val inputs = remember(rev) { Inputs.load(context, appWidgetId) }
            val saved = inputs.saved
            val tokens = inputs.tokens
            if (saved == null) {
                EmptyWidget(GlanceModifier.clickable(actionStartActivity<MainActivity>()))
            } else {
                // The widget as a whole opens the app for what it shows (or this app, if nothing fits).
                // It goes through the tap screen, which also learns where the widget is (for the One UI blur).
                val whole = Taps.forWidget(saved.design)?.let { Taps.resolve(it, tokens) }
                val open = TapIntents.root(context, appWidgetId, whole)
                DesignWidget(saved.design, tokens, inputs.theme, inputs.look, inputs.shape, inputs.onScreen, open, inputs.art)
            }
        }
    }

    companion object {
        /** Every widget's revision, bumped by each refresh so it reloads everything. */
        val REV = longPreferencesKey("rev")

        /** Redraws every placed widget with fresh settings and data. */
        suspend fun refreshAll(context: Context) {
            GlanceAppWidgetManager(context).getGlanceIds(AiWidget::class.java).forEach { refresh(context, it) }
        }

        suspend fun refresh(context: Context, id: GlanceId) {
            updateAppWidgetState(context, id) { it[REV] = System.nanoTime() }
            AiWidget().update(context, id)
        }

        /**
         * The chosen theme. For Material 3 the app reads the phone's colors
         * from the same resources the widget hands to the launcher (used for
         * the in-app preview; the widget itself never resolves them).
         */
        fun styler(context: Context, theme: WidgetTheme, backdropLight: Boolean = false, bold: Boolean = false): Styler = when {
            // Expressive: [backdropLight] is the phone's light mode; [bold] for hero shapes.
            theme == WidgetTheme.expressive -> Styler(theme, { slot, dark -> expressiveTone(context, slot, dark, bold) }, backdropLight)
            theme.frosted -> Styler(theme, { slot, dark -> oneUiTone(context, slot, dark) }, backdropLight)
            // Tonal: [backdropLight] is the phone's light mode.
            theme == WidgetTheme.tonal -> Styler(theme, { slot, dark -> tonalTone(context, slot, dark) }, backdropLight)
            // Studio: [backdropLight] is the phone's light mode; its colors are its own.
            theme == WidgetTheme.studio -> Styler(theme, { slot, dark -> oneUiTone(context, slot, dark) }, backdropLight)
            theme == WidgetTheme.outline || theme == WidgetTheme.timeOfDay -> Styler(theme, { slot, dark -> Baseline.color(slot, dark) }, backdropLight)
            else -> Styler(theme, { slot, _ -> context.getColor(materialRes(slot)) }, bold = bold && theme == WidgetTheme.material)
        }

        /**
         * Material You's tonal palette: a rich tone of the phone's main color for the panel, a deeper one for
         * containers, and text in its darkest (or, in dark mode, lightest) tone. The baseline before Android 12.
         */
        private fun tonalTone(context: Context, slot: MaterialSlot, dark: Boolean): Int {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val res = when (slot) {
                    MaterialSlot.Surface -> if (dark) android.R.color.system_accent1_800 else android.R.color.system_accent1_100
                    MaterialSlot.Container -> if (dark) android.R.color.system_accent1_700 else android.R.color.system_accent1_200
                    MaterialSlot.Text, MaterialSlot.Text80, MaterialSlot.Text65 -> if (dark) android.R.color.system_accent1_50 else android.R.color.system_accent1_900
                    MaterialSlot.Primary, MaterialSlot.Track -> if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600
                    else -> null
                }
                if (res != null) return context.getColor(res)
            }
            return when (slot) {
                MaterialSlot.Surface -> if (dark) 0xFF4F378B else 0xFFEADDFF
                MaterialSlot.Container -> if (dark) 0xFF633B9B else 0xFFD6C6FB
                MaterialSlot.Text, MaterialSlot.Text80, MaterialSlot.Text65 -> if (dark) 0xFFEADDFF else 0xFF21005D
                else -> Baseline.color(slot, dark).toLong()
            }.toInt()
        }

        /** The Expressive style's colors: its tones of the phone's palette (Android 12+), else Material's baseline. */
        private fun expressiveTone(context: Context, slot: MaterialSlot, dark: Boolean, bold: Boolean): Int {
            val t = com.sidequest.widgetmaker.spec.ExpressivePalette.tone(slot, dark, bold) ?: return Baseline.color(slot, dark)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) systemTone(t.accent, t.tone)?.let { return context.getColor(it) }
            return com.sidequest.widgetmaker.spec.ExpressivePalette.baseline(t)
        }

        /** Android's system color for an accent (1 or 3) at a tone, or null for one it doesn't have. */
        @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
        private fun systemTone(accent: Int, tone: Int): Int? = when (accent) {
            1 -> when (tone) {
                50 -> android.R.color.system_accent1_50; 100 -> android.R.color.system_accent1_100; 200 -> android.R.color.system_accent1_200
                300 -> android.R.color.system_accent1_300; 400 -> android.R.color.system_accent1_400; 600 -> android.R.color.system_accent1_600
                700 -> android.R.color.system_accent1_700; 800 -> android.R.color.system_accent1_800; 900 -> android.R.color.system_accent1_900
                else -> null
            }
            3 -> when (tone) {
                50 -> android.R.color.system_accent3_50; 900 -> android.R.color.system_accent3_900
                200 -> android.R.color.system_accent3_200; 600 -> android.R.color.system_accent3_600; 700 -> android.R.color.system_accent3_700
                else -> null
            }
            else -> null
        }

        /**
         * One UI's accents: the phone's palette in a tone that reads over the
         * frosted panel (darker over light, lighter over dark), whatever the
         * phone's light or dark mode.
         */
        private fun oneUiTone(context: Context, slot: MaterialSlot, dark: Boolean): Int {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && slot == MaterialSlot.Primary) {
                return context.getColor(if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600)
            }
            return Baseline.color(slot, dark)
        }

        /** The color resource for a Material 3 slot (res/color*, per Android version and light or dark). */
        fun materialRes(slot: MaterialSlot): Int = when (slot) {
            MaterialSlot.Surface -> R.color.m3_surface
            MaterialSlot.Container -> R.color.m3_container
            MaterialSlot.Text -> R.color.m3_text
            MaterialSlot.Text80 -> R.color.m3_text_80
            MaterialSlot.Text65 -> R.color.m3_text_65
            MaterialSlot.Primary -> R.color.m3_primary
            MaterialSlot.Track -> R.color.m3_track
            MaterialSlot.Up -> R.color.m3_up
            MaterialSlot.Down -> R.color.m3_down
            MaterialSlot.Strong -> R.color.m3_strong
            MaterialSlot.OnStrong -> R.color.m3_on_strong
            MaterialSlot.Pop -> R.color.m3_pop
            MaterialSlot.OnPop -> R.color.m3_on_pop
        }

        fun currentTokens(context: Context): Tokens {
            val battery = context.getSystemService(BatteryManager::class.java)
            val level = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            return Tokens(
                now = LocalDateTime.now(),
                battery = level?.takeIf { it in 0..100 },
                charging = battery?.isCharging == true,
                feeds = run {
                    val feeds = Feeds(context)
                    val values = feeds.snapshot().values
                    val countdowns = feeds.settings.countdowns.mapNotNull { c ->
                        runCatching { com.sidequest.widgetmaker.spec.Smart.Countdown(c.name, java.time.LocalDate.parse(c.date)) }.getOrNull()
                    }
                    com.sidequest.widgetmaker.feeds.NoopMath.merge(values, com.sidequest.widgetmaker.feeds.NoopBridge.values(context)) +
                        com.sidequest.widgetmaker.music.Music.values(context) +
                        com.sidequest.widgetmaker.feeds.Phone.values(context) +
                        com.sidequest.widgetmaker.feeds.Devices.values(context) +
                        com.sidequest.widgetmaker.spec.Smart.timeOfDay(LocalDateTime.now()) +
                        com.sidequest.widgetmaker.spec.Smart.calendarSoon(values, System.currentTimeMillis()) +
                        com.sidequest.widgetmaker.spec.Smart.countdowns(countdowns, java.time.LocalDate.now())
                },
            )
        }
    }
}

/** The widget at each starting size. They draw the same way; each widget follows its own size. */
enum class WidgetSize(val label: String, val receiver: Class<out AiWidgetReceiver>) {
    /** Only for app shortcuts: every other widget is at least 2 cells wide. */
    shortcut("1×1", AiWidgetShortcutReceiver::class.java),
    slim("2×1 slim", AiWidgetSlimReceiver::class.java),
    square("2×2", AiWidgetReceiver::class.java),
    wide("4×2", AiWidgetWideReceiver::class.java),
    large("4×4", AiWidgetLargeReceiver::class.java),
    ;

    companion object {
        /** Every placed widget, whatever size it started as. */
        fun allIds(context: Context): IntArray {
            val manager = AppWidgetManager.getInstance(context)
            return entries.flatMap { manager.getAppWidgetIds(android.content.ComponentName(context, it.receiver)).toList() }.toIntArray()
        }

        /** The size to suggest for a design: a shortcut starts 1×1, a pill slim. */
        fun suggested(design: com.sidequest.widgetmaker.spec.WidgetDesign) = when {
            isShortcut(design) -> shortcut
            design.shape == WidgetShape.pill -> slim
            else -> square
        }

        /** The sizes a design can be added at: 1×1 only for app shortcuts. */
        fun forDesign(design: com.sidequest.widgetmaker.spec.WidgetDesign) = entries.filter { it != shortcut || isShortcut(design) }

        /** An app shortcut (a widget that opens an app, like the Claude and Gemini ones). */
        fun isShortcut(design: com.sidequest.widgetmaker.spec.WidgetDesign) = design.root.tap?.startsWith("app:") == true
    }
}

class AiWidgetSlimReceiver : AiWidgetReceiver()
/** The 1×1 widget, for app shortcuts only (Claude, Gemini). */
class AiWidgetShortcutReceiver : AiWidgetReceiver()
class AiWidgetWideReceiver : AiWidgetReceiver()
class AiWidgetLargeReceiver : AiWidgetReceiver()

open class AiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AiWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        DarkModeWatch.watch(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val store = DesignStore(context)
        appWidgetIds.forEach { store.forget(it) }
    }
}

/** After "Add to home screen": links the new widget to the design it was added for. */
class PinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val designId = intent.getStringExtra(EXTRA_DESIGN_ID) ?: return
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val store = DesignStore(context)
        store.assign(appWidgetId, designId)
        // The widget keeps the design's shape itself too (a pill stays a pill).
        store.get(designId)?.design?.shape?.takeIf { it != WidgetShape.rounded }?.let { store.setShapeFor(appWidgetId, it) }
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                updateWidget(context, appWidgetId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_DESIGN_ID = "design_id"
    }
}

suspend fun updateWidget(context: Context, appWidgetId: Int) {
    AiWidget.refresh(context, GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId))
}

/** Everything one widget draws from, loaded fresh for each revision. */
private class Inputs(
    val saved: com.sidequest.widgetmaker.spec.SavedDesign?,
    val tokens: Tokens,
    val theme: WidgetTheme,
    val look: Look,
    val shape: WidgetShape?,
    val onScreen: Backdrop.Box?,
    val art: android.graphics.Bitmap?,
) {
    companion object {
        fun load(context: Context, appWidgetId: Int): Inputs {
            val store = DesignStore(context)
            val saved = designFor(context, store, appWidgetId)
            return Inputs(
                saved = saved,
                // This widget's own choice of devices to leave out of device batteries.
                tokens = AiWidget.currentTokens(context).let { t ->
                    val hidden = store.hiddenDevices(appWidgetId)
                    if (hidden.isEmpty()) t else t.copy(feeds = com.sidequest.widgetmaker.feeds.DevicesMath.hide(t.feeds, hidden))
                },
                // Ambient is for music widgets; another design shown there gets One UI's frost instead.
                theme = store.themeFor(appWidgetId).let { t ->
                    if (t == WidgetTheme.ambient && saved?.design?.let(WidgetTheme::playsMusic) != true) WidgetTheme.oneui else t
                },
                look = store.look,
                shape = store.shapeFor(appWidgetId),
                onScreen = store.boundsFor(appWidgetId, OneUiBackdrop.screenKey(context)),
                art = saved?.design?.let { if (usesArt(it.root)) com.sidequest.widgetmaker.music.Music.art(context) else null },
            )
        }

        /**
         * The design a widget shows. A 1×1 shortcut widget that wasn't given one shows your newest shortcut, or
         * the Claude one, rather than a design too big for it.
         */
        private fun designFor(context: Context, store: DesignStore, appWidgetId: Int): com.sidequest.widgetmaker.spec.SavedDesign? {
            val picked = store.pickedFor(appWidgetId)
            val shortcut = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider?.className == AiWidgetShortcutReceiver::class.java.name
            if (picked != null || !shortcut) return picked ?: store.designFor(appWidgetId)
            return store.all().firstOrNull { WidgetSize.isShortcut(it.design) }
                ?: com.sidequest.widgetmaker.spec.Starters.all.firstOrNull { WidgetSize.isShortcut(it.design) }
                    ?.let { com.sidequest.widgetmaker.spec.SavedDesign("starter_${it.title}", it.title, it.design, createdAt = 0) }
        }

        private fun usesArt(node: Node): Boolean = when (node) {
            is ArtNode -> true
            is ColumnNode -> node.children.any(::usesArt)
            is RowNode -> node.children.any(::usesArt)
            else -> false
        }
    }
}

@Composable
private fun EmptyWidget(modifier: GlanceModifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Color(0xE61E1E2E)).cornerRadius(24.dp).padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Tap to design this widget",
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp, textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun DesignWidget(
    design: WidgetDesign,
    tokens: Tokens,
    theme: WidgetTheme,
    look: Look,
    shape: WidgetShape?,
    onScreen: Backdrop.Box?,
    open: android.content.Intent,
    albumArt: android.graphics.Bitmap? = null,
) {
    val context = androidx.glance.LocalContext.current
    // The widget's size right now: its panel, and the elements with size rules, follow it.
    val size = LocalSize.current
    // Launchers (Android 12+) may round a widget's corners to the system's radius; corners no tighter than that
    // mean any rounding they do lands on ours, rather than cropping the panel's edge.
    val rounded = systemCornerRadius(context)?.let { sys ->
        val own = look.cornerRadius ?: design.cornerRadius
        if (own < sys) look.copy(cornerRadius = sys) else null
    } ?: look
    val panel = Shapes.panel(size.width.value.toInt(), size.height.value.toInt(), design, rounded, shape)
    val mood = com.sidequest.widgetmaker.spec.WeatherMood.forDesign(design, look, tokens.feeds, System.currentTimeMillis())
    val art = PanelArt.build(context, theme, panel, look, onScreen, mood, tokens.feeds)
    // On a bigger screen (an open foldable) the content can grow with the widget.
    val k = Shapes.scale(panel, look)
    // Until a blurred widget knows where it sits, its parts don't take taps of their own: any tap then
    // lands on the panel, which is the only tap that says where the widget is. (A pill that is all text
    // otherwise never learns it.) Once it knows, the parts' own taps come back.
    val paint = Paint(art.styler, albumArt, k, partTaps = !(theme.showsWallpaper && onScreen == null), weatherIcons = look.weatherIcons)
    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(GlanceModifier.size(panel.width.dp, panel.height.dp)) {
            // The panel: its shape and fill (tinted with live Material colors where they apply), then its edge.
            Image(
                provider = ImageProvider(art.fill),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = if (art.tinted) ColorFilter.tint(paint(Role.Background, design.background)) else null,
            )
            // A design that fills (like a battery): the panel's shape up to the level, in the theme's accent.
            design.fill?.let { fill ->
                val (fraction, color) = com.sidequest.widgetmaker.spec.fillLevel(fill, tokens)
                FillArt.build(context, panel, fraction)?.let {
                    Image(
                        provider = ImageProvider(it),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                        colorFilter = ColorFilter.tint(paint(color.first, color.second)),
                    )
                }
            }
            art.edge?.let {
                Image(ImageProvider(it), contentDescription = null, modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            }
            // The panel's own tap, under the content: parts without a tap of their own let it through.
            AndroidRemoteViews(remoteViews = tapLayer(context, open), modifier = GlanceModifier.fillMaxSize())
            Box(
                GlanceModifier.fillMaxSize().padding(start = (panel.padH * k).dp, end = (panel.padH * k).dp, top = (panel.padTop * k).dp, bottom = (panel.padV * k).dp),
            ) {
                // Size rules see the widget as it is before scaling, so a scaled 2×2 stays a 2×2 layout.
                val fit = Fit((panel.width / k).toInt(), (panel.height / k).toInt(), tokens, inner = (panel.height / k).toInt() - panel.padTop - panel.padV)
                val phoneText = PhoneText(context)
                // Sized to fit the panel: shrunk if it's too tall, long texts in crowded rows cut short with "…".
                val fitted = com.sidequest.widgetmaker.spec.Fitter.fit(
                    design.root, (panel.width - 2 * panel.padH).toFloat(), (panel.height - panel.padTop - panel.padV).toFloat(), k, tokens,
                    measure = phoneText, fontScale = phoneText.fontScale, shows = fit::shows,
                )
                val fittedPaint = if (fitted.k == k) paint else Paint(art.styler, albumArt, fitted.k, partTaps = paint.partTaps, weatherIcons = look.weatherIcons)
                if (fit.shows(fitted.root)) GlanceNode(fitted.root, tokens, fittedPaint, fit, GlanceModifier.fillMaxSize())
            }
        }
    }
}

/**
 * Colors for the widget under the chosen theme. Material 3 colors are handed
 * over as color resources, which the launcher looks up when it draws: they
 * match the phone's current palette (wallpaper or a One UI color palette) and
 * light or dark mode, with nothing cached in this app.
 */
private class Paint(
    val styler: Styler,
    val albumArt: android.graphics.Bitmap? = null,
    val k: Float = 1f,
    val partTaps: Boolean = true,
    val weatherIcons: com.sidequest.widgetmaker.spec.WeatherIcons = com.sidequest.widgetmaker.spec.WeatherIcons.classic,
) {
    /** Drawing inside a tile of [tone]: text there reads on the tile. */
    fun inside(tone: com.sidequest.widgetmaker.spec.Tone?) =
        if (tone == null) this else Paint(styler.inside(tone), albumArt, k, partTaps, weatherIcons)

    operator fun invoke(role: Role, design: String?): ColorProvider {
        // Colors that mean something (a red Charge) stay as they are in every theme.
        if (role == Role.Level) return ColorProvider(Color(Colors.argb(design)))
        if (styler.theme == WidgetTheme.material) return ColorProvider(AiWidget.materialRes(styler.materialSlot(role, design)))
        return ColorProvider(Color(styler.argb(role, design, dark = false)))
    }

    /** Text cut out of a block of [role]'s color: the panel's own color for Material 3, else black or white. */
    fun onText(role: Role, design: String?): ColorProvider {
        if (styler.theme == WidgetTheme.material && role != Role.Level) return ColorProvider(AiWidget.materialRes(MaterialSlot.Surface))
        return ColorProvider(Color(com.sidequest.widgetmaker.spec.Styler.contrast(styler.argb(role, design, dark = false))))
    }
}

@Composable
private fun GlanceNode(node: Node, tokens: Tokens, paint: Paint, fit: Fit, base: GlanceModifier = GlanceModifier) {
    // A month is drawn as the rows and columns of text it lays out to (the preview draws the same).
    if (node is com.sidequest.widgetmaker.spec.MonthNode) {
        GlanceNode(com.sidequest.widgetmaker.spec.MonthGrid.expand(node, tokens.now.toLocalDate(), tokens.feeds, tokens.locale), tokens, paint, fit, base)
        return
    }
    // Its own tap, if it has one: a headline's article, a calendar entry, the clock app...
    val context = androidx.glance.LocalContext.current
    val tap = if (paint.partTaps) Taps.forNode(node)?.let { Taps.resolve(it, tokens) } else null
    val modifier = tap?.let {
        // Music buttons act at once, without opening anything.
        if (it is TapAction.Media) base.clickable(actionSendBroadcast(MusicControlReceiver.intent(context, it.command)))
        else base.clickable(actionStartActivity(TapIntents.intent(context, it)))
    } ?: base
    when (node) {
        // A scrolling list: the launcher scrolls it (a ListView underneath), one row per child.
        is ColumnNode -> if (node.scroll) androidx.glance.appwidget.lazy.LazyColumn(
            modifier = container(modifier, paint, node.background, node.cornerRadius, node.padding, node.tone),
            horizontalAlignment = horizontal(node.align),
        ) {
            node.children.filter(fit::shows).forEach { child ->
                item {
                    val wide = if (spansWidth(child)) GlanceModifier.fillMaxWidth() else GlanceModifier
                    GlanceNode(child, tokens, paint.inside(node.tone), fit, wide)
                }
            }
        } else Column(
            modifier = container(modifier, paint, node.background, node.cornerRadius, node.padding, node.tone),
            horizontalAlignment = horizontal(node.align),
            verticalAlignment = vertical(node.verticalAlign),
        ) {
            node.children.filter(fit::shows).forEach { child ->
                // Rows that spread their parts out (and columns holding them) span the column's width.
                val wide = if (spansWidth(child)) GlanceModifier.fillMaxWidth() else GlanceModifier
                GlanceNode(child, tokens, paint.inside(node.tone), fit, if (child.grow) GlanceModifier.defaultWeight().then(wide) else wide)
            }
        }
        is RowNode -> Row(
            // A row whose parts share out the space must span the width (launchers don't stretch rows themselves).
            modifier = container(
                if (!node.grow && node.children.any { it.grow }) modifier.fillMaxWidth() else modifier,
                paint, node.background, node.cornerRadius, node.padding, node.tone,
            ),
            horizontalAlignment = horizontal(node.align),
            verticalAlignment = vertical(node.verticalAlign),
        ) {
            node.children.filter(fit::shows).forEach { child ->
                val tall = if (node.stretch) GlanceModifier.fillMaxHeight() else GlanceModifier
                GlanceNode(child, tokens, paint.inside(node.tone), fit, if (child.grow) GlanceModifier.defaultWeight().then(tall) else tall)
            }
        }
        is TextNode -> {
            val (role, color) = textColor(node, tokens)
            // Highlighted (today in a calendar): a small block in the text's color, the text cut out of it.
            val marked = when {
                node.highlight -> modifier.background(paint(role, color)).cornerRadius((6 * paint.k).dp).padding(horizontal = (2 * paint.k).dp, vertical = (1 * paint.k).dp)
                // A pill behind the text in the theme's container color.
                node.chip -> modifier.background(paint(chipRole(node), "#262FA7B8")).cornerRadius((node.size * paint.k).dp)
                    .padding(horizontal = (9 * paint.k).dp, vertical = (4 * paint.k).dp)
                else -> modifier
            }
            // A line too wide for its room: the home screen shrinks it to fit, measuring it itself.
            if (node.autoFit && !node.highlight) {
                AndroidRemoteViews(
                    remoteViews = fitTextViews(node, tokens.fill(node.text), role, color, paint.styler, paint.k),
                    modifier = marked.fillMaxWidth().height((node.size * paint.k * PhoneText(context).fontScale * 1.25f).dp),
                )
                return
            }
            Text(
                text = tokens.fill(node.text),
                modifier = marked,
                maxLines = node.maxLines,
                style = TextStyle(
                    color = if (node.highlight) paint.onText(role, color) else paint(role, color),
                    fontSize = (node.size * paint.k).sp,
                    fontWeight = when (node.weight) {
                        Weight.bold -> FontWeight.Bold
                        Weight.medium -> FontWeight.Medium
                        else -> FontWeight.Normal
                    },
                    textAlign = when (node.align) {
                        HAlign.start -> TextAlign.Start
                        HAlign.center -> TextAlign.Center
                        HAlign.end -> TextAlign.End
                    },
                ),
            )
        }
        is ClockNode -> AndroidRemoteViews(remoteViews = clockViews(node, paint.styler, paint.k), modifier = modifier)
        is IconNode -> {
            // A weather icon as a Meteocon (in its own colors, a touch bigger: its art has room around it),
            // the current weather's animated when chosen; else the classic icon, tinted.
            val mcName = if (paint.weatherIcons == com.sidequest.widgetmaker.spec.WeatherIcons.classic) null else Meteocons.nameFor(node, tokens)
            val mc = mcName?.let { Meteocons.frames(context, it) }?.takeIf { it.isNotEmpty() }
            val mcSize = (node.size * paint.k * mcScale(node.size)).dp
            val animated = if (mcName != null && mc != null && paint.weatherIcons.moves && Meteocons.isNow(node)) {
                Meteocons.flipper(context, mcName, calm = paint.weatherIcons == com.sidequest.widgetmaker.spec.WeatherIcons.calm)
            } else null
            when {
                animated != null -> AndroidRemoteViews(remoteViews = animated, modifier = modifier.size(mcSize))
                mc != null -> Image(provider = ImageProvider(mc[0]), contentDescription = null, modifier = modifier.size(mcSize))
                else -> Image(
                    provider = ImageProvider(iconRes(node, tokens)),
                    contentDescription = null,
                    modifier = modifier.size((node.size * paint.k).dp),
                    colorFilter = ColorFilter.tint(accentColor(node.levelOf, node.levels, node.color, tokens, node.tone).let { (r, c) -> paint(r, c) }),
                )
            }
        }
        is ProgressNode -> if (node.capsule) {
            // An upright capsule filling from the bottom, its value in a small pill at the top. The corners are
            // bigger than half its width, which rounds it into a capsule whatever width it ends up.
            val h = (node.height * paint.k).dp
            val f = tokens.progress(node.value)
            val on = accentColor(node.levelOf, node.levels, node.color, tokens, node.tone).let { (r, c) -> paint(r, c) }
            Box(modifier.width(h * CAPSULE_WIDTH).height(h).cornerRadius(h / 2).background(paint(Role.Track, node.trackColor)), contentAlignment = Alignment.TopCenter) {
                Column(GlanceModifier.fillMaxSize()) {
                    Spacer(GlanceModifier.height(h * (1f - f)))
                    if (f > 0f) Box(GlanceModifier.fillMaxWidth().height(h * f).cornerRadius(h / 2).background(on)) {}
                }
                node.text?.let {
                    Box(GlanceModifier.padding(top = (6 * paint.k).dp)) {
                        Text(
                            tokens.fill(it), maxLines = 1,
                            modifier = GlanceModifier.background(paint(Role.Background, null)).cornerRadius((node.textSize * paint.k).dp)
                                .padding(horizontal = (6 * paint.k).dp, vertical = (2 * paint.k).dp),
                            style = TextStyle(color = paint(Role.Text, node.textColor), fontSize = (node.textSize * paint.k).sp, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        } else if (node.segments > 0) {
            // A level meter: blocks that fill up with the value (bottom to top when vertical).
            val filled = Math.round(tokens.progress(node.value) * node.segments)
            val on = accentColor(node.levelOf, node.levels, node.color, tokens, node.tone).let { (r, c) -> paint(r, c) }
            val off = paint(Role.Track, node.trackColor)
            val gap = (1.5f * paint.k).dp
            if (node.vertical) {
                Column(modifier.width((node.height * 0.4f * paint.k).dp).height((node.height * paint.k).dp)) {
                    for (i in 0 until node.segments) {
                        Box(GlanceModifier.defaultWeight().fillMaxWidth().padding(vertical = gap)) {
                            Box(GlanceModifier.fillMaxSize().background(if (i >= node.segments - filled) on else off).cornerRadius((3 * paint.k).dp)) {}
                        }
                    }
                }
            } else {
                Row(modifier.fillMaxWidth().height((node.height * paint.k).dp)) {
                    for (i in 0 until node.segments) {
                        Box(GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = gap)) {
                            Box(GlanceModifier.fillMaxSize().background(if (i < filled) on else off).cornerRadius((3 * paint.k).dp)) {}
                        }
                    }
                }
            }
        } else if (node.pill) {
            // One UI's battery pill: the bar fills a fully rounded pill, with an icon and the value inside it.
            val h = (node.height * paint.k).dp
            Box(modifier.fillMaxWidth().height(h).cornerRadius(h / 2), contentAlignment = Alignment.CenterStart) {
                LinearProgressIndicator(
                    progress = tokens.progress(node.value),
                    modifier = GlanceModifier.fillMaxSize(),
                    color = accentColor(node.levelOf, node.levels, node.color, tokens, node.tone).let { (r, c) -> paint(r, c) },
                    backgroundColor = paint(Role.Track, node.trackColor),
                )
                Row(GlanceModifier.fillMaxSize().padding(horizontal = h * 0.4f), verticalAlignment = Alignment.CenterVertically) {
                    val iconNode = IconNode(node.icon ?: IconName.battery, live = node.iconLive, size = (node.height * 0.55f).toInt())
                    if (node.icon != null || node.iconLive != null) Image(
                        provider = ImageProvider(iconRes(iconNode, tokens)),
                        contentDescription = null,
                        modifier = GlanceModifier.size((iconNode.size * paint.k).dp),
                        // Like One UI's: the opposite of the text, white by day and black at night.
                        colorFilter = ColorFilter.tint(paint.onText(Role.Text, node.textColor)),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    node.text?.let {
                        Text(tokens.fill(it), maxLines = 1, style = TextStyle(color = paint(Role.Text, node.textColor), fontSize = (node.textSize * paint.k).sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        } else {
            // A plain bar; Expressive's is thicker and rounded at its ends.
            val h = (paint.styler.theme.barHeight(node.height) * paint.k).dp
            val bar = @Composable { m: GlanceModifier ->
                LinearProgressIndicator(
                    progress = tokens.progress(node.value),
                    modifier = m,
                    color = accentColor(node.levelOf, node.levels, node.color, tokens, node.tone).let { (r, c) -> paint(r, c) },
                    backgroundColor = paint(Role.Track, node.trackColor),
                )
            }
            if (paint.styler.theme == WidgetTheme.expressive) Box(modifier.fillMaxWidth().height(h).cornerRadius(h / 2)) { bar(GlanceModifier.fillMaxSize()) }
            else bar(modifier.fillMaxWidth().height(h))
        }
        is TimerNode -> AndroidRemoteViews(remoteViews = timerViews(node, paint.styler, paint.k), modifier = modifier)
        is ChartNode -> {
            val density = context.resources.displayMetrics.density
            val values = ChartArt.parse(tokens.fill(node.series))
            val w = (fit.width * density).toInt(); val h = (node.height * paint.k * density).toInt()
            Image(
                provider = ImageProvider(
                    if (node.style == "ticks") ChartArt.ticks(values, w, h, 2.4f * density, min = node.min, max = node.max)
                    else ChartArt.line(values, w, h, 2.2f * density, node.min, node.max),
                ),
                contentDescription = null,
                modifier = modifier.fillMaxWidth().height((node.height * paint.k).dp),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(paint(Role.Accent, node.color)),
            )
        }
        is RingNode -> {
            @Suppress("NAME_SHADOWING") val node = node.fitted(fit.inner)
            val density = context.resources.displayMetrics.density
            val px = (node.size * paint.k * density).toInt()
            val thick = node.thickness * paint.k * density
            val (role, color) = accentColor(node.levelOf, node.levels, node.color, tokens, node.tone)
            Box(modifier.size((node.size * paint.k).dp), contentAlignment = Alignment.Center) {
                Image(ImageProvider(ChartArt.ring(px, thick, null, node.sweep)), null, GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(paint(Role.Track, node.trackColor)))
                Image(ImageProvider(ChartArt.ring(px, thick, node.fraction(tokens), node.sweep)), null, GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(paint(role, color)))
                Text(
                    tokens.fill(node.text),
                    maxLines = 1,
                    style = TextStyle(color = paint(Role.Text, node.textColor), fontSize = (node.textSize * paint.k).sp, fontWeight = FontWeight.Bold),
                )
            }
        }
        is SleepNode -> Image(
            provider = ImageProvider(SleepBar.draw(tokens.feeds["health.sleep.stages"], heightPx = node.height * 3)),
            contentDescription = null,
            modifier = modifier.fillMaxWidth().height((node.height * paint.k).dp),
            contentScale = ContentScale.FillBounds,
        )
        is ArtNode -> {
            val cover = paint.albumArt
            if (cover != null) {
                Image(
                    provider = ImageProvider(ArtCorners.round(cover, node.cornerRadius.toFloat() / node.size)),
                    contentDescription = null,
                    modifier = modifier.size((node.size * paint.k).dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // No art: a music note on a soft tile the same size.
                Box(
                    modifier.size((node.size * paint.k).dp).cornerRadius((node.cornerRadius * paint.k).dp).background(paint(Role.Container, "#26FFFFFF")),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.icon_music),
                        contentDescription = null,
                        modifier = GlanceModifier.size((node.size * paint.k / 2).dp),
                        colorFilter = ColorFilter.tint(paint(Role.Accent, "#CCFFFFFF")),
                    )
                }
            }
        }
        is com.sidequest.widgetmaker.spec.MonthNode -> Unit // Drawn as its rows and columns, above.
        is com.sidequest.widgetmaker.spec.AnalogNode -> AndroidRemoteViews(
            remoteViews = analogViews(node, paint.styler),
            modifier = modifier.size((node.size * paint.k).dp),
        )
        is com.sidequest.widgetmaker.spec.ScaleNode -> {
            // A tick ruler, with the value's tick over it in the accent color.
            val density = context.resources.displayMetrics.density
            val (wDp, hDp) = if (node.vertical) 30f * paint.k to (if (node.grow || node.length <= 0) fit.inner * 0.8f else node.length.toFloat()) * paint.k
            else fit.width.toFloat() to 22f * paint.k
            val sized = if (node.vertical) {
                if (node.grow || node.length <= 0) modifier.width(wDp.dp).fillMaxHeight() else modifier.size(wDp.dp, hDp.dp)
            } else modifier.fillMaxWidth().height(hDp.dp)
            val f = node.fraction(tokens)
            fun art(marker: Boolean) = ChartArt.scale((wDp * density).toInt(), (hDp * density).toInt(), node.ticks, node.vertical, density, marker, f)
            Box(sized) {
                Image(provider = ImageProvider(art(false)), contentDescription = null, modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.tint(paint(Role.Text, node.tickColor)))
                if (f != null) Image(provider = ImageProvider(art(true)), contentDescription = null, modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.tint(paint(Role.Accent, node.color)))
            }
        }
        is com.sidequest.widgetmaker.spec.AppIconNode -> {
            val px = (node.size * paint.k * context.resources.displayMetrics.density).toInt()
            val size = (node.size * paint.k).dp
            val mono = if (node.mono) AppIcons.mono(context, node.packageName, px) else null
            val icon = if (node.mono) null else AppIcons.bitmap(context, node.packageName, px)
            when {
                mono != null -> Image(provider = ImageProvider(mono), contentDescription = null, modifier = modifier.size(size),
                    colorFilter = ColorFilter.tint(paint(Role.Text, "#FFFFFFFF")))
                icon != null -> Image(provider = ImageProvider(icon), contentDescription = null, modifier = modifier.size(size))
                else -> Image(
                    provider = ImageProvider(R.drawable.icon_sparkle), contentDescription = null, modifier = modifier.size(size),
                    colorFilter = ColorFilter.tint(paint(if (node.mono) Role.Text else Role.Accent, "#FFFFFFFF")),
                )
            }
        }
        is com.sidequest.widgetmaker.spec.MapNode -> {
            val sized = (if (node.grow) modifier.fillMaxWidth() else modifier.fillMaxWidth().height((node.height * paint.k).dp))
                .cornerRadius((node.cornerRadius * paint.k).dp)
            val uri = tokens.feeds["map.traffic"]?.let { com.sidequest.widgetmaker.feeds.TrafficMap.uri(context, it) }
            if (uri != null) {
                Image(
                    provider = ImageProvider(android.graphics.drawable.Icon.createWithContentUri(uri)),
                    contentDescription = "Traffic map",
                    modifier = sized,
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(sized.background(paint(Role.Container, "#26FFFFFF")), contentAlignment = Alignment.Center) {
                    Text("Add a TomTom key and your home in Data sources", style = TextStyle(color = paint(Role.Text, "#B3FFFFFF"), fontSize = (12 * paint.k).sp, textAlign = TextAlign.Center))
                }
            }
        }
        is SpacerNode -> Spacer(if (node.grow) modifier else modifier.size((node.size * paint.k).dp))
        is DividerNode -> if (paint.styler.theme.drawsDividers) Box(modifier.fillMaxWidth().height(node.thickness.dp).background(paint(Role.Container, node.color))) {}
            else Spacer(modifier.fillMaxWidth().height(node.thickness.dp))
    }
}

@Composable
private fun clockViews(node: ClockNode, styler: Styler, k: Float = 1f): RemoteViews {
    val context = androidx.glance.LocalContext.current
    val layout = when (node.weight) {
        Weight.bold, Weight.medium -> R.layout.clock_bold
        Weight.light -> R.layout.clock_light
        Weight.normal -> R.layout.clock_normal
    }
    return RemoteViews(context.packageName, layout).apply {
        // The same pattern whatever the phone's 12/24-hour setting: the design chose it.
        setCharSequence(R.id.clock, "setFormat12Hour", node.format)
        setCharSequence(R.id.clock, "setFormat24Hour", node.format)
        val role = clockRole(node)
        if (styler.theme == WidgetTheme.material) {
            val res = AiWidget.materialRes(styler.materialSlot(role, node.color))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Looked up by the launcher, like the rest of the Material 3 colors.
                setColorStateList(R.id.clock, "setTextColor", res)
            } else {
                setTextColor(R.id.clock, context.getColor(res))
            }
        } else {
            setTextColor(R.id.clock, styler.argb(role, node.color, dark = false))
        }
        setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_SP, node.size * k)
    }
}

/**
 * A live analog clock, its dial, hour hand and minute hand tinted to the theme (Android 12+; before that they keep
 * their own grey and teal). The system turns the hands.
 */
@Composable
private fun analogViews(node: com.sidequest.widgetmaker.spec.AnalogNode, styler: Styler): RemoteViews {
    val context = androidx.glance.LocalContext.current
    return RemoteViews(context.packageName, R.layout.analog_clock).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            fun tint(method: String, role: Role, design: String) {
                if (styler.theme == WidgetTheme.material) setColorStateList(R.id.analog, method, AiWidget.materialRes(styler.materialSlot(role, design)))
                else setColorStateList(R.id.analog, method, android.content.res.ColorStateList.valueOf(styler.argb(role, design, dark = false)))
            }
            tint("setDialTintList", Role.Track, node.dialColor)
            tint("setHourHandTintList", Role.Text, node.handColor)
            tint("setMinuteHandTintList", Role.Accent, node.accent)
        }
    }
}

/** The text sizes the shrink-to-fit layouts start from (res/layout/fit_text_<weight>_<size>.xml). */
private val FIT_SIZES = intArrayOf(10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 32, 36, 40, 44, 48, 52, 56, 64, 72, 80, 96)

/**
 * One line of text that the home screen shrinks to fit its width (Android's autosizing, which measures it
 * in the real font), starting from the closest size at or under the design's. Never cut short with "…".
 */
@Composable
private fun fitTextViews(node: TextNode, text: String, role: Role, design: String?, styler: Styler, k: Float): RemoteViews {
    val context = androidx.glance.LocalContext.current
    val target = node.size * k
    val start = FIT_SIZES.lastOrNull { it <= target + 0.5f } ?: FIT_SIZES.first()
    val weight = when (node.weight) {
        Weight.bold -> "b"
        Weight.medium -> "m"
        else -> "n"
    }
    val layout = context.resources.getIdentifier("fit_text_${weight}_$start", "layout", context.packageName)
    return RemoteViews(context.packageName, layout).apply {
        setTextViewText(R.id.fit_text, text)
        when {
            role == Role.Level -> setTextColor(R.id.fit_text, Colors.argb(design))
            styler.theme == WidgetTheme.material -> {
                val res = AiWidget.materialRes(styler.materialSlot(role, design))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setColorStateList(R.id.fit_text, "setTextColor", res)
                else setTextColor(R.id.fit_text, context.getColor(res))
            }
            else -> setTextColor(R.id.fit_text, styler.argb(role, design, dark = false))
        }
        setInt(
            R.id.fit_text, "setGravity",
            android.view.Gravity.CENTER_VERTICAL or when (node.align) {
                HAlign.start -> android.view.Gravity.START
                HAlign.center -> android.view.Gravity.CENTER_HORIZONTAL
                HAlign.end -> android.view.Gravity.END
            },
        )
    }
}

/** A system-run timer: how far into the song it is, counting while it plays. */
@Composable
private fun timerViews(node: TimerNode, styler: Styler, k: Float = 1f): RemoteViews {
    val context = androidx.glance.LocalContext.current
    val now = com.sidequest.widgetmaker.music.Music.current(context)
    val elapsed = android.os.SystemClock.elapsedRealtime()
    val at = now?.let { com.sidequest.widgetmaker.music.MusicMath.position(it, elapsed) } ?: 0L
    return RemoteViews(context.packageName, R.layout.music_timer).apply {
        setChronometer(R.id.timer, elapsed - at, null, now?.playing == true)
        if (styler.theme == WidgetTheme.material) {
            val res = AiWidget.materialRes(styler.materialSlot(Role.Text, node.color))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setColorStateList(R.id.timer, "setTextColor", res)
            else setTextColor(R.id.timer, context.getColor(res))
        } else {
            setTextColor(R.id.timer, styler.argb(Role.Text, node.color, dark = false))
        }
        setTextViewTextSize(R.id.timer, TypedValue.COMPLEX_UNIT_SP, node.size * k)
    }
}

/** Whether a row or column needs the full width: it (or one inside it) shares out space with "grow". */
private fun spansWidth(node: Node): Boolean = when (node) {
    is com.sidequest.widgetmaker.spec.MonthNode, is com.sidequest.widgetmaker.spec.MapNode -> true
    is RowNode -> node.children.any { it.grow } || node.children.any(::spansWidth)
    is ColumnNode -> node.children.any(::spansWidth)
    else -> false
}

private fun container(
    modifier: GlanceModifier, paint: Paint, background: String?, radius: Int, padding: Int,
    tone: com.sidequest.widgetmaker.spec.Tone? = null,
): GlanceModifier {
    var m = modifier
    if (background != null || tone != null) m = m.background(paint(Role.toned(Role.Container, tone), background ?: "#FF2F5C9E")).cornerRadius((radius * paint.k).dp)
    if (padding > 0) m = m.padding((padding * paint.k).dp)
    return m
}

private fun horizontal(align: HAlign) = when (align) {
    HAlign.start -> Alignment.Horizontal.Start
    HAlign.center -> Alignment.Horizontal.CenterHorizontally
    HAlign.end -> Alignment.Horizontal.End
}

private fun vertical(align: VAlign) = when (align) {
    VAlign.top -> Alignment.Vertical.Top
    VAlign.center -> Alignment.Vertical.CenterVertically
    VAlign.bottom -> Alignment.Vertical.Bottom
}

/** A text's color and what it means, following [TextNode.trendOf] when it's set. */
fun textColor(node: TextNode, tokens: Tokens): Pair<Role, String> {
    levelColor(node.levelOf, node.levels, tokens)?.let { return Role.Level to it }
    // A toned chip's text reads on the chip; toned text is in the tone itself.
    node.tone?.let { return (if (node.chip) Role.on(it) else Role.toned(Role.Text, it)) to node.color }
    return when (node.trendOf?.let { tokens.trend(it) }) {
        true -> Role.TrendUp to node.upColor
        false -> Role.TrendDown to node.downColor
        null -> Role.Text to node.color
    }
}

/** An icon's or a bar's color: its level color when it has one, else its own as an accent. */
fun accentColor(
    levelOf: String?, levels: List<com.sidequest.widgetmaker.spec.Level>?, color: String, tokens: Tokens,
    tone: com.sidequest.widgetmaker.spec.Tone? = null,
): Pair<Role, String> = levelColor(levelOf, levels, tokens)?.let { Role.Level to it } ?: (Role.toned(Role.Accent, tone) to color)

/** A chip's fill: the panel's container color, or its tone's. */
fun chipRole(node: TextNode) = Role.toned(Role.Container, node.tone)

/** A clock's color: its tone's, else the accent or text color. */
fun clockRole(node: ClockNode) = node.tone?.let { Role.toned(Role.Text, it) } ?: if (node.accent) Role.Accent else Role.Text

/** The widget's size for size rules, and [inner]: the height inside its padding, which rings shrink to. */
/** An upright capsule's width, as a part of its height. */
const val CAPSULE_WIDTH = 0.42f

data class Fit(val width: Int, val height: Int, val tokens: Tokens? = null, val inner: Int = height) {
    fun shows(node: Node) = visible(node, width, height, tokens)
}

/**
 * The drawable for an icon. A [IconNode.live] icon follows its value (like a
 * forecast hour's weather), and "weather" follows the current weather.
 */
fun iconRes(node: IconNode, tokens: Tokens): Int {
    fun parse(name: String?) = name?.trim()?.let { runCatching { IconName.valueOf(it) }.getOrNull() }
    val live = node.live?.let { parse(tokens.fill(it)) }
    if (live != null && live != IconName.weather) return iconRes(live)
    if (node.name != IconName.weather && node.live == null) return iconRes(node.name)
    return iconRes(parse(tokens.feeds["weather.icon"]) ?: if (node.name == IconName.weather) IconName.sun else node.name)
}

fun iconRes(name: IconName): Int = when (name) {
    IconName.star -> R.drawable.icon_star
    IconName.heart -> R.drawable.icon_heart
    IconName.bolt -> R.drawable.icon_bolt
    IconName.moon -> R.drawable.icon_moon
    IconName.sun -> R.drawable.icon_sun
    IconName.battery -> R.drawable.icon_battery
    IconName.calendar -> R.drawable.icon_calendar
    IconName.music -> R.drawable.icon_music
    IconName.cloud -> R.drawable.icon_cloud
    IconName.rain -> R.drawable.icon_rain
    IconName.snow -> R.drawable.icon_snow
    IconName.storm -> R.drawable.icon_storm
    IconName.car -> R.drawable.icon_car
    IconName.news -> R.drawable.icon_news
    IconName.ticket -> R.drawable.icon_ticket
    IconName.coin -> R.drawable.icon_coin
    IconName.umbrella -> R.drawable.icon_umbrella
    IconName.pin -> R.drawable.icon_pin
    IconName.flame -> R.drawable.icon_flame
    IconName.egg -> R.drawable.icon_egg
    IconName.paw -> R.drawable.icon_paw
    IconName.steps -> R.drawable.icon_steps
    IconName.check -> R.drawable.icon_check
    IconName.play -> R.drawable.icon_play
    IconName.pause -> R.drawable.icon_pause
    IconName.next -> R.drawable.icon_next
    IconName.previous -> R.drawable.icon_previous
    IconName.weather -> R.drawable.icon_sun
    IconName.phone -> R.drawable.icon_phone
    IconName.watch -> R.drawable.icon_watch
    IconName.headphones -> R.drawable.icon_headphones
    IconName.band -> R.drawable.icon_band
    IconName.bluetooth -> R.drawable.icon_bluetooth
    IconName.sparkle -> R.drawable.icon_sparkle
}

/**
 * An invisible layer that opens [intent] when tapped. Glance seals its taps (immutable PendingIntents),
 * and Android drops everything a launcher adds to a sealed tap, including where the widget is on the
 * screen; this one is left open (mutable), so the tap arrives with the widget's place and the One UI
 * blur can line up. The intent is explicit (this app's tap screen), as an open tap must be.
 */
private fun tapLayer(context: Context, intent: android.content.Intent): android.widget.RemoteViews {
    val pending = android.app.PendingIntent.getActivity(
        context,
        intent.getIntExtra(TapIntents.WIDGET, 0),
        intent,
        android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
    )
    return android.widget.RemoteViews(context.packageName, R.layout.tap_layer).apply {
        setOnClickPendingIntent(R.id.tap_layer, pending)
    }
}

/**
 * How much bigger a Meteocon is drawn than a classic icon of [size] dp: their art has room around it.
 * Only a touch, so the text under a big one keeps its room; small ones (in forecast rows) stay as designed.
 */
fun mcScale(size: Int): Float = if (size >= 24) 1.12f else 1f

/** The corner radius launchers round widgets to (Android 12+), in dp, or null before then. */
private fun systemCornerRadius(context: Context): Int? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return runCatching {
        val px = context.resources.getDimension(android.R.dimen.system_app_widget_background_radius)
        (px / context.resources.displayMetrics.density).toInt()
    }.getOrNull()?.takeIf { it > 0 }
}
