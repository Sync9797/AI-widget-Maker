package com.sidequest.widgetmaker.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.sidequest.widgetmaker.data.DesignStore
import com.sidequest.widgetmaker.feeds.Feeds
import com.sidequest.widgetmaker.widget.AiWidget
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.spec.WidgetShape
import com.sidequest.widgetmaker.widget.updateWidget
import kotlinx.coroutines.launch

/**
 * A widget's own settings: shown when it's added from the widget list, and
 * when you long-press it and tap its settings. Pick its theme and its design.
 */
class ChooseDesignActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // Keep the widget even if nothing is picked: it shows the newest design (or a "tap to design" note).
        setResult(RESULT_OK, result)

        val store = DesignStore(this)
        val designs = store.all()
        setContent {
            // null: follow the app's theme (Settings → Widget style).
            var own by remember { mutableStateOf(if (store.hasOwnTheme(appWidgetId)) store.themeFor(appWidgetId) else null) }
            var picked by remember { mutableStateOf(store.designFor(appWidgetId)?.id) }
            var shape by remember { mutableStateOf(store.shapeFor(appWidgetId)) }
            val theme = own ?: store.theme

            var refreshing by remember { mutableStateOf(false) }
            var refreshNote by remember { mutableStateOf(lastUpdated(this@ChooseDesignActivity)) }

            // Fetches every data source now and redraws all widgets (this one included).
            fun refresh() = lifecycleScope.launch {
                refreshing = true
                refreshNote = "Refreshing…"
                val snapshot = runCatching { Feeds(this@ChooseDesignActivity).refresh() }.getOrNull()
                AiWidget.refreshAll(this@ChooseDesignActivity)
                refreshing = false
                refreshNote = when {
                    snapshot == null -> "Couldn't refresh. Check your internet connection."
                    snapshot.errors.isNotEmpty() ->
                        "Updated ${snapshot.values["updated"].orEmpty()}, but: " +
                            snapshot.errors.entries.joinToString("; ") { (k, v) -> "$k: $v" }
                    else -> "Updated ${snapshot.values["updated"].orEmpty()}"
                }
            }

            fun apply(finishAfter: Boolean = false) = lifecycleScope.launch {
                updateWidget(this@ChooseDesignActivity, appWidgetId)
                if (finishAfter) finish()
            }

            MakerTheme {
                Surface {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("Widget settings", style = MaterialTheme.typography.titleLarge)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { refresh() }, enabled = !refreshing) {
                                if (refreshing) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.size(8.dp))
                                }
                                Text("Refresh now")
                            }
                            Text(refreshNote, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        }

                        // Change the design this widget shows, in the app: describe the change, preview, save.
                        picked?.let { id ->
                            Button(onClick = {
                                startActivity(
                                    Intent(this@ChooseDesignActivity, MainActivity::class.java)
                                        .putExtra(MainActivity.EXTRA_EDIT, id)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                                )
                                finish()
                            }, modifier = Modifier.fillMaxWidth()) { Text("Change this design…") }
                        }

                        Text("Style", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(own == null, {
                                own = null
                                store.setThemeFor(appWidgetId, null)
                                apply()
                            }, { Text("App default (${store.theme.label})") })
                            WidgetTheme.entries.forEach { t ->
                                FilterChip(own == t, {
                                    own = t
                                    store.setThemeFor(appWidgetId, t)
                                    apply()
                                }, { Text(t.label) })
                            }
                        }

                        Text("Shape", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(shape == null, {
                                shape = null
                                store.setShapeFor(appWidgetId, null)
                                apply()
                            }, { Text("Design's") })
                            WidgetShape.entries.forEach { s ->
                                FilterChip(shape == s, {
                                    shape = s
                                    store.setShapeFor(appWidgetId, s)
                                    apply()
                                }, { Text(s.label) })
                            }
                        }

                        Text("Design", style = MaterialTheme.typography.titleMedium)
                        if (designs.isEmpty()) {
                            Text("You haven't saved one yet. Make one in the app (or pick a ready-made one) and it will show up here.")
                            Button(onClick = {
                                startActivity(Intent(this@ChooseDesignActivity, MainActivity::class.java))
                                finish()
                            }) { Text("Open AI Widget Maker") }
                        }
                        designs.forEach { saved ->
                            Column(
                                Modifier.clickable {
                                    picked = saved.id
                                    store.assign(appWidgetId, saved.id)
                                    apply()
                                },
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    (if (saved.id == picked) "✓ " else "") + saved.design.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (saved.id == picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                DesignPreview(saved.design, PreviewSize.Square, theme, shape = shape)
                            }
                        }
                        Button(onClick = { apply(finishAfter = true) }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                    }
                }
            }
        }
    }
}

/** "Last updated 3:12 PM", from the saved data, or a hint if nothing has been fetched yet. */
private fun lastUpdated(context: android.content.Context): String {
    val snapshot = Feeds(context).snapshot()
    return snapshot.values["updated"]?.let { "Last updated $it" } ?: "Fetches weather, news and your other data now."
}
