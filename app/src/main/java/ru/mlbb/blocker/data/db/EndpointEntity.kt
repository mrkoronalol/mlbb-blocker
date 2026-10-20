package ru.mlbb.blocker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import ru.mlbb.blocker.data.ConnectionStat

@Entity(
    tableName = "endpoints",
    primaryKeys = ["network", "ip", "port"],
    indices = [
        Index("lastSeen"),
        Index("city"),
        Index("blockedAttempts"),
    ],
)
data class EndpointEntity(
    val network: String,
    val ip: String,
    val port: Int,
    val host: String?,
    val firstSeen: Long,
    val lastSeen: Long,
    val sessions: Int,
    val pingMs: Long?,
    val pingJitterMs: Long?,
    val pingKind: String?,
    val pingAt: Long,
    val blockedAttempts: Int = 0,
    val city: String? = null,
    val country: String? = null,
    @ColumnInfo(name = "geoAt")
    val geoAt: Long = 0,
)

fun EndpointEntity.toConnectionStat(): ConnectionStat {
    return ConnectionStat(
        ip = ip,
        port = port,
        host = host,
        network = network,
        firstSeen = firstSeen,
        lastSeen = lastSeen,
        observations = sessions,
        pingMs = pingMs,
        pingJitterMs = pingJitterMs,
        pingKind = pingKind,
        pingAt = pingAt,
        city = city,
        country = country,
    )
}
