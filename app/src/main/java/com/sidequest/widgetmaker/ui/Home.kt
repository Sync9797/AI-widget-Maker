package com.sidequest.widgetmaker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidequest.widgetmaker.spec.SavedDesign
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.spec.WidgetTheme

/** The app's sections, in the bottom bar (phones) or the side rail (tablets, open foldables). */
enum class Page(val label: String, val title: String, val icon: ImageVector) {
    Create("Create", "AI Creator", Icons.Filled.Create),
    Widgets("Widgets", "Widgets", Icons.Filled.Home),
    Active("Active", "Active widgets", Icons.Filled.CheckCircle),
    Mine("Mine", "Your designs", Icons.Filled.Favorite),
    Settings("Settings", "Settings", Icons.Filled.Settings),
}

/** Screens at least this wide get the side rail and two columns. */
private val WIDE = 600.dp

@Composable
fun MakerScreen(viewModel: MakerViewModel, start: Page = Page.Create) {
    val state by viewModel.state.collectAsState()
    var section by rememberSaveable { mutableStateOf(start) }
    fun go(to: Page) {
        if (section == Page.Settings && to != Page.Settings) viewModel.clearSourcesStatus()
        section = to
    }
    // Back from any other section returns to the creator first.
    BackHandler(enabled = section != Page.Create) { go(Page.Create) }
    // Starting from a ready-made design, editing one or using one again opens it in the creator.
    val making = state.draft != null || state.chat != null
    LaunchedEffect(making) { if (making) section = Page.Create }

    // A Surface, so text everywhere takes the theme's color (the wide layout has no Scaffold to set it).
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WIDE
        val columns = if (wide) 2 else 1
        if (wide) {
            // Like not-NOOP on a big screen: the content, with a floating rail at the right edge.
            Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                Box(Modifier.weight(1f).fillMaxHeight()) { SectionContent(section, state, viewModel, columns, ::go) }
                Rail(section, ::go, Modifier.align(Alignment.CenterVertically).padding(end = 16.dp, start = 8.dp))
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = { BottomBar(section, ::go) },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding).imePadding()) { SectionContent(section, state, viewModel, columns, ::go) }
            }
        }
    }
    }
}

@Composable
private fun BottomBar(current: Page, onGo: (Page) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Page.entries.forEach { s ->
            NavigationBarItem(
                selected = s == current,
                onClick = { onGo(s) },
                icon = { Icon(s.icon, contentDescription = null) },
                label = { Text(s.label, maxLines = 1) },
            )
        }
    }
}

