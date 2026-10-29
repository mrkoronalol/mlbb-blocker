package ru.mlbb.blocker.data.rules

import ru.mlbb.blocker.data.KnownServer
import ru.mlbb.blocker.data.KnownServerKind
import ru.mlbb.blocker.data.RegionProfile
import ru.mlbb.blocker.data.RegionProfiles

data class StartupEndpoint(
    val ip: String,
    val port: Int,
    val label: String = "",
) {
    val cidr: String get() = "$ip/32"
}

/**
 * Единый набор правил: матч-порты, сервисные порты, стартовые серверы, профили
 * и известные серверы. Может приходить из assets, с GitHub-релиза или из
 * встроенных значений, если ничего не загрузилось.
 */
data class RulesConfig(
    val schemaVersion: Int,
    val updatedAt: String,
    val matchPorts: List<Int>,
    val matchPortRanges: List<String>,
    val safePorts: List<Int>,
    val startup: List<StartupEndpoint>,
    val profiles: List<RegionProfile>,
    val knownExact: Map<String, KnownServer>,
    val knownSubnets: Map<String, KnownServer>,
) {
    val matchPortSet: Set<Int> = buildSet {
        addAll(matchPorts)
        matchPortRanges.forEach { range -> expandRange(range).forEach(::add) }
    }

    val safePortSet: Set<Int> = safePorts.toSet()

    val mainPort: Int get() = matchPorts.firstOrNull() ?: 10003

    fun profile(id: String): RegionProfile? = profiles.firstOrNull { it.id == id }

    companion object {
        const val SCHEMA_VERSION = 1

        fun expandRange(range: String): IntRange {
            val parts = range.split(':', '-', '/')
            if (parts.size != 2) return IntRange.EMPTY
            val from = parts[0].trim().toIntOrNull() ?: return IntRange.EMPTY
            val to = parts[1].trim().toIntOrNull() ?: return IntRange.EMPTY
            if (from < 1 || to > 65535 || from > to) return IntRange.EMPTY
            return from..to
        }
    }
}

/**
 * Встроенный набор правил. Используется как fallback и как эталон для тестов.
 */
object BuiltInRules {

    val MOSCOW_SERVERS = listOf(
        "154.85.91.0/24" to "Москва, матч-сервер",
        "66.253.15.0/24" to "Москва, UDP-пробы 66.253.15.82–85",
        "81.177.97.0/24" to "Москва, UDP-проба",
    )

    val NOVOSIBIRSK_SERVERS = listOf(
        "146.185.199.0/24" to "Новосибирск",
    )

    val FAR_EAST_SERVERS = listOf(
        "92.223.80.0/24" to "Хабаровск/ДВ",
    )

    val OTHER_REGIONS = listOf(
        "92.223.91.0/24" to "Екатеринбург",
        "146.185.197.0/24" to "Ростов-на-Дону",
        "90.189.118.10/32" to "Абакан",
        "148.153.82.0/24" to "Франкфурт, игровые пробы",
        "148.153.35.0/24" to "Франкфурт, игровые пробы",
        "148.153.50.0/24" to "Франкфурт, игровые пробы",
        "139.45.245.2/32" to "Тбилиси",
        "202.137.128.134/32" to "Вьентьян",
    )

    val MOSCOW = RegionProfile(
        id = RegionProfiles.ID_MOSCOW,
        title = "Москва",
        badge = "МСК",
        description = "Только московские матч-серверы. Остальные матч-серверы блокируются; логин и служебный трафик работают.",
        rules = NOVOSIBIRSK_SERVERS + OTHER_REGIONS + FAR_EAST_SERVERS,
        allowedCidrs = MOSCOW_SERVERS.map { it.first },
        blockOtherMatchServers = true,
    )

    val FAR_EAST = RegionProfile(
        id = RegionProfiles.ID_FAR_EAST,
        title = "ДВ",
        badge = "ДВ",
        description = "Блокировать всё, кроме ДВ: Москву, Новосибирск, Екатеринбург, Ростов, Абакан, Франкфурт, Тбилиси, Вьентьян. Хабаровск и Владивосток остаются.",
        rules = MOSCOW_SERVERS + NOVOSIBIRSK_SERVERS + OTHER_REGIONS,
    )

    val NOVOSIBIRSK = RegionProfile(
        id = RegionProfiles.ID_NOVOSIBIRSK,
        title = "Новосибирск",
        badge = "НСК",
        description = "Только новосибирские матч-серверы. Остальные матч-серверы блокируются; логин и служебный трафик работают.",
        rules = MOSCOW_SERVERS + OTHER_REGIONS + FAR_EAST_SERVERS,
        allowedCidrs = NOVOSIBIRSK_SERVERS.map { it.first },
        blockOtherMatchServers = true,
    )

    val PROFILES = listOf(MOSCOW, FAR_EAST, NOVOSIBIRSK)

    val KNOWN_EXACT = mapOf(
        "66.253.15.83" to KnownServer("Москва", "нет ответа", KnownServerKind.MOSCOW),
        "81.177.97.5" to KnownServer("Москва", "нет ответа", KnownServerKind.MOSCOW),
        "90.189.118.10" to KnownServer("Абакан", "нет ответа", KnownServerKind.OTHER_REGION),
        "148.153.82.69" to KnownServer("Франкфурт", "≈?", KnownServerKind.OTHER_REGION),
        "139.45.245.2" to KnownServer("Тбилиси", "нет ответа", KnownServerKind.OTHER_REGION),
        "202.137.128.134" to KnownServer("Вьентьян", "нет ответа", KnownServerKind.OTHER_REGION),
        "161.202.218.126" to KnownServer("Сингапур", null, KnownServerKind.SERVICE),
        "161.202.213.218" to KnownServer("Сингапур", null, KnownServerKind.SERVICE),
        "119.81.63.227" to KnownServer("Сингапур", null, KnownServerKind.SERVICE),
        "2.19.183.40" to KnownServer("CDN", null, KnownServerKind.CDN),
        "2.23.167.186" to KnownServer("CDN", null, KnownServerKind.CDN),
        "23.53.32.45" to KnownServer("CDN", null, KnownServerKind.CDN),
        "185.151.204.13" to KnownServer("CDN", null, KnownServerKind.CDN),
        "172.217.117.4" to KnownServer("CDN", null, KnownServerKind.CDN),
    )

    val KNOWN_SUBNETS = mapOf(
        "92.223.80" to KnownServer("Хабаровск", "≈26–38 мс", KnownServerKind.FAR_EAST),
        "154.85.91" to KnownServer("Москва", "≈126 мс", KnownServerKind.MOSCOW),
        "92.223.91" to KnownServer("Екатеринбург", "≈116 мс", KnownServerKind.OTHER_REGION),
        "146.185.199" to KnownServer("Новосибирск", "≈84 мс", KnownServerKind.OTHER_REGION),
        "146.185.197" to KnownServer("Ростов-на-Дону", "≈148 мс", KnownServerKind.OTHER_REGION),
    )

    val config = RulesConfig(
        schemaVersion = RulesConfig.SCHEMA_VERSION,
        updatedAt = "built-in",
        matchPorts = listOf(10003),
        matchPortRanges = listOf("5000:5058", "5500:5599", "10011:10013", "10063:10063"),
        safePorts = listOf(30021, 30071, 30190, 30220),
        startup = listOf(StartupEndpoint("154.85.91.151", 10003, "Основной сервер")),
        profiles = PROFILES,
        knownExact = KNOWN_EXACT,
        knownSubnets = KNOWN_SUBNETS,
    )
}
