package dev.spyglass.android.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePreferences {

    private const val FILE_NAME = "spyglass_secure_prefs"
    private const val KEY_PLAYER_USERNAME = "player_username"
    private const val KEY_PLAYER_UUID = "player_uuid"

    @Volatile
    private var INSTANCE: SharedPreferences? = null

    fun get(context: Context): SharedPreferences =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: createEncryptedPrefs(context.applicationContext).also { INSTANCE = it }
        }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getPlayerUsername(context: Context): String =
        get(context).getString(KEY_PLAYER_USERNAME, "") ?: ""

    fun setPlayerUsername(context: Context, username: String) {
        get(context).edit().putString(KEY_PLAYER_USERNAME, username).apply()
    }

    fun getPlayerUuid(context: Context): String =
        get(context).getString(KEY_PLAYER_UUID, "") ?: ""

    fun setPlayerUuid(context: Context, uuid: String) {
        get(context).edit().putString(KEY_PLAYER_UUID, uuid).apply()
    }

    fun clearPlayer(context: Context) {
        get(context).edit()
            .remove(KEY_PLAYER_USERNAME)
            .remove(KEY_PLAYER_UUID)
            .apply()
    }

    fun clearAll(context: Context) {
        get(context).edit().clear().apply()
    }

    // Convenience overload for SharedPreferences instance
    fun SharedPreferences.clearAll() {
        edit().clear().apply()
    }
}
