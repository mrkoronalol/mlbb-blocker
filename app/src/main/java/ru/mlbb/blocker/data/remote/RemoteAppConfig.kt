package ru.mlbb.blocker.data.remote

import org.json.JSONArray
import org.json.JSONObject
import ru.mlbb.blocker.data.RegionProfile
import ru.mlbb.blocker.data.RegionProfiles
import java.util.Locale

data class RemoteProfile(
    val id: String,
    val name: String,
    val description: String,
    val ips: List<String>,
)

data class RemoteAppConfig(
    val maintenance: Boolean = false,
    val notice: String = "",
    val featureMetaEnabled: Boolean = true,
    val minAppVersion: String = "",
    val appVersion: String? = null,
    val updateRequired: Boolean = false,
    val profiles: List<RemoteProfile> = emptyList(),
    val updatedAt: String? = null,
    val fetchedAt: Long = 0L,
) {
    fun toJson(): String = JSONObject().apply {
        put("maintenance", maintenance)
        put("notice", notice)
        put("feature_meta_enabled", featureMetaEnabled)
        put("min_app_version", minAppVersion)
        put("app_version", appVersion ?: JSONObject.NULL)
        put("update_required", updateRequired)
        put("updated_at", updatedAt ?: JSONObject.NULL)
        put("fetched_at", fetchedAt)
        put(
            "profiles",
            JSONArray().also { array ->
                profiles.forEach { profile ->
                    array.put(
                        JSONObject()
                            .put("id", profile.id)
                            .put("name", profile.name)
                            .put("description", profile.description)
                            .put("ips", JSONArray(profile.ips)),
                    )
                }
            },
        )
    }.toString()

    /** Профили приложения, собранные из удалённой конфигурации. */
    fun regionProfiles(): List<RegionProfile> = profiles.map { it.toRegionProfile() }

    companion object {
        val DEFAULT = RemoteAppConfig()

        fun parse(raw: String, fetchedAt: Long = System.currentTimeMillis()): RemoteAppConfig? {
            return try {
                val json = JSONObject(raw)
                val profiles = mutableListOf<RemoteProfile>()
                json.optJSONArray("profiles")?.let { array ->
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val name = item.optString("name").trim()
                        if (name.isEmpty()) continue
                        val ips = mutableListOf<String>()
                        item.optJSONArray("ips")?.let { list ->
                            for (i in 0 until list.length()) {
                                val value = list.optString(i).trim()
                                if (value.isNotEmpty()) ips += value
                            }
                        }
                        profiles += RemoteProfile(
                            id = item.optString("id").ifBlank { ProfileIds.fromName(name) },
                            name = name,
                            description = item.optString("description"),
                            ips = ips,
                        )
                    }
                }
                RemoteAppConfig(
                    maintenance = json.optBoolean("maintenance", false),
                    notice = json.optString("notice"),
                    featureMetaEnabled = json.optBoolean("feature_meta_enabled", true),
                    minAppVersion = json.optString("min_app_version"),
                    appVersion = json.optString("app_version").takeIf { it.isNotBlank() },
                    updateRequired = json.optBoolean("update_required", false),
                    profiles = profiles,
                    updatedAt = json.optString("updated_at").takeIf { it.isNotBlank() },
                    fetchedAt = fetchedAt,
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}

object ProfileIds {

    /** Стабильные id для трёх встроенных регионов, чтобы выбор не сбрасывался. */
    fun fromName(name: String): String {
        val normalized = name.trim().lowercase(Locale.ROOT)
        return when {
            normalized.contains("моск") || normalized.contains("moscow") ->
                RegionProfiles.ID_MOSCOW

            normalized.contains("новосиб") || normalized.contains("novosibirsk") ->
                RegionProfiles.ID_NOVOSIBIRSK

            normalized == "дв" || normalized.contains("восток") ||
                normalized.contains("far") || normalized.contains("хабаров") ->
                RegionProfiles.ID_FAR_EAST

            else -> slug(name)
        }
    }

    fun badge(name: String): String = when (fromName(name)) {
        RegionProfiles.ID_MOSCOW -> "МСК"
        RegionProfiles.ID_NOVOSIBIRSK -> "НСК"
        RegionProfiles.ID_FAR_EAST -> "ДВ"
        else -> name.trim().take(3).uppercase(Locale.ROOT)
    }

    private fun slug(value: String): String {
        val builder = StringBuilder()
        value.trim().lowercase(Locale.ROOT).forEach { char ->
            builder.append(TRANSLIT[char] ?: char)
        }
        val cleaned = builder.toString().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        return cleaned.ifBlank { "profile" }
    }

    private val TRANSLIT: Map<Char, String> = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
        'е' to "e", 'ё' to "e", 'ж' to "zh", 'з' to "z", 'и' to "i",
        'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
        'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'у' to "u", 'ф' to "f", 'х' to "h", 'ц' to "c", 'ч' to "ch",
        'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y", 'ь' to "",
        'э' to "e", 'ю' to "yu", 'я' to "ya",
    )
}

private fun RemoteProfile.toRegionProfile(): RegionProfile {
    val regionId = ProfileIds.fromName(name)
    val rules = ips.map { it to "из удалённой конфигурации" }
    return RegionProfile(
        id = regionId,
        title = name,
        badge = ProfileIds.badge(name),
        description = description.ifBlank { "Профиль из удалённой конфигурации" },
        rules = emptyList(),
        allowedCidrs = rules.map { it.first },
        blockOtherMatchServers = true,
    )
}
