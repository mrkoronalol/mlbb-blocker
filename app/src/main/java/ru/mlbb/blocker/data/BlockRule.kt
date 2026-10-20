package ru.mlbb.blocker.data

import org.json.JSONObject
import java.util.UUID

data class BlockRule(
    val id: String,
    val value: String,
    val note: String = "",
    val enabled: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("value", value)
        .put("note", note)
        .put("enabled", enabled)

    companion object {
        fun create(value: String, note: String = ""): BlockRule? {
            val normalized = Cidr.normalize(value) ?: return null
            return BlockRule(
                id = UUID.randomUUID().toString(),
                value = normalized,
                note = note.trim(),
            )
        }

        fun fromJson(json: JSONObject): BlockRule? {
            val normalized = Cidr.normalize(json.optString("value")) ?: return null
            return BlockRule(
                id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                value = normalized,
                note = json.optString("note"),
                enabled = json.optBoolean("enabled", true),
            )
        }
    }
}
