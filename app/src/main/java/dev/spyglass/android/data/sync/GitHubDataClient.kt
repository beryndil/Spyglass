package dev.spyglass.android.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Fetches data files from the Spyglass CDN (Cloudflare Pages fronting Spyglass-Data).
 * Fallback: raw.githubusercontent.com if CDN is unreachable.
 */
object GitHubDataClient {

    private const val CDN_BASE_URL = "https://data.hardknocks.university"
    private const val GITHUB_BASE_URL =
        "https://raw.githubusercontent.com/beryndil/Spyglass-Data/main"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Fetches the remote manifest.json. Returns null on network failure. */
    suspend fun fetchManifest(): DataManifest? = withContext(Dispatchers.IO) {
        try {
            val body = fetchWithFallback("manifest.json") ?: return@withContext null
            DataManifest.fromJson(body)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse remote manifest")
            null
        }
    }

    /** Fetches a data file by name (e.g. "blocks.json"). Returns null on failure. */
    suspend fun fetchDataFile(fileName: String): String? = withContext(Dispatchers.IO) {
        fetchWithFallback(fileName)
    }

    /** Fetches a binary file (e.g. "textures.zip"). Returns null on failure. */
    suspend fun fetchBinaryFile(fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        fetchBytesWithFallback(fileName)
    }

    /** Try CDN first, fall back to GitHub raw if CDN fails. */
    private fun fetchWithFallback(path: String): String? {
        return fetch("$CDN_BASE_URL/$path")
            ?: fetch("$GITHUB_BASE_URL/$path").also {
                if (it != null) Timber.d("CDN miss, fell back to GitHub for %s", path)
            }
    }

    private fun fetchBytesWithFallback(path: String): ByteArray? {
        return fetchBytes("$CDN_BASE_URL/$path")
            ?: fetchBytes("$GITHUB_BASE_URL/$path").also {
                if (it != null) Timber.d("CDN miss, fell back to GitHub for %s", path)
            }
    }

    private fun fetch(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Timber.w("HTTP %d fetching %s", response.code, url)
                    null
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Network error fetching %s", url)
            null
        }
    }

    private fun fetchBytes(url: String): ByteArray? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Timber.w("HTTP %d fetching %s", response.code, url)
                    null
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Network error fetching %s", url)
            null
        }
    }
}
