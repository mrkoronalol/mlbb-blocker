package ru.mlbb.blocker.data

import org.json.JSONObject
import java.util.UUID

data class AllowRule(
    val id: String,
    val value: String,
    val note: String = "",
    val expiresAt: Long = 0,
) {
    fun isActive(now: Long = System.currentTimeMillis()): Boolean {
        return expiresAt == 0L || expiresAt > now
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("value", value)
        .put("note", note)
        .put("expiresAt", expiresAt)

    companion object {
        fun create(value: String, note: String = "", ttlMs: Long = 0): AllowRule? {
            val normalized = Cidr.normalize(value) ?: return null
            return AllowRule(
                id = UUID.randomUUID().toString(),
                value = normalized,
                note = note.trim(),
                expiresAt = if (ttlMs > 0) System.currentTimeMillis() + ttlMs else 0,
            )
        }

        fun fromJson(json: JSONObject): AllowRule? {
            val normalized = Cidr.normalize(json.optString("value")) ?: return null
            return AllowRule(
                id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                value = normalized,
                note = json.optString("note"),
                expiresAt = json.optLong("expiresAt", 0),
            )
        }
    }
}
