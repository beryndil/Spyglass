package dev.spyglass.android.home

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Tip(val id: Int, val text: String, val edition: String = "both")

object TipsLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /** Cached tips to avoid repeated disk I/O. */
    @Volatile
    private var cached: List<Tip>? = null

    /** Load tips from disk (IO-safe). Call from coroutine or background thread. */
    suspend fun load(context: Context): List<Tip> = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        val tips = loadFromDisk(context)
        cached = tips
        tips
    }

    /** Synchronous load for callers already on IO thread. Uses cache if available. */
    fun loadCached(): List<Tip> = cached ?: emptyList()

    private fun loadFromDisk(context: Context): List<Tip> {
        val syncedFile = File(context.filesDir, "minecraft/tips.json")
        if (syncedFile.exists()) {
            return tryParse(syncedFile.readText())
        }
        return try {
            val raw = context.assets.open("minecraft/tips.json").bufferedReader().readText()
            tryParse(raw)
        } catch (e: Exception) { emptyList() }
    }

    private fun tryParse(raw: String): List<Tip> = try {
        json.decodeFromString<List<Tip>>(raw)
    } catch (e: Exception) { emptyList() }
}
