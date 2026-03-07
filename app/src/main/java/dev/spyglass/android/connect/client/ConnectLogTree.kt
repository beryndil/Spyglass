package dev.spyglass.android.connect.client

import android.util.Log
import dev.spyglass.android.connect.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Timber tree that captures warnings, errors, and crashes and sends them
 * to the desktop over the existing WebSocket connection.
 *
 * Logs are batched — buffered while disconnected and flushed when the
 * WebSocket becomes available.
 */
class ConnectLogTree(private val client: SpyglassClient) : Timber.Tree() {

    private val json = Json { encodeDefaults = true }
    private val buffer = ConcurrentLinkedQueue<DeviceLogEntry>()

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        // Capture warnings, errors, and WTF (assert) only
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = when (priority) {
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "WTF"
            else -> return
        }

        val throwableStr = if (t != null) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            sw.toString().take(4000) // Cap stack trace size
        } else null

        val entry = DeviceLogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag ?: "unknown",
            message = message.take(2000),
            throwable = throwableStr,
        )

        buffer.add(entry)

        // Trim buffer if it gets too large (keep most recent 200)
        while (buffer.size > 200) buffer.poll()

        // Try to flush if connected
        flush()
    }

    /** Flush all buffered log entries to the desktop. */
    fun flush() {
        if (buffer.isEmpty()) return
        if (!client.connectionState.value.isConnected) return

        val entries = mutableListOf<DeviceLogEntry>()
        while (true) {
            val entry = buffer.poll() ?: break
            entries.add(entry)
        }
        if (entries.isEmpty()) return

        try {
            val payload = json.encodeToJsonElement(DeviceLogPayload(entries))
            client.sendRequest(MessageType.DEVICE_LOG, payload)
        } catch (e: Exception) {
            // Put entries back if send failed
            entries.forEach { buffer.add(it) }
            while (buffer.size > 200) buffer.poll()
        }
    }
}
