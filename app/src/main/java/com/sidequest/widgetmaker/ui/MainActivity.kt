package com.sidequest.widgetmaker.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidequest.widgetmaker.feeds.FeedWorker
import com.sidequest.widgetmaker.ai.ClaudeApp
import com.sidequest.widgetmaker.spec.SavedDesign
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.spec.WidgetShape

class MainActivity : ComponentActivity() {
    private val viewModel: MakerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        CrashLog.install(this)
        FeedWorker.schedule(this)
        openFrom(intent)
        // Settings → Updates: look for a newer build each time the app opens.
        if (savedInstanceState == null && com.sidequest.widgetmaker.update.Updater.checkOnStart(this)) viewModel.checkForUpdates(quiet = true)
        setContent { MakerTheme { MakerScreen(viewModel) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFrom(intent)
    }

    /** "Change this design" in a widget's settings opens it here, ready to change. */
    private fun openFrom(intent: Intent?) {
        intent?.getStringExtra(EXTRA_EDIT)?.let { viewModel.editById(it) }
        intent?.removeExtra(EXTRA_EDIT)
    }

    companion object {
        const val EXTRA_EDIT = "edit_design"
    }

    override fun onResume() {
        super.onResume()
        // Coming back from setting a wallpaper or allowing access to it: read it again for the blur.
        com.sidequest.widgetmaker.widget.OneUiBackdrop.forgetCached()
        viewModel.refreshWidgets()
    }
}

@Composable
fun MakerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val base = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    // Premium: in the dark, deep near-black surfaces (with a hint of the phone's palette) so the
    // widgets glow on them; cards step up in gentle tones of the same.
    val colors = if (!dark) base else base.copy(
        background = androidx.compose.ui.graphics.Color(0xFF0B0C10),
        surface = androidx.compose.ui.graphics.Color(0xFF0B0C10),
        surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF050608),
        surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF121318),
        surfaceContainer = androidx.compose.ui.graphics.Color(0xFF16181E),
        surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF1E2028),
        surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF272A33),
    )
    val type = androidx.compose.material3.Typography().let { t ->
        t.copy(
            headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
            titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
    val shapes = androidx.compose.material3.Shapes(
        small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
    )
    MaterialTheme(colorScheme = colors, typography = type, shapes = shapes, content = content)
}

private val IDEAS = listOf(
    "A big minimalist clock with the date underneath",
    "Today's weather with the high, low and chance of rain",
    "Crypto ticker for my coins, green and red for up and down",
    "My commute time with traffic, and the weather",
    "Top 3 local headlines on a newspaper-style card",
    "What's on nearby this week: the next 3 events",
    "Retro terminal: green text on black, time, date and battery",
    "Pastel sunset gradient vibe with a greeting and the time",
    "A battery widget with a glowing progress bar",
    "Cozy night mode: moon icon, time and day of the week",
)

@Composable
internal fun DescribeCard(busy: Boolean, free: Boolean, onGenerate: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Describe a widget", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("A dark glassy clock with today's date and my battery…") },
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IDEAS.forEach { idea -> SuggestionChip(onClick = { text = idea }, label = { Text(idea) }) }
            }
            BusyButton(if (free) "Design in Claude (free)" else "Design it", busy = busy, enabled = text.isNotBlank()) { onGenerate(text) }
            if (free) {
                Text(
                    "Opens the Claude app (or claude.ai) with your request; a free account works. " +
                        "Prefer it automatic? Add an API key in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
internal fun DraftCard(
    draft: Draft,
    busy: Boolean,
    onRefine: (String) -> Unit,
    onSave: () -> Unit,
    onSaveAndAdd: () -> Unit,
    onDiscard: () -> Unit,
    theme: WidgetTheme,
    free: Boolean,
    onShape: (WidgetShape) -> Unit,
) {
    var change by remember(draft) { mutableStateOf("") }
    var size by rememberSaveable { mutableStateOf(PreviewSize.Wide) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(draft.design.name, style = MaterialTheme.typography.titleLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewSize.entries.forEach { s ->
                    FilterChip(selected = s == size, onClick = { size = s }, label = { Text(s.label) })
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DesignPreview(draft.design, size, theme)
            }
            // The widget's shape: Claude's pick, or any other.
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetShape.entries.forEach { shape ->
                    FilterChip(selected = draft.design.shape == shape, onClick = { onShape(shape) }, label = { Text(shape.label) })
                }
            }
            OutlinedTextField(
                value = change,
                onValueChange = { change = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Change something") },
                placeholder = { Text("Make the clock bigger and the background purple") },
            )
            BusyButton(if (free) "Change it in Claude" else "Change it", busy = busy, enabled = change.isNotBlank()) { onRefine(change) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveAndAdd, enabled = !busy) { Text("Save & add") }
                OutlinedButton(onClick = onSave, enabled = !busy) { Text("Save") }
                TextButton(onClick = onDiscard, enabled = !busy) { Text("Discard") }
            }
        }
    }
}

/** Waiting for Claude's reply from the Claude app or claude.ai. */
@Composable
internal fun ChatCard(
    chat: ChatRequest,
    onPaste: () -> Unit,
    onFix: () -> Unit,
    onReopen: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Finish in Claude", style = MaterialTheme.typography.titleLarge)
            val first = if (chat.openedIn == ClaudeApp.Opened.App) {
                "1. Claude opened with your request. Send it."
            } else {
                "1. claude.ai opened in your browser. Paste the request (it's copied) and send it."
            }
            Text(first)
            Text("2. When Claude replies, copy its whole reply (the copy button under it, or long-press it and tap Copy).")
            Text("3. Come back here and tap Paste design.")
            chat.problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPaste) { Text("Paste design") }
                if (chat.fixable) {
                    OutlinedButton(onClick = onFix) { Text("Ask Claude to fix it") }
                } else {
                    OutlinedButton(onClick = onReopen) { Text("Open Claude again") }
                }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
            if (chat.fixable) {
                Text(
                    "Ask Claude to fix it copies a short note: paste it into the same chat, then copy the new reply.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Ready-made designs: no AI needed. */
@Composable
internal fun SavedCard(
    saved: SavedDesign,
    count: Int,
    onAdd: (com.sidequest.widgetmaker.widget.WidgetSize) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    theme: WidgetTheme,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var choosingSize by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(saved.design.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (count > 1) Text("On $count widgets", style = MaterialTheme.typography.bodySmall)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DesignPreview(saved.design, PreviewSize.Wide, theme)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { choosingSize = true }) { Text("Add to home screen") }
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
            }
        }
    }
    if (choosingSize) {
        val suggested = com.sidequest.widgetmaker.widget.WidgetSize.suggested(saved.design)
        AlertDialog(
            onDismissRequest = { choosingSize = false },
            title = { Text("What size?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("You can still resize it on the home screen afterwards.", style = MaterialTheme.typography.bodySmall)
                    com.sidequest.widgetmaker.widget.WidgetSize.entries.forEach { size ->
                        TextButton(onClick = { choosingSize = false; onAdd(size) }, modifier = Modifier.fillMaxWidth()) {
                            Text(size.label + if (size == suggested) "  (suggested)" else "", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingSize = false }) { Text("Cancel") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${saved.design.name}”?") },
            text = { Text("Widgets showing it will switch to your newest design.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun InfoCard(title: String, body: String, action: Pair<String, () -> Unit>? = null) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            action?.let { (label, onClick) -> TextButton(onClick = onClick) { Text(label) } }
        }
    }
}

@Composable
internal fun BusyButton(label: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.widthIn(min = 140.dp)) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(8.dp))
            Text("Designing…")
        } else {
            Text(label)
        }
    }
}

/**
 * Shows when Exercise & health is on but some data isn't allowed yet: types
 * added in an update (HRV, blood oxygen... for NOOP) need allowing once.
 */
@Composable
internal fun HealthAccessCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val on = remember { com.sidequest.widgetmaker.feeds.Feeds(context).settings.health }
    if (!on) return
    var missing by remember { mutableStateOf(emptySet<String>()) }
    var version by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(version) { missing = com.sidequest.widgetmaker.feeds.Health.missing(context) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        version++
        onPauseOrDispose {}
    }
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract(),
    ) {
        version++
        com.sidequest.widgetmaker.feeds.FeedWorker.refreshSoon(context, setOf("health"))
    }
    if (missing.isEmpty()) return
    val names = missing.mapNotNull { NAMES.entries.firstOrNull { (key, _) -> it.endsWith(key) }?.value }.distinct()
    androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Allow more health data", style = MaterialTheme.typography.titleMedium)
            Text(
                "Health widgets can't read " + (names.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "some of your data") +
                    " yet. NOOP's Charge needs HRV.",
                style = MaterialTheme.typography.bodyMedium,
            )
            androidx.compose.material3.Button(onClick = { ask.launch(com.sidequest.widgetmaker.feeds.Health.permissions) }) { Text("Allow") }
        }
    }
}

