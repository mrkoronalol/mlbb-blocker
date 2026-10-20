package ru.mlbb.blocker.data

data class ConnectionSnapshot(
    val id: String,
    val ip: String,
    val port: Int,
    val host: String?,
    val network: String,
)

data class ConnectionStat(
    val ip: String,
    val port: Int,
    val host: String?,
    val network: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val observations: Int,
    val pingMs: Long? = null,
    val pingJitterMs: Long? = null,
    val pingKind: String? = null,
    val pingAt: Long = 0,
    val city: String? = null,
    val country: String? = null,
) {
    val key: String get() = "$network://$ip:$port"
    val displayAddress: String get() = if (ip.contains(':')) "[$ip]:$port" else "$ip:$port"
}
