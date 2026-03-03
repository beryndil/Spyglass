package dev.spyglass.android.connect.client

import kotlinx.coroutines.delay

/**
 * Exponential backoff reconnection manager.
 * Tries stored IP first, then falls back to mDNS discovery.
 */
class ReconnectManager {

    companion object {
        private const val INITIAL_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 30_000L
        private const val MAX_ATTEMPTS = 10
    }

    private var attempt = 0

    /** Get the next delay for reconnection, or null if max attempts reached. */
    fun nextDelay(): Long? {
        if (attempt >= MAX_ATTEMPTS) return null
        val delay = (INITIAL_DELAY_MS * (1L shl attempt.coerceAtMost(5))).coerceAtMost(MAX_DELAY_MS)
        attempt++
        return delay
    }

    /** Current attempt number. */
    val currentAttempt: Int get() = attempt

    /** Reset the backoff counter (on successful connection). */
    fun reset() {
        attempt = 0
    }

    /** Whether max attempts have been reached. */
    val isExhausted: Boolean get() = attempt >= MAX_ATTEMPTS

    /** Suspend until the next retry delay. Returns false if exhausted. */
    suspend fun waitForNextRetry(): Boolean {
        val d = nextDelay() ?: return false
        delay(d)
        return true
    }
}
