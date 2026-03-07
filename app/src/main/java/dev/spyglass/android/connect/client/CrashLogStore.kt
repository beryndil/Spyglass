package dev.spyglass.android.connect.client

import android.content.Context
import dev.spyglass.android.connect.DeviceLogEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Persists crash and error log entries to disk so they survive process death.
 * On next app launch + WebSocket connection, pending entries are loaded and sent.
 */
object CrashLogStore {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private fun logDir(context: Context): File =
        File(context.filesDir, "connect-logs")

    /** Save a fatal crash entry to disk (called from uncaught exception handler). */
    fun saveCrash(context: Context, thread: String, throwable: Throwable) {
        try {
            val dir = logDir(context)
            dir.mkdirs()

            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))

            val entry = DeviceLogEntry(
                timestamp = System.currentTimeMillis(),
                level = "FATAL",
                tag = "UncaughtException",
                message = "Fatal crash on thread $thread: ${throwable.message?.take(500)}",
                throwable = sw.toString().take(4000),
            )

            val file = File(dir, "crash_${System.currentTimeMillis()}.json")
            file.writeText(json.encodeToString(entry))
        } catch (_: Exception) {
            // Last resort — can't do anything if disk write fails during crash
        }
    }

    /** Load and delete all pending crash log files. */
    fun loadAndClear(context: Context): List<DeviceLogEntry> {
        val dir = logDir(context)
        if (!dir.isDirectory) return emptyList()

        val entries = mutableListOf<DeviceLogEntry>()
        val files = dir.listFiles { f -> f.name.startsWith("crash_") && f.name.endsWith(".json") }
            ?: return emptyList()

        for (file in files) {
            try {
                val entry = json.decodeFromString<DeviceLogEntry>(file.readText())
                entries.add(entry)
                file.delete()
            } catch (e: Exception) {
                Timber.w(e, "Failed to load crash log: ${file.name}")
                file.delete() // Remove corrupt files
            }
        }
        return entries
    }
}
