package ru.mlbb.blocker.data.meta

import org.json.JSONObject

enum class MetaLane(val key: String, val title: String) {
    GOLD("gold", "Голд"),
    EXP("exp", "Эксп"),
    MID("mid", "Мид"),
    ROAM("roam", "Роум"),
    JUNGLE("jungle", "Лес");

    companion object {
        fun from(value: String?): MetaLane? {
            val normalized = value?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { it.key == normalized }
        }
    }
}

enum class MetaTier(val key: String, val title: String) {
    S("S", "S"),
    A("A", "A"),
    B("B", "B"),
    C("C", "C"),
    D("D", "D");

    companion object {
        fun from(value: String?): MetaTier {
            val normalized = value?.trim()?.uppercase()?.removeSuffix("+")?.removeSuffix("-")
            return entries.firstOrNull { it.key == normalized } ?: C
        }
    }
}

data class MetaHero(
    val id: String,
    val nameEn: String,
    val nameRu: String,
    val role: String,
    val lane: MetaLane?,
    val lanes: List<MetaLane>,
    val imageUrl: String?,
    val winRate: Double?,
    val pickRate: Double?,
    val banRate: Double?,
    val matches: Int?,
    val tier: MetaTier,
    val sortOrder: Int,
) {
    val displayName: String get() = nameRu.ifBlank { nameEn }

    /** Линии, по которым герой должен показываться в тир-листе. */
    val displayLanes: List<MetaLane>
        get() = lanes.ifEmpty { listOfNotNull(lane) }.ifEmpty { listOf(MetaLane.EXP) }

    val hasStats: Boolean
        get() = winRate != null || pickRate != null || banRate != null

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("nameEn", nameEn)
        put("nameRu", nameRu)
        put("role", role)
        put("lane", lane?.key ?: "")
        put("lanes", lanes.joinToString(",") { it.key })
        put("imageUrl", imageUrl ?: JSONObject.NULL)
        put("winRate", winRate ?: JSONObject.NULL)
        put("pickRate", pickRate ?: JSONObject.NULL)
        put("banRate", banRate ?: JSONObject.NULL)
        put("matches", matches ?: JSONObject.NULL)
        put("tier", tier.key)
        put("sortOrder", sortOrder)
    }
}

data class MetaSnapshot(
    val heroes: List<MetaHero>,
    val region: String,
    val regional: Boolean,
    val patch: String?,
    val updatedAt: String?,
    val fetchedAt: Long,
) {
    val updatedLabel: String
        get() = patch?.takeIf { it.isNotBlank() && !it.equals("current", ignoreCase = true) }
            ?: "актуальная мета"
}

/** Ответ эндпоинта `GET /api/v1/app/meta`. */
data class MetaCounters(
    val heroId: String,
    val names: List<String>,
    val ids: List<String>,
)
