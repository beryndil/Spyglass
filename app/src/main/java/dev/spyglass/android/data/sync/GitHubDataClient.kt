package dev.spyglass.android.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Fetches data files from the GitHub repository via raw.githubusercontent.com.
 */
object GitHubDataClient {

    private const val OWNER = "Dev-VulX"
    private const val REPO = "Spyglass-Data"
    private const val BRANCH = "main"
    private const val BASE_URL =
        "https://raw.githubusercontent.com/$OWNER/$REPO/$BRANCH"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Fetches the remote manifest.json. Returns null on network failure. */
    suspend fun fetchManifest(): DataManifest? = withContext(Dispatchers.IO) {
        try {
            val body = fetch("$BASE_URL/manifest.json") ?: return@withContext null
            DataManifest.fromJson(body)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse remote manifest")
            null
        }
    }

    /** Fetches a data file by name (e.g. "blocks.json"). Returns null on failure. */
    suspend fun fetchDataFile(fileName: String): String? = withContext(Dispatchers.IO) {
        fetch("$BASE_URL/$fileName")
    }

    private fun fetch(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-cache")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                Timber.w("HTTP %d fetching %s", response.code, url)
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "Network error fetching %s", url)
            null
        }
    }
}
