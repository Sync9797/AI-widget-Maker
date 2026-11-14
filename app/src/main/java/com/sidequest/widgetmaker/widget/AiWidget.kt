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
        fun styler(context: Context, theme: WidgetTheme, backdropLight: Boolean = false): Styler =
            if (theme.frosted) {
                Styler(theme, { slot, dark -> oneUiTone(context, slot, dark) }, backdropLight)
            } else {
                Styler(theme, { slot, _ -> context.getColor(materialRes(slot)) })
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

        /** The size to suggest for a design: a pill starts slim. */
        fun suggested(design: com.sidequest.widgetmaker.spec.WidgetDesign) =
            if (design.shape == WidgetShape.pill) slim else square
    }
}

class AiWidgetSlimReceiver : AiWidgetReceiver()
class AiWidgetWideReceiver : AiWidgetReceiver()
class AiWidgetLargeReceiver : AiWidgetReceiver()

open class AiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AiWidget()

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
            return Inputs(
                saved = store.designFor(appWidgetId),
                tokens = AiWidget.currentTokens(context),
                // Ambient is for music widgets; another design shown there gets One UI's frost instead.
                theme = store.themeFor(appWidgetId).let { t ->
                    if (t == WidgetTheme.ambient && store.designFor(appWidgetId)?.design?.let(WidgetTheme::playsMusic) != true) WidgetTheme.oneui else t
                },
                look = store.look,
                shape = store.shapeFor(appWidgetId),
                onScreen = store.boundsFor(appWidgetId, OneUiBackdrop.screenKey(context)),
                art = store.designFor(appWidgetId)?.design?.let { if (usesArt(it.root)) com.sidequest.widgetmaker.music.Music.art(context) else null },
            )
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
    val panel = Shapes.panel(size.width.value.toInt(), size.height.value.toInt(), design, look, shape)
    val art = PanelArt.build(context, theme, panel, look, onScreen)
    // On a bigger screen (an open foldable) the content can grow with the widget.
    val k = Shapes.scale(panel, look)
    // Until a blurred widget knows where it sits, its parts don't take taps of their own: any tap then
    // lands on the panel, which is the only tap that says where the widget is. (A pill that is all text
    // otherwise never learns it.) Once it knows, the parts' own taps come back.
    val paint = Paint(art.styler, albumArt, k, partTaps = !(theme.frosted && onScreen == null), weatherIcons = look.weatherIcons)
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
                if (fit.shows(design.root)) GlanceNode(design.root, tokens, paint, fit, GlanceModifier.fillMaxSize())
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
    operator fun invoke(role: Role, design: String?): ColorProvider {
        // Colors that mean something (a red Charge) stay as they are in every theme.
        if (role == Role.Level) return ColorProvider(Color(Colors.argb(design)))
        if (styler.theme == WidgetTheme.material) return ColorProvider(AiWidget.materialRes(styler.materialSlot(role, design)))
        return ColorProvider(Color(styler.argb(role, design, dark = false)))
    }
}

