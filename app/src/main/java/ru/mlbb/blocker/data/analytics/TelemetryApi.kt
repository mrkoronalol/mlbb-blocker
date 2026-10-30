package ru.mlbb.blocker.data.analytics

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Отправка телеметрии на сайт. Эндпоинты публичные, без API-ключа:
 * приложение не может хранить секрет.
 */
class TelemetryApi(baseUrl: String) {

    private val base = baseUrl.trim().trimEnd('/')

    fun sendEvents(
        deviceId: String,
        appVersion: String,
        events: List<AnalyticsEventEntity>,
    ): Boolean {
        if (base.isBlank() || events.isEmpty()) return false
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject()
                    .put("name", event.name)
                    .put("properties", parseProperties(event.properties)),
            )
        }
        val body = JSONObject()
            .put("platform", "android")
            .put("deviceId", deviceId)
            .put("appVersion", appVersion)
            .put("events", array)
        return post("/api/v1/telemetry/events", body)
    }

    fun sendIps(items: List<TelemetryIpEntity>): Boolean {
        if (base.isBlank() || items.isEmpty()) return false
        val array = JSONArray()
        items.forEach { item ->
            val json = JSONObject()
                .put("ip", item.ip)
                .put("port", item.port)
                .put("protocol", item.protocol)
            item.city?.let { json.put("city", it) }
            item.country?.let { json.put("country", it) }
            item.pingMs?.let { json.put("pingMs", it) }
            array.put(json)
        }
        return post("/api/v1/telemetry/ips", JSONObject().put("items", array))
    }

    private fun parseProperties(raw: String): JSONObject {
        return runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
    }

    private fun post(path: String, body: JSONObject): Boolean {
        return try {
            val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 12_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MLBB-Blocker")
            }
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    runCatching { connection.errorStream?.close() }
                    return false
                }
                runCatching { connection.inputStream.close() }
                true
            } finally {
                runCatching { connection.disconnect() }
            }
        } catch (_: Throwable) {
            false
        }
    }
}
