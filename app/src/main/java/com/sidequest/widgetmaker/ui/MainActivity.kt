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
        FeedWorker.schedule(this)
        openFrom(intent)
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
        viewModel.refreshWidgets()
    }
}

@Composable
fun MakerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MakerScreen(viewModel: MakerViewModel) {
    val state by viewModel.state.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(viewModel, state, onBack = {
            viewModel.clearSourcesStatus()
            showSettings = false
        })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Widget Maker") },
                actions = { TextButton(onClick = { showSettings = true }) { Text("Settings") } },
            )
        },
    ) { padding ->
        // An open foldable or a tablet gets two panes: making widgets on one side, yours on the other
        // (split down the middle, where a book-style fold's hinge is).
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().padding(padding).imePadding()) {
            val twoPanes = maxWidth >= 600.dp
            if (twoPanes) {
                Row(Modifier.fillMaxSize()) {
                    Pane(Modifier.weight(1f)) { CreatePane(state, viewModel, columns = 3) }
                    androidx.compose.material3.VerticalDivider()
                    Pane(Modifier.weight(1f)) { WidgetsPane(state, viewModel, startOpen = true) }
                }
            } else {
                Pane(Modifier.fillMaxSize()) {
                    CreatePane(state, viewModel, columns = 2)
                    WidgetsPane(state, viewModel, startOpen = false)
                }
            }
        }
    }
}

/** One scrolling column of the main screen. */
@Composable
private fun Pane(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

/** Describing a widget, the draft being made, and the ready-made designs by section. */
@Composable
private fun CreatePane(state: MakerState, viewModel: MakerViewModel, columns: Int) {
    HealthAccessCard()
    val draft = state.draft
    val chat = state.chat
    if (chat != null) {
        ChatCard(
            chat = chat,
            onPaste = viewModel::pasteChatReply,
            onFix = viewModel::askClaudeToFix,
            onReopen = viewModel::reopenChat,
            onCancel = viewModel::cancelChat,
        )
    } else if (draft == null) {
        DescribeCard(busy = state.busy, free = !state.hasApiKey, onGenerate = viewModel::generate)
        MyDesigns(state.saved.filter { it.madeByYou }, state.theme, columns, onPick = viewModel::useAgain)
        StarterSections(theme = state.theme, columns = columns, onPick = viewModel::useStarter)
    } else {
        DraftCard(
            draft = draft,
            busy = state.busy,
            onRefine = viewModel::refine,
            onSave = { viewModel.saveDraft(thenPin = false) },
            onSaveAndAdd = { viewModel.saveDraft(thenPin = true) },
            onDiscard = viewModel::discardDraft,
            theme = state.theme,
            free = !state.hasApiKey,
            onShape = viewModel::setDraftShape,
        )
    }
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
}

/** Your widgets: the ones on the home screen, and saved designs that aren't, each in a fold-away list. */
@Composable
private fun WidgetsPane(state: MakerState, viewModel: MakerViewModel, startOpen: Boolean) {
    val active = state.saved.filter { it.id in state.placed }
    val others = state.saved.filter { it.id !in state.placed }
    Collapsible("Active widgets", "${active.size} on your home screen", startOpen) {
        if (active.isEmpty()) {
            Text(
                "None yet. Save a design and tap Add to home screen, or long-press your home screen → Widgets → AI Widget Maker. " +
                    "Widgets you remove from the home screen leave this list by themselves.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        active.forEach { saved ->
            SavedCard(
                saved = saved,
                count = state.placed[saved.id] ?: 0,
                onAdd = { size -> viewModel.pin(saved, size) },
                onEdit = { viewModel.edit(saved) },
                onDelete = { viewModel.delete(saved) },
                theme = state.theme,
            )
        }
    }
    if (others.isNotEmpty()) {
        Collapsible("Saved designs", "${others.size} not on your home screen", false) {
            others.forEach { saved ->
                SavedCard(
                    saved = saved,
                    count = 0,
                    onAdd = { size -> viewModel.pin(saved, size) },
                    onEdit = { viewModel.edit(saved) },
                    onDelete = { viewModel.delete(saved) },
                    theme = state.theme,
                )
            }
        }
    }
    InfoCard(
        title = "On your lock screen",
        body = "The widget also offers itself to lock screens. On a Samsung phone: long-press " +
            "the lock screen, tap Widgets, and look for AI Widget Maker. If it isn't listed, " +
            "your One UI version only allows Samsung's own lock screen widgets for now; " +
            "it will still work on your home screen.",
    )
    Spacer(Modifier.height(24.dp))
}

/** A titled card that opens and closes. */
@Composable
private fun Collapsible(title: String, summary: String, startOpen: Boolean, content: @Composable () -> Unit) {
    var open by rememberSaveable(title) { mutableStateOf(startOpen) }
    Card(Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(summary, style = MaterialTheme.typography.bodySmall)
                }
                Text(if (open) "▲" else "▼", style = MaterialTheme.typography.bodyMedium)
            }
            androidx.compose.animation.AnimatedVisibility(open) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content()
                }
            }
        }
    }
}

/** The designs you made with Claude, as a gallery to start new widgets from (kept even when not on the home screen). */
@Composable
private fun MyDesigns(mine: List<SavedDesign>, theme: WidgetTheme, columns: Int, onPick: (SavedDesign) -> Unit) {
    Collapsible(
        "My designs",
        if (mine.isEmpty()) "Widgets you make with Claude are kept here" else "${mine.size} made by you · tap one to make another from it",
        startOpen = mine.isNotEmpty(),
    ) {
        if (mine.isEmpty()) {
            Text(
                "Describe a widget above (or change a ready-made one with Claude) and save it: it's kept here to use again, " +
                    "even after you take it off your home screen.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        mine.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { saved ->
                    Card(
                        Modifier.weight(1f).clickable { onPick(saved) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DesignPreview(saved.design, PreviewSize.Square, theme, Modifier.fillMaxWidth())
                            Text(saved.design.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text(saved.prompt, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Ready-made designs, grouped (Weather, Health...): tap a group to open its grid. */
@Composable
private fun StarterSections(theme: WidgetTheme, columns: Int, onPick: (Starters.Starter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Or start from one of these", style = MaterialTheme.typography.titleMedium)
        Starters.all.groupBy { it.category }.toSortedMap(compareBy { it.ordinal }).forEach { (category, starters) ->
            Collapsible(category.label, "${starters.size} " + if (starters.size == 1) "design" else "designs", false) {
                starters.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { starter ->
                            Card(
                                Modifier.weight(1f).clickable { onPick(starter) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                            ) {
                                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DesignPreview(starter.design, PreviewSize.Square, theme, Modifier.fillMaxWidth())
                                    Text(starter.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(starter.about, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                }
                            }
                        }
                        // Keep the last row's cards the same width as the others.
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DescribeCard(busy: Boolean, free: Boolean, onGenerate: (String) -> Unit) {
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
private fun DraftCard(
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
private fun ChatCard(
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
private fun SavedCard(
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
private fun InfoCard(title: String, body: String, action: Pair<String, () -> Unit>? = null) {
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
private fun BusyButton(label: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
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
private fun HealthAccessCard() {
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
