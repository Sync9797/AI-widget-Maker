package com.sidequest.widgetmaker.ui

import androidx.activity.compose.BackHandler
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

/** Settings: the Claude key, and the data sources widgets can show. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MakerViewModel, state: MakerState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var apiKey by remember { mutableStateOf(viewModel.apiKey) }
    var form by remember { mutableStateOf(SourcesForm.from(state.sources)) }

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
            Section("Claude", "Your Anthropic API key, from console.anthropic.com. Stored only on this phone.") {
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
                "Nearby events",
                "Concerts, games and shows near your town, from Ticketmaster. Get a free key at " +
                    "developer.ticketmaster.com (sign up, then copy the Consumer Key from My Apps).",
            ) {
                Secret("Ticketmaster key", form.ticketmasterKey, "") { form = form.copy(ticketmasterKey = it) }
                Field("Within (miles)", form.radiusMiles, "25", KeyboardType.Number) { form = form.copy(radiusMiles = it) }
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
