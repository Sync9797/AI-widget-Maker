package com.sidequest.widgetmaker.ui

import kotlinx.coroutines.launch
import com.sidequest.widgetmaker.music.Music
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sidequest.widgetmaker.feeds.FeedSnapshot
import com.sidequest.widgetmaker.data.DesignStore
import com.sidequest.widgetmaker.spec.MaterialSlot
import com.sidequest.widgetmaker.spec.TapKind
import com.sidequest.widgetmaker.feeds.Health
import com.sidequest.widgetmaker.feeds.MonsterQuest
import androidx.health.connect.client.PermissionController
import androidx.compose.runtime.LaunchedEffect
import com.sidequest.widgetmaker.spec.Look
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.widget.OneUiBackdrop
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.widget.AiWidget
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Settings: the Claude key, and the data sources widgets can show. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MakerViewModel, state: MakerState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var apiKey by remember { mutableStateOf(viewModel.apiKey) }
    var form by remember { mutableStateOf(SourcesForm.from(state.sources)) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
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
            Section("Widget style", "Restyles the colors of all your widgets. Layouts stay as designed.") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetTheme.everywhere.forEach { theme ->
                        FilterChip(state.theme == theme, { viewModel.setTheme(theme) }, { Text(theme.label) })
                    }
                }
                Text(
                    when (state.theme) {
                        WidgetTheme.glass -> "Smoky see-through glass with white text. Icons and bars keep their colors."
                        WidgetTheme.material -> "Your phone's colors (wallpaper or One UI color palette, Android 12+), light or dark with your phone."
                        WidgetTheme.original -> "The colors Claude chose for each design."
                        WidgetTheme.oneui -> "Like Samsung One UI: your wallpaper behind each widget, blurred and frosted, with " +
                            "text that turns dark or light to suit it and accents in your color palette."
                        WidgetTheme.ambient -> "Music widgets only: frosted from the album art of what's playing."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (state.theme == WidgetTheme.oneui) OneUiBlurControls(onChanged = viewModel::refreshWidgets)
                if (state.theme == WidgetTheme.material) MaterialSwatches()
            }

            Section(
                "Widget look",
                "Shape and size for all your widgets. Each widget can also pick its own shape in its settings (long-press it).",
            ) { LookControls(state.theme, onChange = viewModel::setLook) }

            Section(
                "Updates",
                "You have version ${com.sidequest.widgetmaker.update.Updater.currentVersion}. New versions come from the app's GitHub page; " +
                    "updating keeps your widgets, designs and settings.",
            ) {
                val context = LocalContext.current
                var onStart by remember { mutableStateOf(com.sidequest.widgetmaker.update.Updater.checkOnStart(context)) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { viewModel.checkForUpdates() }, enabled = !state.update.checking) { Text("Check for updates") }
                    state.update.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)) }
                }
                state.update.available?.let { UpdateCard(it, state.update, viewModel::installUpdate, viewModel::laterUpdate) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Check when the app opens", modifier = Modifier.weight(1f))
                    Switch(checked = onStart, onCheckedChange = {
                        onStart = it
                        com.sidequest.widgetmaker.update.Updater.setCheckOnStart(context, it)
                    })
                }
            }

            Section(
                "Update frequency",
                "How often weather, health, calendar and the rest refresh in the background. More often uses a little " +
                    "more battery. Music and Side Quest update right away anyway.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15 to "15 min", 30 to "30 min", 60 to "1 hour").forEach { (m, label) ->
                        FilterChip(state.sources.refreshMinutes == m, { viewModel.setRefreshMinutes(m) }, { Text(label) })
                    }
                }
            }

            Section(
                "Countdowns",
                "Days until things you're looking forward to, for the Countdown widget. Pick a date, or an event from your calendar.",
            ) { CountdownControls(state.sources.countdowns, viewModel::setCountdowns) }

            Section(
                "When you tap a widget",
                "Headlines, events and calendar entries open themselves. The rest of a widget opens an app " +
                    "for what it shows. Clock, calendar, maps and battery use your phone's defaults unless you pick one.",
            ) { TapChoices() }

            Section(
                "Claude",
                "Optional. Without a key, designs are made free in the Claude app or claude.ai and pasted back. " +
                    "With an Anthropic API key (from console.anthropic.com, paid per use) they're made automatically. " +
                    "Stored only on this phone.",
            ) {
                Secret("API key", apiKey, "sk-ant-…") { apiKey = it }
            }

            Section("Your town", "Used for weather, local news and nearby events. Free, no account needed.") {
                Field("Town or city", form.town, "Austin") { form = form.copy(town = it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(form.fahrenheit, { form = form.copy(fahrenheit = true) }, { Text("°F, miles") })
                    FilterChip(!form.fahrenheit, { form = form.copy(fahrenheit = false) }, { Text("°C, km") })
                }
            }

            Section("Crypto", "Prices from CoinGecko. Free, no account needed.") {
                Field("Coins", form.coins, "BTC, ETH, SOL") { form = form.copy(coins = it) }
                Field("Currency", form.currency, "USD") { form = form.copy(currency = it) }
            }

            Section(
                "Traffic",
                "Your commute time with live traffic, from TomTom. Get a free key at developer.tomtom.com " +
                    "(sign up, then copy the key from your dashboard). Mornings show the trip to work; from noon, the trip home.",
            ) {
                Secret("TomTom key", form.tomtomKey, "") { form = form.copy(tomtomKey = it) }
                Field("Home address", form.home, "123 Main St, Austin TX") { form = form.copy(home = it) }
                Field("Work address", form.work, "500 Congress Ave, Austin TX") { form = form.copy(work = it) }
            }

            Section("Local news", "Headlines from Google News. Free, no account needed. Leave the topic blank for news about your town.") {
                Field("Topic (optional)", form.newsTopic, "Austin food, Texas Longhorns…") { form = form.copy(newsTopic = it) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hide obituaries", style = MaterialTheme.typography.bodyLarge)
                        Text("Leaves death notices and funeral announcements out of news and events.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = form.hideObituaries, onCheckedChange = { form = form.copy(hideObituaries = it) })
                }
                Field("Hide headlines with these words (optional)", form.mutedWords, "election, crash, …") { form = form.copy(mutedWords = it) }
            }

            Section(
                "Your calendar",
                "Your upcoming events (next 7 days) for \"up next\" widgets, from the calendars on this phone " +
                    "(Samsung Calendar, Google Calendar and any you subscribe to). Read on the phone only; nothing is uploaded.",
            ) {
                val askCalendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    form = form.copy(calendar = granted)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show my calendar", modifier = Modifier.weight(1f))
                    Switch(
                        checked = form.calendar,
                        onCheckedChange = { on ->
                            if (on && context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                                askCalendar.launch(Manifest.permission.READ_CALENDAR)
                            } else {
                                form = form.copy(calendar = on)
                            }
                        },
                    )
                }
            }

            Section(
                "Exercise & health",
                "Steps, heart rate, kcal burned, workouts and sleep from Health Connect, where Samsung Health, WHOOP, " +
                    "Google Fit, Fitbit and others share their data (in Samsung Health: Settings → Health Connect). " +
                    "Read on the phone only. WHOOP keeps its strain score to itself, so \"effort\" is estimated from your heart rate.\n\n" +
                    "NOOP (the offline WHOOP app): turn on Health Connect in NOOP's Data Sources screen, and widgets get its HRV, " +
                    "resting heart rate, blood oxygen, breathing rate, VO2 max and sleep (its data comes first when other apps share too). " +
                    "NOOP keeps its Charge score to itself, so \"charge\" here is estimated the same way, from HRV and resting heart rate " +
                    "against your own 30-day baseline (it needs about 5 days of data). Tapping a health widget opens NOOP when it's installed.\n\n" +
                    "not-NOOP (the fork) shares straight with this app instead: its own Charge, Rest and Effort, live heart rate " +
                    "and strap battery, with no Health Connect in between and no estimates. Nothing to set up; just install a not-NOOP " +
                    "build with sharing.",
            ) {
                HealthControls(form.health) { form = form.copy(health = it) }
                Field("Daily step goal", form.stepGoal, "8000", KeyboardType.Number) { form = form.copy(stepGoal = it) }
            }

            Section(
                "Music",
                "A now-playing widget for Spotify, YouTube Music, podcasts or any player: the song, artist, album art and " +
                    "play/pause, next and previous buttons. Android only shares what's playing with apps allowed notification " +
                    "access; this app uses it for the music and nothing else.",
            ) {
                MusicControls()
            }

            Section(
                "Side Quest",
                "Widgets for the game: your eggs and how far to hatch, monsters out on adventures, and your steps and sleep.",
            ) {
                MonsterQuestControls(form.monsterQuest) { form = form.copy(monsterQuest = it) }
            }

            Section(
                "Nearby events",
                "Works without an account: recent local articles about events and things to do in your town. " +
                    "Optional: a free Ticketmaster key (developer.ticketmaster.com, Consumer Key under My Apps) " +
                    "gives exact listings with dates and venues instead.",
            ) {
                Secret("Ticketmaster key (optional)", form.ticketmasterKey, "") { form = form.copy(ticketmasterKey = it) }
                Field("Within (miles, Ticketmaster only)", form.radiusMiles, "25", KeyboardType.Number) { form = form.copy(radiusMiles = it) }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.setApiKey(apiKey)
                        viewModel.saveSources(form)
                    },
                    enabled = !state.sourcesBusy,
                ) {
                    if (state.sourcesBusy) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                    }
                    Text("Save and test")
                }
            }
            state.sourcesStatus?.let {
                Text(it, color = if (it.startsWith("Saved.")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            if (state.snapshot.values.isNotEmpty()) LatestValues(state.snapshot)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** What the widgets will show right now, so you can check each source. */
@Composable
private fun LatestValues(snapshot: FeedSnapshot) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Latest data", style = MaterialTheme.typography.titleMedium)
            snapshot.values.toSortedMap().forEach { (name, value) ->
                Text("$name: $value", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Section(title: String, about: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(about, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    hint: String,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(hint) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}

@Composable
private fun Secret(label: String, value: String, hint: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(hint) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}

/**
 * The colors Material 3 widgets use on this phone right now, read from the
 * same system colors the launcher uses, to compare with the phone's palette.
 */
@Composable
private fun MaterialSwatches() {
    val context = LocalContext.current
    val slots = listOf(
        "Background" to MaterialSlot.Surface,
        "Panel" to MaterialSlot.Container,
        "Text" to MaterialSlot.Text,
        "Accent" to MaterialSlot.Primary,
    )
    Text("Your phone's colors right now:", style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        slots.forEach { (label, slot) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(context.getColor(AiWidget.materialRes(slot))))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** One row per kind of tap, showing which app it opens; tap a row to change it. */
@Composable
private fun TapChoices() {
    val context = LocalContext.current
    val store = remember { DesignStore(context) }
    var version by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<TapKind?>(null) }
    val labels = remember(version) {
        val pm = context.packageManager
        TapKind.entries.associateWith { kind ->
            when (val choice = store.tapApp(kind)) {
                null -> when {
                    kind == TapKind.health && Health.installedNoop(context) != null -> "NOOP"
                    kind == TapKind.music -> "The app that's playing"
                    AppOpener.hasSystemDefault(kind) -> "Phone's default"
                    else -> "Ask every time"
                }
                AppOpener.WEB -> "On the web"
                else -> runCatching { pm.getApplicationLabel(pm.getApplicationInfo(choice, 0)).toString() }.getOrDefault(choice)
            }
        }
    }
    Column {
        TapKind.entries.forEach { kind ->
            Row(
                Modifier.fillMaxWidth().clickable { editing = kind }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(kind.label, modifier = Modifier.weight(1f))
                Text(labels.getValue(kind), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    editing?.let { kind ->
        Dialog(onDismissRequest = { editing = null }) {
            Surface(shape = MaterialTheme.shapes.extraLarge) {
                AppPicker(
                    kind = kind,
                    offerAlways = false,
                    onPicked = { choice, _ ->
                        store.setTapApp(kind, choice)
                        version++
                        editing = null
                    },
                    onCancel = { editing = null },
                    onAskEveryTime = {
                        store.setTapApp(kind, null)
                        version++
                        editing = null
                    },
                )
            }
        }
    }
}

/** One UI blur: where the wallpaper comes from, and how to make the blur real. */
@Composable
private fun OneUiBlurControls(onChanged: () -> Unit) {
    val context = LocalContext.current
    var version by remember { mutableStateOf(0) }
    val source = remember(version) { OneUiBackdrop.source(context) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && OneUiBackdrop.savePicked(context, uri)) {
            version++
            onChanged()
        }
    }
    Text(source.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text(if (source == OneUiBackdrop.Source.Picked) "Pick again" else "Pick my wallpaper picture")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && source != OneUiBackdrop.Source.System) {
            OutlinedButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}")),
                    )
                }
            }) { Text("Allow wallpaper access") }
        }
        if (source == OneUiBackdrop.Source.Picked) {
            TextButton(onClick = { OneUiBackdrop.forgetPicked(context); version++; onChanged() }) { Text("Forget picture") }
        }
    }
    Text(
        "Pick the same picture as your home screen wallpaper (or allow wallpaper access, which reads it directly and " +
            "follows when you change it). Then tap an empty spot on each widget once: it learns where it sits, " +
            "so the blur lines up with what's behind it.",
        style = MaterialTheme.typography.bodySmall,
    )
}

/** Corner radius, edge, size and 1-row ovals, with a live preview. */
@Composable
private fun LookControls(theme: WidgetTheme, onChange: (Look) -> Unit) {
    val context = LocalContext.current
    var look by remember { mutableStateOf(DesignStore(context).look) }
    /** Saves and redraws the home screen widgets. */
    fun update(new: Look) {
        look = new
        onChange(new)
    }
    /** While a slider moves: only the preview follows; it's saved when let go. */
    fun preview(new: Look) {
        look = new
    }
    val save = { onChange(look) }
    val samples = remember { Starters.all }
    val wide = samples.first { it.title == "Battery" }.design
    val square = samples.first { it.title == "Clock and weather" }.design
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        DesignPreview(wide, PreviewSize.Strip, theme, look = look)
        DesignPreview(square, PreviewSize.Square, theme, look = look)
    }

    LookSlider(
        "Corner radius", look.cornerRadius?.let { "$it" } ?: "Design's", (look.cornerRadius ?: 24).toFloat(), 0f..40f, 40, save,
    ) { preview(look.copy(cornerRadius = it.toInt())) }
    if (look.cornerRadius != null) TextButton(onClick = { update(look.copy(cornerRadius = null)) }) { Text("Use each design's radius") }
    LookSlider("Edge", "%.1f".format(look.edge), look.edge, 0f..3f, 12, save) { preview(look.copy(edge = it)) }
    LookSlider("Size", if (look.inset == 0) "Full" else "-${look.inset}", (16 - look.inset).toFloat(), 0f..16f, 16, save) {
        preview(look.copy(inset = 16 - it.toInt()))
    }

    Text("Bigger screens (foldables, tablets)", style = MaterialTheme.typography.bodyLarge)
    Text(
        "When a widget is bigger than a standard 2×2, like on an open foldable, its text and icons can grow with it. " +
            "Your launcher gives each screen its own size, so this follows the fold by itself.",
        style = MaterialTheme.typography.bodySmall,
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.sidequest.widgetmaker.spec.AutoScale.entries.forEach { mode ->
            FilterChip(look.autoScale == mode, { update(look.copy(autoScale = mode)) }, { Text(mode.label) })
        }
    }
    Text(look.autoScale.about, style = MaterialTheme.typography.bodySmall)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("One UI proportions", style = MaterialTheme.typography.bodyLarge)
            Text("2×2 and 4×4 widgets keep the shape of Samsung's own (a touch taller than wide) instead of stretching tall in their cell.", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = look.squareCells, onCheckedChange = { update(look.copy(squareCells = it)) })
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Oval 1-row widgets", style = MaterialTheme.typography.bodyLarge)
            Text("2×1 and 4×1 widgets become pills of this height (it also sets the height of any widget given the Pill shape).", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = look.pillMaxHeight != null, onCheckedChange = { update(look.copy(pillMaxHeight = if (it) 52 else null)) })
    }
    look.pillMaxHeight?.let { maxH ->
        LookSlider("Pill height", "$maxH dp", maxH.toFloat(), 32f..96f, 64, save) { preview(look.copy(pillMaxHeight = it.toInt())) }
    }
}

@Composable
private fun LookSlider(
    label: String,
    value: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onDone: () -> Unit,
    onChange: (Float) -> Unit,
) {
    Column {
        Row {
            Text(label, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
        Slider(value = current, onValueChange = onChange, onValueChangeFinished = onDone, valueRange = range, steps = steps - 1)
    }
}

/** Connects Health Connect: shows where it stands and asks for access. */
@Composable
private fun HealthControls(on: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<Health.Status?>(null) }
    var version by remember { mutableStateOf(0) }
    LaunchedEffect(version) { status = Health.status(context) }
    val ask = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        onChange(granted.isNotEmpty())
        version++
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Show exercise & health", modifier = Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = { want ->
            if (!want) onChange(false)
            else when (status) {
                Health.Status.Ready, Health.Status.Partly -> onChange(true)
                Health.Status.NeedsPermission -> ask.launch(Health.permissions)
                Health.Status.NeedsInstall -> runCatching { context.startActivity(Health.installIntent()) }
                else -> Unit
            }
        })
    }
    Text(
        when (status) {
            Health.Status.Ready -> "Connected to Health Connect."
            Health.Status.Partly -> "Connected, but some data isn't allowed. Tap below to allow the rest."
            Health.Status.NeedsPermission -> "Turn it on to choose what to share from Health Connect."
            Health.Status.NeedsInstall -> "Health Connect needs installing or updating first (turn it on to open the Play Store)."
            Health.Status.Unavailable -> "Health Connect isn't available on this phone."
            null -> "Checking Health Connect…"
        },
        style = MaterialTheme.typography.bodySmall,
    )
    if (status == Health.Status.Partly) TextButton(onClick = { ask.launch(Health.permissions) }) { Text("Allow the rest") }
    // What Health Connect actually shares, to see why a widget shows "--".
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var report by remember { mutableStateOf<List<String>?>(null) }
    if (status == Health.Status.Ready || status == Health.Status.Partly) {
        TextButton(onClick = {
            report = listOf("Checking…")
            scope.launch { report = Health.diagnose(context) }
        }) { Text("Check what Health Connect shares") }
    }
    report?.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
}

