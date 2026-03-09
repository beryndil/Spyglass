package dev.spyglass.android.connect.client

import android.util.Log
import dev.spyglass.android.connect.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Timber tree that captures warnings, errors, and crashes and sends them
 * to the desktop over the existing WebSocket connection.
 *
 * Smart features:
 * - Deduplicates repeated messages (collapses to "× N" counts)
 * - Extracts root cause from throwables instead of full obfuscated traces
 * - Throttles flush to max once per 2 seconds to avoid reconnect spam
 */
class ConnectLogTree(private val client: SpyglassClient) : Timber.Tree() {

    private val json = Json { encodeDefaults = true }
    private val buffer = ConcurrentLinkedQueue<DeviceLogEntry>()

    // Deduplication: track last message to collapse repeats
    @Volatile private var lastKey = ""
    @Volatile private var lastEntry: DeviceLogEntry? = null
    @Volatile private var repeatCount = 0

    // Throttle: don't flush more than once per 2 seconds
    private val lastFlushTime = AtomicLong(0)
    private companion object {
        const val FLUSH_INTERVAL_MS = 2000L
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = when (priority) {
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "WTF"
            else -> return
        }

        val safeTag = tag ?: "unknown"
        val safeMessage = message.take(2000)
        val key = "$level/$safeTag:$safeMessage"

        // Extract just the root cause message — full R8-obfuscated traces are useless
        val throwableStr = t?.let { extractRootCause(it) }

        synchronized(this) {
            if (key == lastKey && repeatCount < 999) {
                // Same message again — just bump the count
                repeatCount++
                return
            }

            // Different message — flush any pending repeats first
            emitPendingRepeat()

            val entry = DeviceLogEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                tag = safeTag,
                message = safeMessage,
                throwable = throwableStr,
            )

            lastKey = key
            lastEntry = entry
            repeatCount = 1
            buffer.add(entry)

            while (buffer.size > 200) buffer.poll()
        }

        // Throttled flush
        val now = System.currentTimeMillis()
        if (now - lastFlushTime.get() >= FLUSH_INTERVAL_MS) {
            lastFlushTime.set(now)
            flush()
        }
    }

    /** Emit a "repeated N times" entry if the last message was seen more than once. */
    private fun emitPendingRepeat() {
        if (repeatCount > 1) {
            val last = lastEntry ?: return
            buffer.add(DeviceLogEntry(
                timestamp = System.currentTimeMillis(),
                level = last.level,
                tag = last.tag,
                message = "↑ repeated ${repeatCount - 1} more times",
                throwable = null,
            ))
            while (buffer.size > 200) buffer.poll()
        }
    }

    /**
     * Extract a human-readable root cause from a throwable chain.
     * Skips obfuscated stack frames — just returns the exception class + message
     * for each cause in the chain.
     */
    private fun extractRootCause(t: Throwable): String {
        val causes = mutableListOf<String>()
        var current: Throwable? = t
        val seen = mutableSetOf<Throwable>()
        while (current != null && seen.add(current)) {
            val name = current.javaClass.name
            val msg = current.message?.take(500) ?: ""
            causes.add("$name: $msg")
            current = current.cause
        }
        return causes.joinToString("\n  Caused by: ").take(2000)
    }

    /** Flush all buffered log entries to the desktop. */
    fun flush() {
        if (buffer.isEmpty() && repeatCount <= 1) return
        if (!client.connectionState.value.isConnected) return

        // Emit any pending repeat count before flushing
        synchronized(this) {
            emitPendingRepeat()
            repeatCount = if (lastEntry != null) 1 else 0
        }

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