@Composable
private fun GlanceNode(node: Node, tokens: Tokens, paint: Paint, fit: Fit, base: GlanceModifier = GlanceModifier) {
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
            modifier = container(modifier, paint, node.background, node.cornerRadius, node.padding),
            horizontalAlignment = horizontal(node.align),
        ) {
            node.children.filter(fit::shows).forEach { child ->
                item {
                    val wide = if (spansWidth(child)) GlanceModifier.fillMaxWidth() else GlanceModifier
                    GlanceNode(child, tokens, paint, fit, wide)
                }
            }
        } else Column(
            modifier = container(modifier, paint, node.background, node.cornerRadius, node.padding),
            horizontalAlignment = horizontal(node.align),
            verticalAlignment = vertical(node.verticalAlign),
        ) {
            node.children.filter(fit::shows).forEach { child ->
                // Rows that spread their parts out (and columns holding them) span the column's width.
                val wide = if (spansWidth(child)) GlanceModifier.fillMaxWidth() else GlanceModifier
                GlanceNode(child, tokens, paint, fit, if (child.grow) GlanceModifier.defaultWeight().then(wide) else wide)
            }
        }
        is RowNode -> Row(
            // A row whose parts share out the space must span the width (launchers don't stretch rows themselves).
            modifier = container(
                if (!node.grow && node.children.any { it.grow }) modifier.fillMaxWidth() else modifier,
                paint, node.background, node.cornerRadius, node.padding,
            ),
            horizontalAlignment = horizontal(node.align),
            verticalAlignment = vertical(node.verticalAlign),
        ) {
            node.children.filter(fit::shows).forEach { child ->
                GlanceNode(child, tokens, paint, fit, if (child.grow) GlanceModifier.defaultWeight() else GlanceModifier)
            }
        }
        is TextNode -> {
            val (role, color) = textColor(node, tokens)
            Text(
                text = tokens.fill(node.text),
                modifier = modifier,
                maxLines = node.maxLines,
                style = TextStyle(
                    color = paint(role, color),
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
            val mc = if (paint.weatherIcons == com.sidequest.widgetmaker.spec.WeatherIcons.classic) null
            else Meteocons.nameFor(node, tokens)?.let { Meteocons.frames(context, it) }?.takeIf { it.isNotEmpty() }
            val mcSize = (node.size * paint.k * mcScale(node.size)).dp
            when {
                mc != null && mc.size > 1 && paint.weatherIcons == com.sidequest.widgetmaker.spec.WeatherIcons.animated && Meteocons.isNow(node) ->
                    AndroidRemoteViews(remoteViews = Meteocons.flipper(context, mc), modifier = modifier.size(mcSize))
                mc != null -> Image(provider = ImageProvider(mc[0]), contentDescription = null, modifier = modifier.size(mcSize))
                else -> Image(
                    provider = ImageProvider(iconRes(node, tokens)),
                    contentDescription = null,
                    modifier = modifier.size((node.size * paint.k).dp),
                    colorFilter = ColorFilter.tint(accentColor(node.levelOf, node.levels, node.color, tokens).let { (r, c) -> paint(r, c) }),
                )
            }
        }
        is ProgressNode -> LinearProgressIndicator(
            progress = tokens.progress(node.value),
            modifier = modifier.fillMaxWidth().height((node.height * paint.k).dp),
            color = accentColor(node.levelOf, node.levels, node.color, tokens).let { (r, c) -> paint(r, c) },
            backgroundColor = paint(Role.Track, node.trackColor),
        )
        is TimerNode -> AndroidRemoteViews(remoteViews = timerViews(node, paint.styler, paint.k), modifier = modifier)
        is ChartNode -> {
            val density = context.resources.displayMetrics.density
            val values = ChartArt.parse(tokens.fill(node.series))
            Image(
                provider = ImageProvider(ChartArt.line(values, (fit.width * density).toInt(), (node.height * paint.k * density).toInt(), 2.2f * density, node.min, node.max)),
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
            val (role, color) = accentColor(node.levelOf, node.levels, node.color, tokens)
            Box(modifier.size((node.size * paint.k).dp), contentAlignment = Alignment.Center) {
                Image(ImageProvider(ChartArt.ring(px, thick, null)), null, GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(paint(Role.Track, node.trackColor)))
                Image(ImageProvider(ChartArt.ring(px, thick, tokens.progress(node.value))), null, GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(paint(role, color)))
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
        is SpacerNode -> Spacer(if (node.grow) modifier else modifier.size((node.size * paint.k).dp))
        is DividerNode -> Box(modifier.fillMaxWidth().height(node.thickness.dp).background(paint(Role.Container, node.color))) {}
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
        if (styler.theme == WidgetTheme.material) {
            val res = AiWidget.materialRes(styler.materialSlot(Role.Text, node.color))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Looked up by the launcher, like the rest of the Material 3 colors.
                setColorStateList(R.id.clock, "setTextColor", res)
            } else {
                setTextColor(R.id.clock, context.getColor(res))
            }
        } else {
            setTextColor(R.id.clock, styler.argb(Role.Text, node.color, dark = false))
        }
        setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_SP, node.size * k)
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
    is RowNode -> node.children.any { it.grow } || node.children.any(::spansWidth)
    is ColumnNode -> node.children.any(::spansWidth)
    else -> false
}

private fun container(modifier: GlanceModifier, paint: Paint, background: String?, radius: Int, padding: Int): GlanceModifier {
    var m = modifier
    if (background != null) m = m.background(paint(Role.Container, background)).cornerRadius((radius * paint.k).dp)
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
    return when (node.trendOf?.let { tokens.trend(it) }) {
        true -> Role.TrendUp to node.upColor
        false -> Role.TrendDown to node.downColor
        null -> Role.Text to node.color
    }
}

/** An icon's or a bar's color: its level color when it has one, else its own as an accent. */
fun accentColor(levelOf: String?, levels: List<com.sidequest.widgetmaker.spec.Level>?, color: String, tokens: Tokens): Pair<Role, String> =
    levelColor(levelOf, levels, tokens)?.let { Role.Level to it } ?: (Role.Accent to color)

/** The widget's size for size rules, and [inner]: the height inside its padding, which rings shrink to. */
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
 * Small ones (in forecast rows) stay as designed, so the rows keep their room.
 */
fun mcScale(size: Int): Float = if (size >= 24) 1.3f else 1f
