package com.sidequest.widgetmaker.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sidequest.widgetmaker.data.DesignStore
import com.sidequest.widgetmaker.spec.Backdrop
import com.sidequest.widgetmaker.spec.Shapes
import com.sidequest.widgetmaker.widget.OneUiBackdrop
import kotlin.math.roundToInt

/**
 * Where a widget sits, shown by hand: the wallpaper as the screen shows it, with an outline the size of
 * the widget to drag over the spot where it sits. For home screens that don't say where a widget is
 * when it's tapped (Samsung's, for one), the only way the blur can line up.
 */
@Composable
fun PlaceBlurDialog(appWidgetId: Int, onSaved: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { DesignStore(context) }
    val picture = remember { OneUiBackdrop.screenPicture(context) }
    val (sw, sh) = remember { OneUiBackdrop.screenSize(context) }
    val screen = remember { OneUiBackdrop.screenKey(context) }
    // The widget's panel in screen pixels: its cell as the launcher reports it, then the panel inside.
    val panel = remember { panelPx(context, appWidgetId, store) }
    // Where it was placed before on this screen, else the middle.
    val start = remember {
        store.boundsFor(appWidgetId, screen)?.takeIf { store.knowsPlace(appWidgetId, screen) }?.let { it.left to it.top }
            ?: ((sw - panel.first) / 2 to (sh - panel.second) / 2)
    }
    var left by remember { mutableStateOf(start.first.toFloat()) }
    var top by remember { mutableStateOf(start.second.toFloat()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            // Clear of the status and navigation bars, so Save and Cancel are never under them.
            Column(Modifier.systemBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Place the blur", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Drag the outline to where this widget sits on your home screen, then Save. Its blur then shows " +
                        "exactly that part of your wallpaper.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (picture == null) {
                    Text(
                        "Your wallpaper can't be read yet: allow wallpaper access (or pick your wallpaper picture) in " +
                            "Settings → Widget style → One UI first.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        // The screen, scaled to fit, keeping its shape.
                        val density = LocalDensity.current
                        val fitW = with(density) { maxWidth.toPx() }
                        val fitH = with(density) { maxHeight.toPx() }
                        val k = minOf(fitW / sw, fitH / sh)
                        val boxW = with(density) { (sw * k).toDp() }
                        val boxH = with(density) { (sh * k).toDp() }
                        Box(
                            Modifier.size(boxW, boxH).clip(RoundedCornerShape(20.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                        ) {
                            Image(picture.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                            Box(
                                Modifier
                                    .offset { IntOffset((left * k).roundToInt(), (top * k).roundToInt()) }
                                    .size(with(density) { (panel.first * k).toDp() }, with(density) { (panel.second * k).toDp() })
                                    .background(Color(0x55FFFFFF), RoundedCornerShape(12.dp))
                                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                                    .pointerInput(k) {
                                        detectDragGestures { change, drag ->
                                            change.consume()
                                            left = (left + drag.x / k).coerceIn(0f, (sw - panel.first).toFloat().coerceAtLeast(0f))
                                            top = (top + drag.y / k).coerceIn(0f, (sh - panel.second).toFloat().coerceAtLeast(0f))
                                        }
                                    },
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(enabled = picture != null, onClick = {
                        val l = left.roundToInt()
                        val t = top.roundToInt()
                        store.setBoundsFor(appWidgetId, Backdrop.Box(l, t, l + panel.first, t + panel.second), screen)
                        onSaved()
                    }) { Text("Save") }
                }
            }
        }
    }
}

/** The widget's panel size in pixels, from its cell as the launcher reports it (portrait), then its look and shape. */
private fun panelPx(context: Context, appWidgetId: Int, store: DesignStore): Pair<Int, Int> {
    val density = context.resources.displayMetrics.density
    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    val portrait = context.resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val cellW = options.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).takeIf { it > 0 } ?: 160
    val cellH = options.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 } ?: 160
    val design = store.designFor(appWidgetId)?.design
    val (w, h) = if (design != null) {
        Shapes.panel(cellW, cellH, design, store.look, store.shapeFor(appWidgetId)).let { it.width to it.height }
    } else cellW to cellH
    return (w * density).roundToInt() to (h * density).roundToInt()
}
