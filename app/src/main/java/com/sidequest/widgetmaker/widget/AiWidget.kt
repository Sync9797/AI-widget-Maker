package com.sidequest.widgetmaker.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
import com.sidequest.widgetmaker.spec.TextNode
import com.sidequest.widgetmaker.spec.Tokens
import com.sidequest.widgetmaker.spec.VAlign
import com.sidequest.widgetmaker.spec.Material3Colors
import com.sidequest.widgetmaker.spec.Role
import com.sidequest.widgetmaker.spec.Styler
import com.sidequest.widgetmaker.spec.Weight
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.spec.visible
import com.sidequest.widgetmaker.spec.WidgetDesign
import com.sidequest.widgetmaker.ui.MainActivity
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
        val store = DesignStore(context)
        val saved = withContext(Dispatchers.IO) { store.designFor(appWidgetId) }
        val tokens = currentTokens(context)
        val styler = styler(context, store.theme)
        provideContent {
            val open = GlanceModifier.clickable(actionStartActivity<MainActivity>())
            if (saved == null) EmptyWidget(open) else DesignWidget(saved.design, tokens, styler, open)
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) = AiWidget().updateAll(context)

        /** The chosen theme, with this phone's wallpaper colors for Material 3 (Android 12+). */
        fun styler(context: Context, theme: WidgetTheme): Styler {
            if (theme != WidgetTheme.material || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return Styler(theme)
            fun c(id: Int) = context.getColor(id)
            return Styler(
                theme,
                Material3Colors(
                    surfaceDay = c(android.R.color.system_neutral1_10),
                    surfaceNight = c(android.R.color.system_neutral1_900),
                    containerDay = c(android.R.color.system_accent2_100),
                    containerNight = c(android.R.color.system_accent2_700),
                    onSurfaceDay = c(android.R.color.system_neutral1_900),
                    onSurfaceNight = c(android.R.color.system_neutral1_100),
                    primaryDay = c(android.R.color.system_accent1_600),
                    primaryNight = c(android.R.color.system_accent1_200),
                ),
            )
        }

        fun currentTokens(context: Context): Tokens {
            val battery = context.getSystemService(BatteryManager::class.java)
            val level = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            return Tokens(
                now = LocalDateTime.now(),
                battery = level?.takeIf { it in 0..100 },
                charging = battery?.isCharging == true,
                feeds = Feeds(context).snapshot().values,
            )
        }
    }
}

class AiWidgetReceiver : GlanceAppWidgetReceiver() {
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
        DesignStore(context).assign(appWidgetId, designId)
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
    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
    AiWidget().update(context, glanceId)
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
private fun DesignWidget(design: WidgetDesign, tokens: Tokens, styler: Styler, modifier: GlanceModifier) {
    val paint = Paint(styler)
    val surface = if (styler.theme == WidgetTheme.glass) {
        // Glass: a translucent tint with a highlight and a bright edge, drawn from a drawable.
        modifier.background(ImageProvider(R.drawable.widget_glass))
    } else {
        modifier.background(paint(Role.Background, design.background))
    }
    Box(
        modifier = surface
            .fillMaxSize()
            .cornerRadius(design.cornerRadius.dp)
            .padding(design.padding.dp),
    ) {
        // The widget's size right now: elements with size rules appear or hide as it's resized.
        val size = LocalSize.current
        val fit = Fit(size.width.value.toInt(), size.height.value.toInt())
        if (fit.shows(design.root)) GlanceNode(design.root, tokens, paint, fit, GlanceModifier.fillMaxSize())
    }
}

/** Colors for the widget under the chosen theme; Material 3 colors switch with dark mode. */
private class Paint(val styler: Styler) {
    operator fun invoke(role: Role, design: String?): ColorProvider {
        val day = styler.argb(role, design, dark = false)
        if (!styler.followsDarkMode) return ColorProvider(Color(day))
        return androidx.glance.color.ColorProvider(day = Color(day), night = Color(styler.argb(role, design, dark = true)))
    }
}

@Composable
private fun GlanceNode(node: Node, tokens: Tokens, paint: Paint, fit: Fit, modifier: GlanceModifier = GlanceModifier) {
    when (node) {
        is ColumnNode -> Column(
            modifier = container(modifier, paint, node.background, node.cornerRadius, node.padding),
            horizontalAlignment = horizontal(node.align),
            verticalAlignment = vertical(node.verticalAlign),
        ) {
            node.children.filter(fit::shows).forEach { child ->
                GlanceNode(child, tokens, paint, fit, if (child.grow) GlanceModifier.defaultWeight() else GlanceModifier)
            }
        }
        is RowNode -> Row(
            modifier = container(modifier, paint, node.background, node.cornerRadius, node.padding),
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
                    fontSize = node.size.sp,
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
        is ClockNode -> AndroidRemoteViews(remoteViews = clockViews(node, paint.styler), modifier = modifier)
        is IconNode -> Image(
            provider = ImageProvider(iconRes(node, tokens)),
            contentDescription = null,
            modifier = modifier.size(node.size.dp),
            colorFilter = ColorFilter.tint(paint(Role.Accent, node.color)),
        )
        is ProgressNode -> LinearProgressIndicator(
            progress = tokens.progress(node.value),
            modifier = modifier.fillMaxWidth().height(node.height.dp),
            color = paint(Role.Accent, node.color),
            backgroundColor = paint(Role.Track, node.trackColor),
        )
        is SpacerNode -> Spacer(if (node.grow) modifier else modifier.size(node.size.dp))
    }
}

@Composable
private fun clockViews(node: ClockNode, styler: Styler): RemoteViews {
    val context = androidx.glance.LocalContext.current
    val layout = when (node.weight) {
        Weight.bold, Weight.medium -> R.layout.clock_bold
        Weight.light -> R.layout.clock_light
        Weight.normal -> R.layout.clock_normal
    }
    val day = styler.argb(Role.Text, node.color, dark = false)
    val night = styler.argb(Role.Text, node.color, dark = true)
    return RemoteViews(context.packageName, layout).apply {
        // The same pattern whatever the phone's 12/24-hour setting: the design chose it.
        setCharSequence(R.id.clock, "setFormat12Hour", node.format)
        setCharSequence(R.id.clock, "setFormat24Hour", node.format)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && day != night) {
            setColorInt(R.id.clock, "setTextColor", day, night)
        } else {
            val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            setTextColor(R.id.clock, if (dark) night else day)
        }
        setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_SP, node.size.toFloat())
    }
}

private fun container(modifier: GlanceModifier, paint: Paint, background: String?, radius: Int, padding: Int): GlanceModifier {
    var m = modifier
    if (background != null) m = m.background(paint(Role.Container, background)).cornerRadius(radius.dp)
    if (padding > 0) m = m.padding(padding.dp)
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
fun textColor(node: TextNode, tokens: Tokens): Pair<Role, String> = when (node.trendOf?.let { tokens.trend(it) }) {
    true -> Role.TrendUp to node.upColor
    false -> Role.TrendDown to node.downColor
    null -> Role.Text to node.color
}

/** A widget size, in dp. */
data class Fit(val width: Int, val height: Int) {
    fun shows(node: Node) = visible(node, width, height)
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
    IconName.weather -> R.drawable.icon_sun
}
