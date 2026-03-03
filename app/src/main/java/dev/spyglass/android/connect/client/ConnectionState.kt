package dev.spyglass.android.connect.client

/**
 * Sealed class representing all possible connection states for Spyglass Connect.
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Scanning : ConnectionState()
    data class Connecting(val ip: String, val port: Int) : ConnectionState()
    data object Pairing : ConnectionState()
    data class Connected(val deviceName: String) : ConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int = 10) : ConnectionState()
    data class Error(val message: String) : ConnectionState()

    val isConnected: Boolean get() = this is Connected
    val statusText: String
        get() = when (this) {
            is Disconnected -> "Disconnected"
            is Scanning -> "Scanning QR code..."
            is Connecting -> "Connecting to $ip..."
            is Pairing -> "Pairing..."
            is Connected -> "Connected to $deviceName"
            is Reconnecting -> "Reconnecting (attempt $attempt)..."
            is Error -> "Error: $message"
        }
}
