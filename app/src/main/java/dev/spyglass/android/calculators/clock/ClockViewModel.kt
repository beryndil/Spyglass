package dev.spyglass.android.calculators.clock

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EventDisplay(
    val name: String,
    val countdown: String,
    val category: String,
    val color: String,
    val ticksAway: Long,
)

data class ClockState(
    val synced: Boolean = false,
    val syncMethod: String = "",
    val syncTick: Long = 0,
    val syncTimeMs: Long = 0,
    val currentTick: Long = 0,
    val timeString: String = "--:--",
    val dayProgress: Float = 0f,
    val events: List<EventDisplay> = emptyList(),
    val activeEvents: List<ClockEngine.GameEvent> = emptyList(),
    val dayOffset: Long = 0,
    val displayedDay: Long = 0,
)

class ClockViewModel(app: Application) : AndroidViewModel(app) {
    private val store = app.dataStore

    private val _state = MutableStateFlow(ClockState())
    val state: StateFlow<ClockState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.data.first().let { prefs ->
                // Load active events
                val eventsJson = prefs[PreferenceKeys.CLOCK_ACTIVE_EVENTS]
                val activeEvents = if (eventsJson != null) {
                    ClockEngine.deserializeEvents(eventsJson).sortedBy { it.tick }
                } else {
                    // Default set on first launch
                    ClockEngine.PREDEFINED_EVENTS
                        .filter { it.predefinedId in ClockEngine.DEFAULT_EVENT_IDS }
                        .sortedBy { it.tick }
                }

                val dayOffset = prefs[PreferenceKeys.CLOCK_DAY_OFFSET] ?: 0L
                _state.update { it.copy(activeEvents = activeEvents, dayOffset = dayOffset) }

                // Restore sync
                val tick = prefs[PreferenceKeys.CLOCK_TICK_OFFSET] ?: -1L
                val timeMs = prefs[PreferenceKeys.CLOCK_SYNC_TIME_MS] ?: 0L
                val method = prefs[PreferenceKeys.CLOCK_SYNC_METHOD] ?: ""
                if (tick >= 0 && timeMs > 0) {
                    _state.update {
                        it.copy(synced = true, syncMethod = method, syncTick = tick, syncTimeMs = timeMs)
                    }
                    startTicking()
                }
            }
        }
    }

    fun syncFromF3(tickInput: String) {
        val tick = tickInput.trim().toLongOrNull() ?: return
        if (tick < 0 || tick >= ClockEngine.TICKS_PER_DAY) return
        val now = System.currentTimeMillis()
        _state.update {
            it.copy(synced = true, syncMethod = "f3", syncTick = tick, syncTimeMs = now)
        }
        persistSync(tick, now, "f3")
        startTicking()
    }

    fun syncManual(event: String) {
        val tick = when (event.lowercase()) {
            "sunrise" -> 0L
            "noon" -> 6_000L
            "sunset" -> 11_000L
            else -> return
        }
        val now = System.currentTimeMillis()
        _state.update {
            it.copy(synced = true, syncMethod = "manual", syncTick = tick, syncTimeMs = now)
        }
        persistSync(tick, now, "manual")
        startTicking()
    }

    fun resetSync() {
        _state.update { it.copy(synced = false, syncMethod = "", syncTick = 0, syncTimeMs = 0, currentTick = 0, timeString = "--:--", dayProgress = 0f, events = emptyList()) }
        viewModelScope.launch {
            store.edit {
                it.remove(PreferenceKeys.CLOCK_TICK_OFFSET)
                it.remove(PreferenceKeys.CLOCK_SYNC_TIME_MS)
                it.remove(PreferenceKeys.CLOCK_SYNC_METHOD)
            }
        }
    }

    fun setDay(day: Long) {
        val s = _state.value
        if (!s.synced) return
        val daysSinceSync = ClockEngine.elapsedDays(s.syncTimeMs)
        val offset = day - daysSinceSync
        _state.update { it.copy(dayOffset = offset, displayedDay = day) }
        viewModelScope.launch {
            store.edit { it[PreferenceKeys.CLOCK_DAY_OFFSET] = offset }
        }
    }

    fun addPredefinedEvent(id: String) {
        val event = ClockEngine.PREDEFINED_EVENTS.find { it.predefinedId == id } ?: return
        val current = _state.value.activeEvents
        if (current.any { it.predefinedId == id }) return
        val updated = (current + event).sortedBy { it.tick }
        _state.update { it.copy(activeEvents = updated) }
        persistEvents(updated)
    }

    fun addCustomEvent(name: String, tick: Long, color: String) {
        if (name.isBlank() || tick < 0 || tick >= ClockEngine.TICKS_PER_DAY) return
        val event = ClockEngine.GameEvent(
            name = name.trim(),
            tick = tick,
            category = "custom",
            color = color,
            predefinedId = null,
        )
        val updated = (_state.value.activeEvents + event).sortedBy { it.tick }
        _state.update { it.copy(activeEvents = updated) }
        persistEvents(updated)
    }

    fun removeEvent(event: ClockEngine.GameEvent) {
        val updated = _state.value.activeEvents.filter { it != event }.sortedBy { it.tick }
        _state.update { it.copy(activeEvents = updated) }
        persistEvents(updated)
    }

    private fun persistEvents(events: List<ClockEngine.GameEvent>) {
        viewModelScope.launch {
            store.edit {
                it[PreferenceKeys.CLOCK_ACTIVE_EVENTS] = ClockEngine.serializeEvents(events)
            }
        }
    }

    private fun persistSync(tick: Long, timeMs: Long, method: String) {
        viewModelScope.launch {
            store.edit {
                it[PreferenceKeys.CLOCK_TICK_OFFSET] = tick
                it[PreferenceKeys.CLOCK_SYNC_TIME_MS] = timeMs
                it[PreferenceKeys.CLOCK_SYNC_METHOD] = method
            }
        }
    }

    private var tickingJob: kotlinx.coroutines.Job? = null

    private fun startTicking() {
        tickingJob?.cancel()
        tickingJob = viewModelScope.launch {
            while (isActive) {
                val s = _state.value
                if (!s.synced) break
                val current = ClockEngine.currentTick(s.syncTick, s.syncTimeMs)
                val timeStr = ClockEngine.formatTime(current)
                val progress = current.toFloat() / ClockEngine.TICKS_PER_DAY
                val displayedDay = s.dayOffset + ClockEngine.elapsedDays(s.syncTimeMs)
                val events = s.activeEvents
                    .map { evt ->
                        val away = ClockEngine.ticksUntil(current, evt.tick)
                        EventDisplay(
                            name = evt.name,
                            countdown = ClockEngine.formatCountdown(if (away == 0L) ClockEngine.TICKS_PER_DAY else away),
                            category = evt.category,
                            color = evt.color,
                            ticksAway = if (away == 0L) ClockEngine.TICKS_PER_DAY else away,
                        )
                    }
                    .sortedBy { it.ticksAway }

                _state.update {
                    it.copy(
                        currentTick = current,
                        timeString = timeStr,
                        dayProgress = progress,
                        events = events,
                        displayedDay = displayedDay,
                    )
                }
                delay(1000)
            }
        }
    }
}
