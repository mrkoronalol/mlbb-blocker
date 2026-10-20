package ru.mlbb.blocker.data

data class RegionProfile(
    val id: String,
    val title: String,
    val badge: String,
    val description: String,
    val rules: List<Pair<String, String>>,
    val allowedCidrs: List<String> = emptyList(),
    val blockOtherMatchServers: Boolean = false,
)

object RegionProfiles {

    const val ID_FAR_EAST = "far_east"
    const val ID_MOSCOW = "moscow"
    const val ID_NOVOSIBIRSK = "novosibirsk"

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
        id = ID_MOSCOW,
        title = "Москва",
        badge = "МСК",
        description = "Только московские матч-серверы. Остальные матч-серверы блокируются; логин и служебный трафик работают.",
        rules = NOVOSIBIRSK_SERVERS + OTHER_REGIONS + FAR_EAST_SERVERS,
        allowedCidrs = MOSCOW_SERVERS.map { it.first },
        blockOtherMatchServers = true,
    )

    val FAR_EAST = RegionProfile(
        id = ID_FAR_EAST,
        title = "ДВ",
        badge = "ДВ",
        description = "Блокировать всё, кроме ДВ: Москву, Новосибирск, Екатеринбург, Ростов, Абакан, Франкфурт, Тбилиси, Вьентьян. Хабаровск и Владивосток остаются.",
        rules = MOSCOW_SERVERS + NOVOSIBIRSK_SERVERS + OTHER_REGIONS,
    )

    val NOVOSIBIRSK = RegionProfile(
        id = ID_NOVOSIBIRSK,
        title = "Новосибирск",
        badge = "НСК",
        description = "Только новосибирские матч-серверы. Остальные матч-серверы блокируются; логин и служебный трафик работают.",
        rules = MOSCOW_SERVERS + OTHER_REGIONS + FAR_EAST_SERVERS,
        allowedCidrs = NOVOSIBIRSK_SERVERS.map { it.first },
        blockOtherMatchServers = true,
    )

    val all = listOf(MOSCOW, FAR_EAST, NOVOSIBIRSK)

    fun byId(id: String): RegionProfile? = all.firstOrNull { it.id == id }
}
