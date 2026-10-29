package ru.mlbb.blocker.data

import ru.mlbb.blocker.data.rules.RuleConfigRepository

enum class KnownServerKind {
    FAR_EAST,
    MOSCOW,
    OTHER_REGION,
    SERVICE,
    CDN,
}

data class KnownServer(
    val city: String,
    val latency: String?,
    val kind: KnownServerKind,
)

/**
 * Известные серверы MLBB. Набор приходит из rules.json, пока он не загружен —
 * используются встроенные значения.
 */
object KnownServers {

    fun lookup(ip: String): KnownServer? {
        val config = RuleConfigRepository.current
        config.knownExact[ip]?.let { return it }
        if (!Cidr.isIpv4(ip)) return null
        val prefix = ip.substringBeforeLast('.')
        return config.knownSubnets[prefix]
    }

    fun exactCount(): Int = RuleConfigRepository.current.knownExact.size

    fun subnetCount(): Int = RuleConfigRepository.current.knownSubnets.size
}
