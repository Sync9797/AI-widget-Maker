package com.sidequest.widgetmaker.ui

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sidequest.widgetmaker.ai.WidgetDesigner
import com.sidequest.widgetmaker.data.DesignStore
import com.sidequest.widgetmaker.feeds.FeedSettings
import com.sidequest.widgetmaker.feeds.FeedSnapshot
import com.sidequest.widgetmaker.feeds.FeedWorker
import com.sidequest.widgetmaker.feeds.Feeds
import com.sidequest.widgetmaker.feeds.Place
import com.sidequest.widgetmaker.spec.SavedDesign
import com.sidequest.widgetmaker.spec.WidgetDesign
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.widget.AiWidget
import com.sidequest.widgetmaker.widget.AiWidgetReceiver
import com.sidequest.widgetmaker.widget.PinnedReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class Draft(
    /** Set when editing a saved design, so saving replaces it. */
    val id: String?,
    val prompt: String,
    val design: WidgetDesign,
)

data class MakerState(
    val draft: Draft? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val saved: List<SavedDesign> = emptyList(),
    val hasApiKey: Boolean = false,
    val theme: WidgetTheme = WidgetTheme.glass,
    val sources: FeedSettings = FeedSettings(),
    val snapshot: FeedSnapshot = FeedSnapshot(),
    val sourcesBusy: Boolean = false,
    val sourcesStatus: String? = null,
)

/** What the Data sources screen edits: plain text, looked up when saved. */
data class SourcesForm(
    val town: String,
    val fahrenheit: Boolean,
    val coins: String,
    val currency: String,
    val tomtomKey: String,
    val home: String,
    val work: String,
    val newsTopic: String,
    val ticketmasterKey: String,
    val radiusMiles: String,
) {
    companion object {
        fun from(s: FeedSettings) = SourcesForm(
            town = s.place?.name.orEmpty(),
            fahrenheit = s.fahrenheit,
            coins = s.coins.joinToString(", "),
            currency = s.currency.uppercase(),
            tomtomKey = s.tomtomKey,
            home = s.home?.name.orEmpty(),
            work = s.work?.name.orEmpty(),
            newsTopic = s.newsTopic,
            ticketmasterKey = s.ticketmasterKey,
            radiusMiles = s.eventsRadiusMiles.toString(),
        )
    }
}

