package dev.spyglass.android.connect.client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Fetches Minecraft player skin avatars from Crafatar.
 * Single-entry in-memory cache (one player at a time).
 */
object SkinManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private var cachedUuid: String? = null
    private var cachedBitmap: Bitmap? = null
    private var cachedBodyBitmap: Bitmap? = null
    private var cachedPlayerName: String? = null

    suspend fun fetchSkin(uuid: String): Bitmap? {
        // Return cached if same player
        if (uuid == cachedUuid && cachedBitmap != null) return cachedBitmap

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://crafatar.com/avatars/$uuid?size=64&overlay")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bytes = response.body?.bytes() ?: return@withContext null
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        cachedUuid = uuid
                        cachedBitmap = bitmap
                    }
                    bitmap
                }
            } catch (e: Exception) {
                Timber.d(e, "Failed to fetch skin for $uuid")
                null
            }
        }
    }

    /** Fetch full body render from Starlight SkinAPI. */
    suspend fun fetchBodyRender(playerName: String): Bitmap? {
        if (playerName == cachedPlayerName && cachedBodyBitmap != null) return cachedBodyBitmap

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://starlightskins.lunareclipse.studio/render/default/$playerName/full")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bytes = response.body?.bytes() ?: return@withContext null
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        cachedBodyBitmap = bitmap
                    }
                    bitmap
                }
            } catch (e: Exception) {
                Timber.d(e, "Failed to fetch body render for $playerName")
                null
            }
        }
    }

    /** Fetch player name from Mojang session API. */
    suspend fun fetchPlayerName(uuid: String): String? {
        if (uuid == cachedUuid && cachedPlayerName != null) return cachedPlayerName

        return withContext(Dispatchers.IO) {
            try {
                val cleanUuid = uuid.replace("-", "")
                val request = Request.Builder()
                    .url("https://sessionserver.mojang.com/session/minecraft/profile/$cleanUuid")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    val nameMatch = Regex(""""name"\s*:\s*"([^"]+)"""").find(body)
                    val name = nameMatch?.groupValues?.get(1)
                    if (name != null) {
                        cachedPlayerName = name
                    }
                    name
                }
            } catch (e: Exception) {
                Timber.d(e, "Failed to fetch player name for $uuid")
                null
            }
        }
    }

    fun clear() {
        cachedUuid = null
        cachedBitmap = null
        cachedBodyBitmap = null
        cachedPlayerName = null
    }
}
