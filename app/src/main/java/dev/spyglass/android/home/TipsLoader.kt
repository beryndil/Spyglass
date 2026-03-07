package dev.spyglass.android.home

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Tip(val id: Int, val text: String, val edition: String = "both")

object TipsLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<Tip> {
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