class MakerViewModel(app: Application) : AndroidViewModel(app) {
    private val store = DesignStore(app)
    private val feeds = Feeds(app)
    private val _state = MutableStateFlow(MakerState())
    val state: StateFlow<MakerState> = _state

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { store.all() }
            val sources = feeds.settings
            val snapshot = withContext(Dispatchers.IO) { feeds.snapshot() }
            _state.update {
                it.copy(
                    saved = saved, hasApiKey = store.apiKey.isNotBlank(), theme = store.theme,
                    sources = sources, snapshot = snapshot,
                )
            }
        }
    }

    val apiKey: String get() = store.apiKey

    /** Switches every widget's theme right away. */
    fun setTheme(theme: WidgetTheme) {
        store.theme = theme
        _state.update { it.copy(theme = theme) }
        viewModelScope.launch { AiWidget.refreshAll(getApplication()) }
    }

    fun setApiKey(key: String) {
        store.apiKey = key
        _state.update { it.copy(hasApiKey = key.isNotBlank(), error = null) }
    }

    /** A new design from a description. */
    fun generate(description: String) = run(description, previous = null)

    /** Changes the current draft as described. */
    fun refine(change: String) {
        val draft = _state.value.draft ?: return
        run(change, previous = draft)
    }

    private fun run(text: String, previous: Draft?) {
        if (text.isBlank() || _state.value.busy) return
        val key = store.apiKey
        if (key.isBlank()) {
            _state.update { it.copy(error = "Add your Anthropic API key in Settings first.") }
            return
        }
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val design = WidgetDesigner(key).design(text, previous?.design, feeds.settings.summary())
                val prompt = if (previous == null) text else previous.prompt + "\n→ " + text
                _state.update { it.copy(busy = false, draft = Draft(previous?.id, prompt, design)) }
            } catch (e: WidgetDesigner.DesignFailed) {
                _state.update { it.copy(busy = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = "Something went wrong: ${e.message}") }
            }
        }
    }

    fun edit(saved: SavedDesign) {
        _state.update { it.copy(draft = Draft(saved.id, saved.prompt, saved.design), error = null, message = null) }
    }

    fun discardDraft() = _state.update { it.copy(draft = null, error = null) }

    fun dismissMessage() = _state.update { it.copy(message = null, error = null) }

    /** Saves the draft (replacing the design it was opened from) and redraws placed widgets. */
    fun saveDraft(thenPin: Boolean) {
        val draft = _state.value.draft ?: return
        val saved = SavedDesign(
            id = draft.id ?: UUID.randomUUID().toString(),
            prompt = draft.prompt,
            design = draft.design,
            createdAt = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.save(saved) }
            AiWidget.refreshAll(getApplication())
            _state.update { it.copy(draft = null, message = "Saved “${saved.design.name}”.") }
            reload()
            if (thenPin) pin(saved)
        }
    }

    fun delete(saved: SavedDesign) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.delete(saved.id) }
            AiWidget.refreshAll(getApplication())
            reload()
        }
    }

    /** Asks the launcher to add a widget showing [saved] (Android 8+, most launchers including Samsung's). */
    fun pin(saved: SavedDesign) {
        val app = getApplication<Application>()
        val manager = AppWidgetManager.getInstance(app)
        if (!manager.isRequestPinAppWidgetSupported) {
            _state.update {
                it.copy(message = "Your launcher can't add widgets from apps. Long-press the home screen, tap Widgets, and find AI Widget Maker.")
            }
            return
        }
        val callback = PendingIntent.getBroadcast(
            app,
            saved.id.hashCode(),
            Intent(app, PinnedReceiver::class.java).putExtra(PinnedReceiver.EXTRA_DESIGN_ID, saved.id),
            // Mutable: the launcher adds the new widget's id to this intent.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        manager.requestPinAppWidget(ComponentName(app, AiWidgetReceiver::class.java), null, callback)
    }

    /** Called when the app comes to the front: fresh data, then redraw widgets. */
    fun refreshWidgets() {
        viewModelScope.launch {
            val snapshot = feeds.snapshot()
            // Fetch now if the data is older than 15 minutes; the background job covers the rest.
            if (System.currentTimeMillis() - snapshot.updatedAt > 15 * 60_000) {
                runCatching { feeds.refresh() }.onSuccess { fresh -> _state.update { it.copy(snapshot = fresh) } }
            }
            AiWidget.refreshAll(getApplication())
        }
    }

    /** Looks up the places, saves the data sources, then fetches everything once to check it works. */
    fun saveSources(form: SourcesForm) {
        if (_state.value.sourcesBusy) return
        _state.update { it.copy(sourcesBusy = true, sourcesStatus = "Saving…") }
        viewModelScope.launch {
            val old = feeds.settings
            val problems = mutableListOf<String>()
            suspend fun lookup(text: String, current: Place?, label: String, find: suspend (String) -> Place?): Place? {
                if (text.isBlank()) return null
                if (current != null && current.name == text.trim()) return current
                return runCatching { find(text) }.getOrNull()
                    ?: run { problems += "Couldn't find $label “${text.trim()}”."; current }
            }
            val place = lookup(form.town, old.place, "the town") { feeds.findTown(it) }
            val key = form.tomtomKey.trim()
            val home = if (key.isBlank()) null else lookup(form.home, old.home, "the home address") { feeds.findAddress(it, key) }
            val work = if (key.isBlank()) null else lookup(form.work, old.work, "the work address") { feeds.findAddress(it, key) }
            val settings = FeedSettings(
                place = place,
                fahrenheit = form.fahrenheit,
                coins = form.coins.split(',', ' ', ';').map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct().take(10),
                currency = form.currency.trim().lowercase().ifBlank { "usd" },
                tomtomKey = key,
                home = home ?: old.home.takeIf { form.home.isNotBlank() },
                work = work ?: old.work.takeIf { form.work.isNotBlank() },
                newsTopic = form.newsTopic.trim(),
                ticketmasterKey = form.ticketmasterKey.trim(),
                eventsRadiusMiles = form.radiusMiles.trim().toIntOrNull()?.coerceIn(1, 200) ?: 25,
            )
            feeds.settings = settings
            FeedWorker.schedule(getApplication())
            val snapshot = runCatching { feeds.refresh() }.getOrElse { FeedSnapshot(errors = mapOf("all" to (it.message ?: "failed"))) }
            AiWidget.refreshAll(getApplication())
            problems += snapshot.errors.map { (feed, why) -> "${feed.replaceFirstChar { it.uppercase() }}: $why" }
            _state.update {
                it.copy(
                    sources = settings,
                    snapshot = snapshot,
                    sourcesBusy = false,
                    sourcesStatus = if (problems.isEmpty()) "Saved. Everything is working." else problems.joinToString("\n"),
                )
            }
        }
    }

    fun clearSourcesStatus() = _state.update { it.copy(sourcesStatus = null) }
}
