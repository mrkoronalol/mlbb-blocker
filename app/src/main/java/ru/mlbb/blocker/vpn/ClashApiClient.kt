package ru.mlbb.blocker.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.mlbb.blocker.data.Cidr
import ru.mlbb.blocker.data.ConnectionSnapshot
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object ClashApiClient {

    suspend fun fetch(port: Int, secret: String): List<ConnectionSnapshot> = withContext(Dispatchers.IO) {
        val connection = (URL("http://127.0.0.1:$port/connections").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 1500
            readTimeout = 1500
            setRequestProperty("Authorization", "Bearer $secret")
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("Clash API HTTP $code")
            }
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            parse(payload)
        } finally {
            connection.disconnect()
        }
    }

    fun parse(payload: String): List<ConnectionSnapshot> {
        val root = try {
            JSONObject(payload)
        } catch (_: Exception) {
            return emptyList()
        }
        val array = root.optJSONArray("connections") ?: return emptyList()
        val result = LinkedHashMap<String, ConnectionSnapshot>()
        for (index in 0 until array.length()) {
            val connection = array.optJSONObject(index) ?: continue
            val metadata = connection.optJSONObject("metadata") ?: continue
            val ip = metadata.optString("destinationIP").trim()
            if (ip.isEmpty() || !looksLikeIp(ip)) continue
            if (Cidr.isPrivate(ip)) continue

            val port = metadata.optInt("destinationPort", 0).takeIf { it > 0 } ?: continue
            if (port == 53) continue

            val network = metadata.optString("network", "tcp").ifBlank { "tcp" }
            val host = metadata.optString("host").takeIf { it.isNotBlank() }
                ?: metadata.optString("sniffHost").takeIf { it.isNotBlank() }
            val id = connection.optString("id").ifBlank { "$network://$ip:$port" }
            val key = "$network://$ip:$port"
            result[key] = ConnectionSnapshot(id, ip, port, host, network)
        }
        return result.values.toList()
    }

    private fun looksLikeIp(value: String): Boolean {
        return Cidr.isIpv4(value) || (value.contains(':') && Cidr.normalize(value) != null)
    }
}
