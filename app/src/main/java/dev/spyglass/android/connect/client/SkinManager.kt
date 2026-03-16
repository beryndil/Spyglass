package dev.spyglass.android.connect.client

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Fetches Minecraft player skin avatars.
 * Head avatars use a multi-entry cache (for Players screen).
 * Body renders + player names use single-entry caches (one player at a time).
 */
object SkinManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val headCache = ConcurrentHashMap<String, Bitmap>()
    private var cachedBodyBitmap: Bitmap? = null
    private var cachedPlayerName: String? = null
    private var cachedBodyPlayerName: String? = null
    private var cachedNameUuid: String? = null

    private val AVATAR_URLS = listOf(
        "https://mc-heads.net/avatar/%s/64",
        "https://minotar.net/helm/%s/64",
    )

    suspend fun fetchSkin(uuid: String): Bitmap? {
        headCache[uuid]?.let { return it }

        val cleanUuid = uuid.replace("-", "")
        return withContext(Dispatchers.IO) {
            for (urlTemplate in AVATAR_URLS) {
                try {
                    val request = Request.Builder()
                        .url(urlTemplate.format(cleanUuid))
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val bytes = response.body?.bytes() ?: return@use
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            headCache[uuid] = bitmap
                            return@withContext bitmap
                        }
                    }
                } catch (e: Exception) {
                    Timber.d(e, "Failed to fetch skin from ${urlTemplate.format(cleanUuid)}")
                }
            }
            null
        }
    }

    /** Fetch full body render from Starlight SkinAPI (dungeons pose). */
    suspend fun fetchBodyRender(playerName: String): Bitmap? {
        if (playerName == cachedBodyPlayerName && cachedBodyBitmap != null) return cachedBodyBitmap

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://starlightskins.lunareclipse.studio/render/dungeons/$playerName/full")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bytes = response.body?.bytes() ?: return@withContext null
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        cachedBodyPlayerName = playerName
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
        if (uuid == cachedNameUuid && cachedPlayerName != null) return cachedPlayerName

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
                        cachedNameUuid = uuid
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
        headCache.clear()
        cachedBodyBitmap = null
        cachedBodyPlayerName = null
        cachedPlayerName = null
        cachedNameUuid = null
    }
}
