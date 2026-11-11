package com.sidequest.widgetmaker.ui

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sidequest.widgetmaker.data.DesignStore
import com.sidequest.widgetmaker.feeds.Feeds
import com.sidequest.widgetmaker.spec.TapAction
import com.sidequest.widgetmaker.spec.TapKind
import java.net.URLEncoder

/**
 * Where a widget tap lands. Links and calendar events open straight away; an
 * app kind opens the app you picked, the system's default (clock, calendar,
 * maps, battery), or a small chooser with "Always use this app".
 */
class TapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = TapIntents.read(intent)
        if (action == null) return finish()
        if (action !is TapAction.Open) {
            AppOpener.open(this, action)
            return finish()
        }
        if (AppOpener.openSavedOrDefault(this, action.kind)) return finish()
        setContent {
            MakerTheme {
                Surface(shape = MaterialTheme.shapes.extraLarge) {
                    AppPicker(
                        kind = action.kind,
                        offerAlways = true,
                        onPicked = { choice, always ->
                            if (always) DesignStore(this).setTapApp(action.kind, choice)
                            AppOpener.openChoice(this, action.kind, choice)
                            finish()
                        },
                        onCancel = { finish() },
                    )
                }
            }
        }
    }
}

/** Packs a tap into the intent a widget element launches, and reads it back. */
object TapIntents {
    fun intent(context: Context, action: TapAction): Intent {
        val intent = Intent(context, TapActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        when (action) {
            is TapAction.Link -> intent.putExtra("url", action.url)
            is TapAction.CalendarEvent -> intent.putExtra("event", longArrayOf(action.id, action.beginMs, action.endMs))
            is TapAction.Open -> intent.putExtra("kind", action.kind.name)
        }
        // A distinct data URI per target, so each element gets its own PendingIntent.
        return intent.setData(Uri.parse("aiwidget://tap/" + action.hashCode().toUInt()))
    }

    fun read(intent: Intent?): TapAction? {
        intent ?: return null
        intent.getStringExtra("url")?.let { return TapAction.Link(it) }
        intent.getLongArrayExtra("event")?.takeIf { it.size == 3 }?.let { return TapAction.CalendarEvent(it[0], it[1], it[2]) }
        return intent.getStringExtra("kind")?.let { runCatching { TapAction.Open(TapKind.valueOf(it)) }.getOrNull() }
    }
}

/** Opens links, events and apps for taps. */
object AppOpener {
    /** A choice saved for a kind: a package name, or this for the web. */
    const val WEB = "web"

