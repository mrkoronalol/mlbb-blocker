package ru.mlbb.blocker.data.meta

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface MetaResult<out T> {
    data class Success<T>(val value: T) : MetaResult<T>
    data class Failure(val message: String) : MetaResult<Nothing>
}

/**
 * Клиент публичного API сайта mlbb-meta.
 * Базовый адрес задаётся в настройках приложения (по умолчанию — заглушка из
 * BuildConfig.META_API_BASE), поэтому после деплоя сайта его достаточно поменять
 * на реальный домен.
 */
class MetaApi(private val baseUrl: String) {

    private val base: String = baseUrl.trim().trimEnd('/')

    fun fetchMeta(region: String): MetaResult<MetaSnapshot> {
        if (base.isBlank()) {
            return MetaResult.Failure("Адрес API не задан. Укажите его в настройках.")
        }
        val url = "$base/api/v1/app/meta?region=${region.lowercase()}"
        return when (val response = get(url)) {
            is HttpText.Success -> parseMeta(response.body, region)
            is HttpText.Failure -> MetaResult.Failure(response.message)
        }
    }

    fun fetchCounters(heroId: String): MetaResult<MetaCounters> {
        if (base.isBlank()) return MetaResult.Failure("Адрес API не задан")
        val url = "$base/api/v1/matchups?heroId=$heroId&kind=counter"
        return when (val response = get(url)) {
            is HttpText.Success -> parseCounters(heroId, response.body)
            is HttpText.Failure -> MetaResult.Failure(response.message)
        }
    }

    internal fun parseMeta(body: String, region: String): MetaResult<MetaSnapshot> {
        if (body.trimStart().startsWith("<")) {
            return MetaResult.Failure(
                "По адресу API отвечает не MLBB Meta. Укажите домен сайта в настройках.",
            )
        }
        return try {
            val root = JSONObject(body)
            val array = root.optJSONArray("heroes")
                ?: return MetaResult.Failure("API вернул неожиданный ответ")
            val heroes = buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    parseHero(obj, index)?.let(::add)
                }
            }
            if (heroes.isEmpty()) {
                return MetaResult.Failure("API вернул пустой список героев")
            }
            MetaResult.Success(
                MetaSnapshot(
                    heroes = heroes,
                    region = root.optString("region", region),
                    regional = root.optBoolean("regional", false),
                    patch = root.optString("patch").takeIf { it.isNotBlank() } ?: root.optString("gamePatch").takeIf { it.isNotBlank() },
                    updatedAt = root.optString("updatedAt").takeIf { it.isNotBlank() },
                    fetchedAt = System.currentTimeMillis(),
                ),
            )
        } catch (t: Throwable) {
            MetaResult.Failure("Не удалось разобрать ответ API: ${t.message ?: "ошибка"}")
        }
    }

    private fun parseHero(obj: JSONObject, index: Int): MetaHero? {
        val id = obj.optString("id").ifBlank { return null }
        val lane = MetaLane.from(obj.optString("lane"))
        val lanes = when (val raw = obj.opt("lanes")) {
            is org.json.JSONArray -> (0 until raw.length())
                .mapNotNull { MetaLane.from(raw.optString(it)) }

            is String -> raw.split(',').mapNotNull { MetaLane.from(it) }
            else -> emptyList()
        }.distinct()
        val image = obj.optString("imageUrl").takeIf { it.isNotBlank() }
        return MetaHero(
            id = id,
            nameEn = obj.optString("nameEn").ifBlank { id },
            nameRu = obj.optString("nameRu"),
            role = obj.optString("role"),
            lane = lane,
            lanes = lanes.ifEmpty { listOfNotNull(lane) },
            imageUrl = image?.let(::absoluteUrl),
            winRate = obj.optDoubleOrNull("winRate"),
            pickRate = obj.optDoubleOrNull("pickRate"),
            banRate = obj.optDoubleOrNull("banRate"),
            matches = obj.optIntOrNull("matches"),
            tier = MetaTier.from(obj.optString("tier").ifBlank { obj.optString("metaTier") }),
            sortOrder = obj.optInt("sortOrder", index),
        )
    }

    private fun parseCounters(heroId: String, body: String): MetaResult<MetaCounters> {
        return try {
            val root = JSONObject(body)
            val array = root.optJSONArray("matchups") ?: root.optJSONArray("items")
            if (array == null) return MetaResult.Success(MetaCounters(heroId, emptyList(), emptyList()))
            val ids = mutableListOf<String>()
            val names = mutableListOf<String>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val targetId = item.optString("targetId")
                if (targetId.isBlank() || targetId == heroId) continue
                if (!ids.add(targetId)) continue
                names.add(item.optString("targetName").ifBlank { item.optString("name", targetId) })
            }
            MetaResult.Success(MetaCounters(heroId, names, ids))
        } catch (t: Throwable) {
            MetaResult.Failure("Не удалось загрузить контрпики: ${t.message ?: "ошибка"}")
        }
    }

    private fun absoluteUrl(value: String): String {
        return when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.startsWith("/") -> base + value
            else -> "$base/$value"
        }
    }

    private fun get(url: String): HttpText {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MLBB-Blocker")
            }
            try {
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    val hint = when (code) {
                        401, 403 -> "API закрыт: включите публичный доступ на сайте"
                        404 -> "Эндпоинт не найден: проверьте адрес API"
                        else -> "HTTP $code"
                    }
                    return HttpText.Failure(hint)
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                if (body.isBlank()) HttpText.Failure("Пустой ответ API") else HttpText.Success(body)
            } finally {
                runCatching { connection.disconnect() }
            }
        } catch (t: Throwable) {
            HttpText.Failure("Сайт недоступен: ${t.message ?: "ошибка сети"}")
        }
    }

    private sealed interface HttpText {
        data class Success(val body: String) : HttpText
        data class Failure(val message: String) : HttpText
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return value.takeIf { !it.isNaN() }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key, -1).takeIf { it >= 0 }
    }
}
