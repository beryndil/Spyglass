package dev.spyglass.android.calculators.clock

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ClockEngine {
    const val TICKS_PER_DAY = 24_000L
    const val COUNTDOWN_PREVIEW_TICKS = 600L // 30 real seconds

    fun currentTick(syncTick: Long, syncTimeMs: Long): Long {
        val elapsed = System.currentTimeMillis() - syncTimeMs
        val ticksElapsed = elapsed / 50 // 20 ticks/sec = 50ms/tick
        return ((syncTick + ticksElapsed) % TICKS_PER_DAY + TICKS_PER_DAY) % TICKS_PER_DAY
    }

    fun tickToHours(tick: Long): Int = ((tick / 1000 + 6) % 24).toInt()
    fun tickToMinutes(tick: Long): Int = ((tick % 1000) * 60 / 1000).toInt()

    fun formatTime(tick: Long): String {
        val h = tickToHours(tick)
        val m = tickToMinutes(tick)
        return "%02d:%02d".format(h, m)
    }

    fun ticksUntil(current: Long, target: Long): Long =
        ((target - current) % TICKS_PER_DAY + TICKS_PER_DAY) % TICKS_PER_DAY

    fun formatCountdown(ticks: Long): String {
        val totalSeconds = ticks / 20
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    @Serializable
    data class GameEvent(
        val name: String,
        val tick: Long,
        val category: String,
        val color: String,
        val predefinedId: String? = null,
    )

    val PREDEFINED_EVENTS = listOf(
        GameEvent("Sunrise", 0, "sun", "green", "sunrise"),
        GameEvent("Villagers wake up", 0, "villager", "green", "villagers_wake"),
        GameEvent("Day begins", 1_000, "sun", "green", "day_begins"),
        GameEvent("Noon", 6_000, "sun", "green", "noon"),
        GameEvent("Sunset", 11_000, "sun", "gold", "sunset"),
        GameEvent("Villagers go to bed", 12_000, "villager", "gold", "villagers_sleep"),
        GameEvent("Beds usable", 12_542, "sleep", "gold", "beds_usable"),
        GameEvent("Night begins", 13_000, "moon", "red", "night_begins"),
        GameEvent("Hostile mobs start spawning", 13_188, "mob", "red", "mobs_spawn"),
        GameEvent("Midnight", 18_000, "moon", "red", "midnight"),
        GameEvent("Undead mobs start burning", 23_460, "sun", "green", "undead_burn"),
    )

    val DEFAULT_EVENT_IDS = setOf(
        "sunrise", "sunset", "beds_usable", "night_begins", "mobs_spawn", "undead_burn",
    )

    fun nextEvent(currentTick: Long, events: List<GameEvent>): Pair<GameEvent, Long>? {
        if (events.isEmpty()) return null
        val sorted = events.sortedBy { ticksUntil(currentTick, it.tick) }
        val next = sorted.firstOrNull { ticksUntil(currentTick, it.tick) > 0 } ?: sorted.first()
        val away = ticksUntil(currentTick, next.tick)
        return next to (if (away == 0L) TICKS_PER_DAY else away)
    }

    fun currentEvent(currentTick: Long, events: List<GameEvent>): GameEvent? {
        if (events.isEmpty()) return null
        // The current event is the most recent one that has passed (largest ticksUntil from the "behind" perspective)
        return events.maxByOrNull { (currentTick - it.tick + TICKS_PER_DAY) % TICKS_PER_DAY }
    }

    fun serializeEvents(events: List<GameEvent>): String = Json.encodeToString(events)

    fun deserializeEvents(json: String): List<GameEvent> = try {
        Json.decodeFromString<List<GameEvent>>(json)
    } catch (_: Exception) {
        emptyList()
    }
}