private val NAMES = mapOf(
    "HEART_RATE_VARIABILITY" to "HRV",
    "OXYGEN_SATURATION" to "blood oxygen",
    "RESPIRATORY_RATE" to "breathing rate",
    "VO2_MAX" to "VO2 max",
    "RESTING_HEART_RATE" to "resting heart rate",
    "READ_HEART_RATE" to "heart rate",
    "SLEEP" to "sleep",
    "STEPS" to "steps",
    "EXERCISE" to "workouts",
    "ACTIVE_CALORIES_BURNED" to "calories",
    "TOTAL_CALORIES_BURNED" to "calories",
    "IN_BACKGROUND" to "background updates",
)

/** A newer build is out: update now (download, then Android's installer) or later. */
@Composable
fun UpdateCard(release: com.sidequest.widgetmaker.update.Release, ui: UpdateUi, onUpdate: () -> Unit, onLater: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Update available", style = MaterialTheme.typography.titleMedium)
            Text(
                "Version ${release.version ?: "(newer build)"} is ready (you have ${com.sidequest.widgetmaker.update.Updater.currentVersion}). " +
                    "Your widgets and designs stay as they are.",
                style = MaterialTheme.typography.bodyMedium,
            )
            val pct = ui.downloading
            if (pct != null) {
                androidx.compose.material3.LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                Text("Downloading… $pct%", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onUpdate) { Text("Update") }
                    TextButton(onClick = onLater) { Text("Later") }
                }
                ui.note?.takeIf { it.startsWith("Allow") || it.startsWith("The download") }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