/** The list of countdowns, with adding by date or from the calendar. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CountdownControls(list: List<com.sidequest.widgetmaker.feeds.Countdown>, onChange: (List<com.sidequest.widgetmaker.feeds.Countdown>) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    var fromCalendar by remember { mutableStateOf<List<com.sidequest.widgetmaker.feeds.Countdown>?>(null) }
    val today = java.time.LocalDate.now()
    list.forEach { c ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            val days = runCatching { java.time.temporal.ChronoUnit.DAYS.between(today, java.time.LocalDate.parse(c.date)) }.getOrNull()
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.bodyLarge)
                Text("${c.date} · " + when {
                    days == null -> ""
                    days < 0 -> "passed"
                    days == 0L -> "today"
                    else -> "in $days days"
                }, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { onChange(list - c) }) { Text("Remove") }
        }
    }
    OutlinedTextField(name, { name = it }, label = { Text("Name, like \"Beach trip\"") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { picking = true }, enabled = name.isNotBlank()) { Text("Pick date") }
        OutlinedButton(onClick = { fromCalendar = com.sidequest.widgetmaker.feeds.Feeds(context).upcomingEvents(days = 365) }) { Text("From calendar") }
    }
    if (picking) {
        val pickerState = androidx.compose.material3.rememberDatePickerState()
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        // The picker gives midnight UTC of the chosen day.
                        val date = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        onChange(list + com.sidequest.widgetmaker.feeds.Countdown(name.trim(), date.toString()))
                        name = ""
                    }
                    picking = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { androidx.compose.material3.DatePicker(pickerState) }
    }
    fromCalendar?.let { events ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { fromCalendar = null },
            title = { Text("Count down to…") },
            text = {
                if (events.isEmpty()) {
                    Text("No upcoming events found. Turn on Your calendar above (and allow access) first.")
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        events.forEach { e ->
                            Text(
                                "${e.name}  ·  ${e.date}",
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onChange(list + e)
                                    fromCalendar = null
                                }.padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { fromCalendar = null }) { Text("Close") } },
        )
    }
}

/** Shows whether music access is allowed, with the way to allow it. */
@Composable
private fun MusicControls() {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(Music.hasAccess(context)) }
    // Checks again on coming back from Android's settings.
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        allowed = Music.hasAccess(context)
        onPauseOrDispose {}
    }
    Text(
        if (allowed) "Allowed: music widgets show what's playing." else "Not allowed yet. Play/pause still works, but the widget can't show the song.",
        style = MaterialTheme.typography.bodySmall,
    )
    if (!allowed) {
        TextButton(onClick = { runCatching { context.startActivity(Music.accessIntent(context)) } }) { Text("Allow music access") }
        Text(
            "If Android says it's a restricted setting (apps not from the Play Store, Android 13+): open App info below, " +
                "tap ⋮ at the top, choose \"Allow restricted settings\", then come back and try again.",
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = { runCatching { context.startActivity(Music.appInfoIntent(context)) } }) { Text("Open App info") }
    }
}

/** Shows whether Side Quest is there and sharing. */
@Composable
private fun MonsterQuestControls(on: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val status = remember { MonsterQuest.status(context) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Show Side Quest", modifier = Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = onChange)
    }
    Text(
        when (status) {
            MonsterQuest.Status.Ready -> "Found Side Quest. Its widgets update whenever the game changes."
            MonsterQuest.Status.NeedsUpdate -> "Update Side Quest to the latest version so it can share with widgets."
            MonsterQuest.Status.NotInstalled -> "Side Quest isn't installed."
        },
        style = MaterialTheme.typography.bodySmall,
    )
}