/** A floating pill of sections, at the right edge of a big screen. */
@Composable
private fun Rail(current: Page, onGo: (Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(88.dp),
        shape = RoundedCornerShape(44.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            Modifier.padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Page.entries.forEach { s ->
                NavigationRailItem(
                    selected = s == current,
                    onClick = { onGo(s) },
                    icon = { Icon(s.icon, contentDescription = null) },
                    label = { Text(s.label, maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun SectionContent(section: Page, state: MakerState, viewModel: MakerViewModel, columns: Int, onGo: (Page) -> Unit) {
    when (section) {
        Page.Create -> Sheet(section, columns) { CreateItems(state, viewModel) }
        Page.Widgets -> Sheet(section, columns) { WidgetsItems(state, viewModel) }
        Page.Active -> Sheet(section, columns) { ActiveItems(state, viewModel, onGo) }
        Page.Mine -> Sheet(section, columns) { MineItems(state, viewModel, onGo) }
        Page.Settings -> SettingsContent(viewModel, state, columns, header = { SectionHeader(Page.Settings) })
    }
}

/**
 * A section's page: its header across the top, then its cards in [columns] columns, each column
 * filling up independently (so a tall card doesn't leave a gap beside it).
 */
@Composable
private fun Sheet(section: Page, columns: Int, items: LazyStaggeredGridScope.() -> Unit) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalItemSpacing = 14.dp,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header", span = StaggeredGridItemSpan.FullLine) { SectionHeader(section) }
        items()
    }
}

/** A section's title, big and bold. */
@Composable
fun SectionHeader(section: Page) {
    Text(
        section.title,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp, start = 4.dp),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
    )
}

private fun LazyStaggeredGridScope.full(key: String, content: @Composable () -> Unit) =
    item(key = key, span = StaggeredGridItemSpan.FullLine) { content() }

/** Describing a widget, or the draft being made; then ideas to start from. */
private fun LazyStaggeredGridScope.CreateItems(state: MakerState, viewModel: MakerViewModel) {
    full("crash") { CrashCard() }
    state.update.available?.let { release ->
        full("update") { UpdateCard(release, state.update, viewModel::installUpdate, viewModel::laterUpdate) }
    }
    full("health") { HealthAccessCard() }
    val draft = state.draft
    val chat = state.chat
    when {
        chat != null -> full("chat") {
            ChatCard(chat, viewModel::pasteChatReply, viewModel::askClaudeToFix, viewModel::reopenChat, viewModel::cancelChat)
        }
        draft != null -> full("draft") {
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
        else -> {
            item(key = "describe") { DescribeCard(busy = state.busy, free = !state.hasApiKey, onGenerate = viewModel::generate) }
            item(key = "how") {
                InfoCard(
                    "How it works",
                    "Describe a widget in your own words and Claude designs it, live data and all: weather, your calendar, " +
                        "music, health, Side Quest and more. Preview it at every size, change anything by asking, then " +
                        "add it to your home screen. Ready-made designs are under Widgets.",
                )
            }
        }
    }
    state.error?.let { full("error") { Text(it, color = MaterialTheme.colorScheme.error) } }
    state.message?.let { full("message") { Text(it, color = MaterialTheme.colorScheme.primary) } }
}

/** The ready-made designs: a search, then one card per category. */
private fun LazyStaggeredGridScope.WidgetsItems(state: MakerState, viewModel: MakerViewModel) {
    full("search") { SearchBox() }
    val query = searchQuery.value.trim()
    val groups = Starters.all
        .filter { query.isEmpty() || it.title.contains(query, true) || it.about.contains(query, true) || it.category.label.contains(query, true) }
        .groupBy { it.category }
        .toSortedMap(compareBy { it.ordinal })
    if (groups.isEmpty()) full("none") { Text("No designs match “$query”.", style = MaterialTheme.typography.bodyMedium) }
    groups.forEach { (category, starters) ->
        item(key = "cat_${category.name}") {
            CategoryCard(category.label, starters.size) {
                PreviewGrid(starters, state.theme, title = { it.title }, about = { it.about }, design = { it.design }, onPick = viewModel::useStarter)
            }
        }
    }
}

/** The widgets on your home screen, each with its actions. */
private fun LazyStaggeredGridScope.ActiveItems(state: MakerState, viewModel: MakerViewModel, onGo: (Page) -> Unit) {
    // One card per design, even if a save went in twice (the same key twice would crash the list).
    val active = state.saved.distinctBy { it.id }.filter { it.id in state.placed }
    if (active.isEmpty()) {
        full("empty") {
            InfoCard(
                "Nothing on your home screen yet",
                "Make a widget or pick a ready-made one, then tap Add to home screen. Widgets you remove from the " +
                    "home screen leave this list by themselves.",
                "Browse widgets" to { onGo(Page.Widgets) },
            )
        }
    }
    active.forEach { saved ->
        item(key = "a_${saved.id}") { SavedItem(saved, state.placed[saved.id] ?: 0, state.theme, viewModel) }
    }
    full("lock") {
        InfoCard(
            "On your lock screen",
            "Widgets also offer themselves to lock screens. On a Samsung phone: long-press the lock screen, tap Widgets " +
                "and look for AI Widget Maker. If it isn't listed, your One UI version only allows Samsung's own " +
                "lock screen widgets for now.",
        )
    }
}

/** Designs you made, and saved ones that aren't on your home screen. */
private fun LazyStaggeredGridScope.MineItems(state: MakerState, viewModel: MakerViewModel, onGo: (Page) -> Unit) {
    val saved = state.saved.distinctBy { it.id }
    val made = saved.filter { it.madeByYou }
    val others = saved.filter { !it.madeByYou && it.id !in state.placed }
    if (made.isEmpty() && others.isEmpty()) {
        full("empty") {
            InfoCard(
                "Your designs live here",
                "Widgets you make with Claude are kept here to use again, even after you take them off your home screen.",
                "Make one" to { onGo(Page.Create) },
            )
        }
    }
    if (made.isNotEmpty()) {
        item(key = "made") {
            CategoryCard("Made by you", made.size, hint = "Tap one to make another from it") {
                PreviewGrid(made, state.theme, title = { it.design.name }, about = { it.prompt }, design = { it.design }, onPick = viewModel::useAgain)
            }
        }
    }
    others.forEach { saved ->
        item(key = "m_${saved.id}") { SavedItem(saved, 0, state.theme, viewModel) }
    }
}

/** After a crash: what went wrong, to copy and send (it stays on the phone otherwise). */
@Composable
private fun CrashCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var details by androidx.compose.runtime.remember { mutableStateOf(CrashLog.last(context)) }
    val text = details ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("The app closed unexpectedly last time", style = MaterialTheme.typography.titleMedium)
            Text(
                "Copy the details and send them along, and it can be fixed. Nothing was sent anywhere.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(text.lineSequence().take(3).joinToString("\n"), style = MaterialTheme.typography.bodySmall, maxLines = 3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Button(onClick = {
                    val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                    clip?.setPrimaryClip(android.content.ClipData.newPlainText("AI Widget Maker crash", text))
                    android.widget.Toast.makeText(context, "Crash details copied", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Copy crash details") }
                androidx.compose.material3.TextButton(onClick = { CrashLog.clear(context); details = null }) { Text("Dismiss") }
            }
        }
    }
}

@Composable
private fun SavedItem(saved: SavedDesign, count: Int, theme: WidgetTheme, viewModel: MakerViewModel) {
    SavedCard(
        saved = saved,
        count = count,
        onAdd = { size -> viewModel.pin(saved, size) },
        onEdit = { viewModel.edit(saved) },
        onDelete = { viewModel.delete(saved) },
        theme = theme,
    )
}

/** What the widget gallery is filtered by; kept across the section's recompositions. */
private val searchQuery = mutableStateOf("")

@Composable
private fun SearchBox() {
    var text by searchQuery
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search widgets") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(32.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

/**
 * A category of designs: a deep card with the category's name in big letters, echoing down in fading
 * copies, over its designs.
 */
@Composable
private fun CategoryCard(name: String, count: Int, hint: String? = null, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                EchoTitle(name, Modifier.weight(1f))
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

/** A title in bold capitals, with fading copies stepping down behind it. */
@Composable
private fun EchoTitle(text: String, modifier: Modifier = Modifier, copies: Int = 3, step: Dp = 13.dp) {
    val label = text.uppercase()
    val style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Box(modifier.height(40.dp + step * copies)) {
        for (i in copies downTo 1) {
            Text(
                label,
                style = style,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f / i),
                modifier = Modifier.offset(y = step * i),
            )
        }
        Text(label, style = style, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Designs as previews in two columns, each with its name and what it shows. */
@Composable
private fun <T> PreviewGrid(
    items: List<T>,
    theme: WidgetTheme,
    title: (T) -> String,
    about: (T) -> String,
    design: (T) -> com.sidequest.widgetmaker.spec.WidgetDesign,
    onPick: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { onPick(item) }
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainer),
                                ),
                                RoundedCornerShape(24.dp),
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            DesignPreview(design(item), PreviewSize.Square, theme)
                        }
                        Spacer(Modifier.size(2.dp))
                        Text(title(item), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(about(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Lays its children out in [columns] columns of [spacing], each child going into the shortest
 * column so far (a masonry). One column is a plain stack.
 */
@Composable
fun Masonry(columns: Int, spacing: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val colWidth = ((constraints.maxWidth - gap * (columns - 1)) / columns).coerceAtLeast(0)
        val heights = IntArray(columns)
        val placed = measurables.map { m ->
            val p = m.measure(constraints.copy(minWidth = colWidth, maxWidth = colWidth, minHeight = 0))
            val col = heights.indices.minBy { heights[it] }
            val y = heights[col]
            heights[col] += p.height + gap
            Triple(p, col * (colWidth + gap), y)
        }
        val height = ((heights.maxOrNull() ?: 0) - gap).coerceAtLeast(0)
        layout(constraints.maxWidth, height) {
            placed.forEach { (p, x, y) -> p.placeRelative(x, y) }
        }
    }
}
