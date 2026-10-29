package ru.mlbb.blocker.data.rules

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Загрузка правил с GitHub-релиза. Файлы лежат рядом с APK:
 * rules.json и rules.json.sig (detached-подпись ключом релиза).
 */
object RulesRemoteClient {

    const val RULES_URL =
        "https://github.com/mrkoronalol/mlbb-blocker/releases/latest/download/rules.json"
    const val SIGNATURE_URL =
        "https://github.com/mrkoronalol/mlbb-blocker/releases/latest/download/rules.json.sig"

    private const val MAX_RULES_BYTES = 256 * 1024
    private const val MAX_SIGNATURE_BYTES = 8 * 1024
    private const val TIMEOUT_MS = 10_000

    fun fetchRules(): ByteArray = fetch(RULES_URL, MAX_RULES_BYTES)

    fun fetchSignature(): String {
        val bytes = fetch(SIGNATURE_URL, MAX_SIGNATURE_BYTES)
        return String(bytes, Charsets.UTF_8).trim()
    }

    private fun fetch(url: String, limit: Int): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "MLBB-Blocker")
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP $code")
            }
            val declared = connection.contentLengthLong
            if (declared > limit) throw IllegalStateException("слишком большой файл: $declared байт")
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    if (output.size() > limit) {
                        throw IllegalStateException("файл превысил $limit байт")
                    }
                }
            }
            if (output.size() == 0) throw IllegalStateException("пустой ответ")
            return output.toByteArray()
        } finally {
            runCatching { connection.disconnect() }
        }
    }
}
