package dev.spyglass.android.home

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val body: String,
    val image: String? = null,
    val date: String,
)

object NewsLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /** Loads news items from synced storage, falling back to bundled assets. */
    fun load(context: Context): List<NewsItem> {
        // Try synced file first
        val syncedFile = File(context.filesDir, "minecraft/news.json")
        if (syncedFile.exists()) {
            return tryParse(syncedFile.readText())
        }
        // Fall back to bundled asset
        return try {
            val raw = context.assets.open("minecraft/news.json").bufferedReader().readText()
            tryParse(raw)
        } catch (e: Exception) {
            Timber.d("No bundled news.json found")
            emptyList()
        }
    }

    private fun tryParse(raw: String): List<NewsItem> {
        return try {
            json.decodeFromString<List<NewsItem>>(raw)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse news.json")
            emptyList()
        }
    }
}
