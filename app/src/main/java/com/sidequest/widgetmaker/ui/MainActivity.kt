package com.sidequest.widgetmaker.ui

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

class MainActivity : ComponentActivity() {
    private val viewModel: MakerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FeedWorker.schedule(this)
        setContent { MakerTheme { MakerScreen(viewModel) } }
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
                StartersRow(theme = state.theme, onPick = viewModel::useStarter)
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
                )
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            if (state.saved.isNotEmpty()) {
                Text("Your widgets", style = MaterialTheme.typography.titleLarge)
                state.saved.forEach { saved ->
                    SavedCard(
                        saved = saved,
                        onAdd = { viewModel.pin(saved) },
                        onEdit = { viewModel.edit(saved) },
                        onDelete = { viewModel.delete(saved) },
                        theme = state.theme,
                    )
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
private fun StartersRow(theme: WidgetTheme, onPick: (Starters.Starter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Or start from one of these", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Starters.all.forEach { starter ->
                Card(Modifier.width(184.dp).clickable { onPick(starter) }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DesignPreview(starter.design, PreviewSize.Square, theme)
                        Text(starter.title, style = MaterialTheme.typography.titleSmall)
                        Text(starter.about, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedCard(
    saved: SavedDesign,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    theme: WidgetTheme,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(saved.design.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DesignPreview(saved.design, PreviewSize.Wide, theme)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd) { Text("Add to home screen") }
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
            }
        }
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
