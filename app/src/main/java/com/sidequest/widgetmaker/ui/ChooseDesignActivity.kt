@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.sidequest.widgetmaker.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.material3.TextButton
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
            val picked = remember { store.designFor(appWidgetId) }
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
                    snapshot.errors.isNotEmpty() -> "Updated ${snapshot.values["updated"].orEmpty()}, with some sources missing"
                    else -> "Updated ${snapshot.values["updated"].orEmpty()}"
                }
            }

            fun apply(finishAfter: Boolean = false) = lifecycleScope.launch {
                updateWidget(this@ChooseDesignActivity, appWidgetId)
                if (finishAfter) finish()
            }

            /** Opens the app to pick another design for this widget (from the gallery or yours). */
            fun pickAnother() {
                startActivity(
                    Intent(this@ChooseDesignActivity, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_ASSIGN, appWidgetId)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
                finish()
            }

            MakerTheme {
                // A sheet at the bottom of the screen, short enough that the home screen (and the widget on it)
                // stays in view above; tapping above it closes it.
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x66000000))
                            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { apply(finishAfter = true) },
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().widthIn(max = 640.dp)
                            .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.62f).dp),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 6.dp,
                    ) {
                        Column(
                            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // The grab handle.
                            Box(
                                Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)),
                            )
                            // The widget being edited, live: style and shape changes show here at once.
                            if (picked != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    DesignPreview(picked.design, previewSize(appWidgetId), theme, Modifier.weight(1f, fill = false), shape = shape)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(picked.design.name, style = MaterialTheme.typography.titleLarge)
                                        Text(refreshNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    androidx.compose.material3.IconButton(onClick = { refresh() }, enabled = !refreshing) {
                                        if (refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                        else androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Refresh, contentDescription = "Refresh now")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = ::pickAnother, modifier = Modifier.weight(1f)) { Text("Change design") }
                                    OutlinedButton(onClick = {
                                        startActivity(
                                            Intent(this@ChooseDesignActivity, MainActivity::class.java)
                                                .putExtra(MainActivity.EXTRA_EDIT, picked.id)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                                        )
                                        finish()
                                    }, modifier = Modifier.weight(1f)) { Text("Edit it") }
                                }
                            } else {
                                Text("Pick a design for this widget", style = MaterialTheme.typography.titleLarge)
                                Button(onClick = ::pickAnother, modifier = Modifier.fillMaxWidth()) { Text(if (designs.isEmpty()) "Open AI Widget Maker" else "Choose a design") }
                            }

                            SheetLabel("Style")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(own == null, {
                                    own = null
                                    store.setThemeFor(appWidgetId, null)
                                    apply()
                                }, { Text("App default (${store.theme.label})") })
                                // Ambient (album art) only for widgets that show music.
                                val music = picked?.design?.let(WidgetTheme::playsMusic) == true
                                (if (music) WidgetTheme.entries else WidgetTheme.everywhere).forEach { t ->
                                    FilterChip(own == t, {
                                        own = t
                                        store.setThemeFor(appWidgetId, t)
                                        apply()
                                    }, { Text(t.label) })
                                }
                            }

                            SheetLabel("Shape")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(shape == null, {
                                    shape = null
                                    store.setShapeFor(appWidgetId, null)
                                    apply()
                                }, { Text("Design's") })
                                WidgetShape.entries.forEach { sh ->
                                    FilterChip(shape == sh, {
                                        shape = sh
                                        store.setShapeFor(appWidgetId, sh)
                                        apply()
                                    }, { Text(sh.label) })
                                }
                            }

                            // One UI and Ambient blur the wallpaper behind the widget: where it sits decides what shows.
                            if (theme.showsWallpaper) {
                                SheetLabel("Blur")
                                val known = store.knowsPlace(appWidgetId, com.sidequest.widgetmaker.widget.OneUiBackdrop.screenKey(this@ChooseDesignActivity))
                                Text(
                                    if (known) "Lined up with your wallpaper on this screen."
                                    else "Not lined up yet: tap the widget once, or place it by hand.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    com.sidequest.widgetmaker.widget.OneUiBackdrop.source(this@ChooseDesignActivity).description + ". " +
                                        (store.lastTap(appWidgetId) ?: "No tap on this widget yet."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                var placing by remember { mutableStateOf(false) }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { placing = true }) { Text("Place by hand") }
                                    if (known) TextButton(onClick = {
                                        store.forgetPlace(appWidgetId)
                                        android.widget.Toast.makeText(this@ChooseDesignActivity, "Now tap the widget once, anywhere on it, to line up its blur.", android.widget.Toast.LENGTH_LONG).show()
                                        apply(finishAfter = true)
                                    }) { Text("Line up again") }
                                }
                                if (placing) {
                                    PlaceBlurDialog(
                                        appWidgetId,
                                        onSaved = {
                                            placing = false
                                            android.widget.Toast.makeText(this@ChooseDesignActivity, "Blur placed.", android.widget.Toast.LENGTH_SHORT).show()
                                            apply()
                                        },
                                        onDismiss = { placing = false },
                                    )
                                }
                            }

                            Button(onClick = { apply(finishAfter = true) }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                        }
                    }
                }
            }
        }
    }

    /** The preview size closest to the widget's cell, so the sheet shows it as it sits. */
    private fun previewSize(appWidgetId: Int): PreviewSize {
        val options = AppWidgetManager.getInstance(this).getAppWidgetOptions(appWidgetId)
        val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: 160
        val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 } ?: 160
        val wide = w >= 250
        return when {
            h <= 130 -> if (wide) PreviewSize.Strip else PreviewSize.Small
            h >= 260 -> if (wide) PreviewSize.Large else PreviewSize.Tall
            else -> if (wide) PreviewSize.Wide else PreviewSize.Square
        }
    }
}

/** A small heading in the widget settings sheet. */
@androidx.compose.runtime.Composable
private fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/** "Last updated 3:12 PM", from the saved data, or a hint if nothing has been fetched yet. */
private fun lastUpdated(context: android.content.Context): String {
    val snapshot = Feeds(context).snapshot()
    return snapshot.values["updated"]?.let { "Last updated $it" } ?: "Fetches weather, news and your other data now."
}
