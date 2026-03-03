package dev.spyglass.android.connect.client

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persist paired device info to DataStore for reconnection without re-scanning QR.
 */
object PairingStore {

    @Serializable
    data class PairedDevice(
        val ip: String,
        val port: Int,
        val deviceName: String,
        val publicKey: String,
        val lastConnected: Long = System.currentTimeMillis(),
    )

    private val PAIRED_DEVICE_KEY = stringPreferencesKey("connect_paired_device")
    private val json = Json { ignoreUnknownKeys = true }

    /** Save a paired device. */
    suspend fun save(context: Context, device: PairedDevice) {
        context.dataStore.edit { prefs ->
            prefs[PAIRED_DEVICE_KEY] = json.encodeToString(device)
        }
    }

    /** Load the paired device, if any. */
    suspend fun load(context: Context): PairedDevice? {
        val raw = context.dataStore.data.map { it[PAIRED_DEVICE_KEY] }.first() ?: return null
        return try {
            json.decodeFromString<PairedDevice>(raw)
        } catch (_: Exception) {
            null
        }
    }

    /** Clear the paired device (unpair). */
    suspend fun clear(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(PAIRED_DEVICE_KEY)
        }
    }
}
