package dev.spyglass.android.connect.client

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R

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
}

/** Resolves the localized status text for a [ConnectionState] using string resources. */
@Composable
fun connectionStatusText(state: ConnectionState): String = when (state) {
    is ConnectionState.Disconnected -> stringResource(R.string.connect_state_disconnected)
    is ConnectionState.Scanning -> stringResource(R.string.connect_state_scanning)
    is ConnectionState.Connecting -> stringResource(R.string.connect_state_connecting, state.ip)
    is ConnectionState.Pairing -> stringResource(R.string.connect_state_pairing)
    is ConnectionState.Connected -> stringResource(R.string.connect_state_connected, state.deviceName)
    is ConnectionState.Reconnecting -> stringResource(R.string.connect_state_reconnecting, state.attempt)
    is ConnectionState.Error -> stringResource(R.string.connect_state_error, state.message)
}
