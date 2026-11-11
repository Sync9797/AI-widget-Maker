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
import com.sidequest.widgetmaker.spec.WidgetTheme
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
            val theme = own ?: store.theme

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
                                DesignPreview(saved.design, PreviewSize.Square, theme)
                            }
                        }
                        Button(onClick = { apply(finishAfter = true) }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                    }
                }
            }
        }
    }
}