    fun open(context: Context, action: TapAction) {
        when (action) {
            is TapAction.Link -> start(context, Intent(Intent.ACTION_VIEW, Uri.parse(action.url)))
            is TapAction.CalendarEvent -> start(
                context,
                Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, action.id))
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, action.beginMs)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, action.endMs),
            )
            is TapAction.Open -> if (!openSavedOrDefault(context, action.kind)) {
                start(context, TapIntents.intent(context, action))
            }
        }
    }

    /** Opens the saved app for [kind], or else the system default where Android has one. False if it should ask. */
    fun openSavedOrDefault(context: Context, kind: TapKind): Boolean {
        val store = DesignStore(context)
        store.tapApp(kind)?.let { choice ->
            if (openChoice(context, kind, choice)) return true
            store.setTapApp(kind, null) // Uninstalled since: ask again.
        }
        val default = systemDefault(context, kind) ?: return false
        return start(context, default)
    }

    /** Android's own default for a kind, if it has one. */
    fun systemDefault(context: Context, kind: TapKind): Intent? = when (kind) {
        TapKind.clock -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
        TapKind.calendar -> Intent(Intent.ACTION_VIEW).setData(
            CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                .appendPath(System.currentTimeMillis().toString()).build(),
        )
        TapKind.battery -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        TapKind.commute -> {
            // To wherever the commute is headed now: work in the morning, home from noon.
            val s = Feeds(context).settings
            val toWork = java.time.LocalTime.now().hour < 12
            val place = (if (toWork) s.work else s.home) ?: (s.work ?: s.home)
            place?.let {
                val label = if (place == s.work) "Work" else "Home"
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:${it.lat},${it.lon}?q=${it.lat},${it.lon}(${Uri.encode(label)})"))
            }
        }
        TapKind.weather, TapKind.crypto, TapKind.news, TapKind.events -> null
    }

    fun hasSystemDefault(kind: TapKind) = kind in setOf(TapKind.clock, TapKind.calendar, TapKind.battery, TapKind.commute)

    fun openChoice(context: Context, kind: TapKind, choice: String): Boolean {
        if (choice == WEB) return start(context, Intent(Intent.ACTION_VIEW, Uri.parse(webPage(context, kind))))
        val launch = context.packageManager.getLaunchIntentForPackage(choice) ?: return false
        return start(context, launch)
    }

    /** A web page for a kind, when no app fits. */
    fun webPage(context: Context, kind: TapKind): String {
        val town = Feeds(context).settings.place?.name?.substringBefore(",").orEmpty()
        fun search(q: String) = "https://www.google.com/search?q=" + URLEncoder.encode(q.trim(), "UTF-8")
        return when (kind) {
            TapKind.weather -> search("weather $town")
            TapKind.crypto -> "https://www.coingecko.com/"
            TapKind.news -> "https://news.google.com/"
            TapKind.events -> search("events near $town this week")
            TapKind.calendar -> "https://calendar.google.com/"
            TapKind.clock -> search("time")
            TapKind.commute -> "https://www.google.com/maps"
            TapKind.battery -> search("battery tips")
        }
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    /** An installed app that can be opened from the launcher. */
    data class App(val packageName: String, val label: String, val icon: Drawable?)

    /** Installed apps, the ones that fit [kind] first. */
    fun apps(context: Context, kind: TapKind): Pair<List<App>, List<App>> {
        val pm = context.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val all = pm.queryIntentActivities(main, 0)
            .map { it: ResolveInfo -> App(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
        val (suggested, others) = all.partition { fits(kind, it) }
        return suggested to others
    }

    private val WORDS = mapOf(
        TapKind.weather to listOf("weather", "forecast", "accuweather", "radar", "climate"),
        TapKind.crypto to listOf("coinbase", "binance", "kraken", "crypto", "robinhood", "coin", "bitcoin", "wallet", "exodus", "gemini", "webull", "uphold"),
        TapKind.news to listOf("news", "feedly", "flipboard", "inoreader", "reddit", "newsbreak", "bbc", "cnn", "nytimes", "guardian", "apnews", "reuters"),
        TapKind.events to listOf("ticket", "eventbrite", "meetup", "seatgeek", "stubhub", "bandsintown", "fever", "songkick", "live nation", "axs"),
        TapKind.calendar to listOf("calendar", "planner", "agenda"),
        TapKind.clock to listOf("clock", "alarm"),
        TapKind.commute to listOf("maps", "waze", "navigation", "transit", "citymapper", "here we go"),
        TapKind.battery to listOf("battery", "device care", "settings"),
    )

    private fun fits(kind: TapKind, app: App): Boolean {
        val text = (app.label + " " + app.packageName).lowercase()
        return WORDS[kind].orEmpty().any { text.contains(it) }
    }
}

/**
 * Lists apps for a kind (fitting ones first), plus the web, and an "Always
 * use this app" box. Shown on a tap with nothing picked yet, and in Settings.
 */
@Composable
fun AppPicker(
    kind: TapKind,
    offerAlways: Boolean,
    onPicked: (choice: String, always: Boolean) -> Unit,
    onCancel: () -> Unit,
    onAskEveryTime: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val (suggested, others) = remember(kind) { AppOpener.apps(context, kind) }
    var showAll by remember { mutableStateOf(suggested.isEmpty()) }
    var always by remember { mutableStateOf(true) }

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Open ${kind.label.lowercase()} with", style = MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.heightIn(max = 380.dp)) {
            if (suggested.isNotEmpty()) item { Label("Suggested") }
            items(suggested, key = { "s" + it.packageName }) { app -> AppRow(app) { onPicked(app.packageName, always) } }
            item {
                AppRow(AppOpener.App(AppOpener.WEB, "On the web", null)) { onPicked(AppOpener.WEB, always) }
            }
            if (!showAll) {
                item { TextButton(onClick = { showAll = true }) { Text("Show all apps") } }
            } else {
                item { HorizontalDivider(Modifier.padding(vertical = 6.dp)); Label("All apps") }
                items(others, key = { "o" + it.packageName }) { app -> AppRow(app) { onPicked(app.packageName, always) } }
            }
        }
        if (offerAlways) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { always = !always }) {
                Checkbox(checked = always, onCheckedChange = { always = it })
                Text("Always use this app (change it in Settings)")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onAskEveryTime?.let { TextButton(onClick = it) { Text("Ask every time") } }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun AppRow(app: AppOpener.App, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            app.icon?.let { Image(it.toBitmap(64, 64).asImageBitmap(), contentDescription = null, modifier = Modifier.size(32.dp)) }
                ?: Text("🌐", style = MaterialTheme.typography.titleLarge)
        }
        Text(app.label, style = MaterialTheme.typography.bodyLarge)
    }
}
