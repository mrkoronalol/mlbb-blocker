package ru.mlbb.blocker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val tag: String,
    val title: String,
    val releaseUrl: String,
    val downloadUrl: String,
    val notes: String,
    val sha256: String? = null,
    val size: Long? = null,
)

object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/mrkoronalol/mlbb-blocker/releases/latest"

    suspend fun checkLatest(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "MLBB-Blocker")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            parseRelease(JSONObject(body), currentVersion)
        } catch (_: Throwable) {
            null
        }
    }

    internal fun parseRelease(json: JSONObject, currentVersion: String): UpdateInfo? {
        val tag = json.optString("tag_name").ifBlank { return null }
        val version = tag.removePrefix("v")
        if (!isNewer(version, currentVersion)) return null

        val releaseUrl = json.optString("html_url")
        if (releaseUrl.isBlank()) return null

        var selected: JSONObject? = null
        var fallback: JSONObject? = null
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (!name.endsWith(".apk", ignoreCase = true) || url.isBlank()) continue
                if (name.contains("arm64", ignoreCase = true)) {
                    selected = asset
                    break
                }
                if (fallback == null) fallback = asset
            }
            if (selected == null) selected = fallback
        }
        val downloadUrl = selected?.optString("browser_download_url")
            ?.takeIf { it.isNotBlank() }
            ?: releaseUrl

        return UpdateInfo(
            version = version,
            tag = tag,
            title = json.optString("name").ifBlank { "MLBB Blocker $tag" },
            releaseUrl = releaseUrl,
            downloadUrl = downloadUrl,
            notes = json.optString("body"),
            sha256 = selected?.optString("digest")
                ?.removePrefix("sha256:")
                ?.takeIf { it.length == 64 },
            size = selected?.optLong("size", 0L)?.takeIf { it > 0 },
        )
    }

    internal fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.', '-', '+').mapNotNull { it.toIntOrNull() }
        val b = current.split('.', '-', '+').mapNotNull { it.toIntOrNull() }
        val size = maxOf(a.size, b.size)
        for (index in 0 until size) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        return false
    }
}
