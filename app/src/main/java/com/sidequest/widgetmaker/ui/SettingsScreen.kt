package com.sidequest.widgetmaker.ui

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetTheme.entries.forEach { theme ->
                        FilterChip(state.theme == theme, { viewModel.setTheme(theme) }, { Text(theme.label) })
                    }
                }
                Text(
                    when (state.theme) {
                        WidgetTheme.glass -> "Smoky see-through glass with white text. Icons and bars keep their colors."
                        WidgetTheme.material -> "Your phone's colors (wallpaper or One UI color palette, Android 12+), light or dark with your phone."
                        WidgetTheme.original -> "The colors Claude chose for each design."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (state.theme == WidgetTheme.material) MaterialSwatches()
            }

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
                null -> if (AppOpener.hasSystemDefault(kind)) "Phone's default" else "Ask every time"
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
