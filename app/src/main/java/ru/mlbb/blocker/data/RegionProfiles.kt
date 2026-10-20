package ru.mlbb.blocker.data

data class RegionProfile(
    val id: String,
    val title: String,
    val description: String,
    val rules: List<Pair<String, String>>,
)

object RegionProfiles {

    const val ID_FAR_EAST = "far_east"
    const val ID_MOSCOW = "moscow"
    const val ID_NOVOSIBIRSK = "novosibirsk"

    val MOSCOW = RegionProfile(
        id = ID_MOSCOW,
        title = "МСК",
        description = "Блокировать московские серверы: 154.85.91.0/24, 66.253.15.83, 81.177.97.5.",
        rules = listOf(
            "154.85.91.0/24" to "Москва, матч-сервер",
            "66.253.15.0/24" to "Москва, UDP-пробы 66.253.15.82–85",
            "81.177.97.0/24" to "Москва, UDP-проба",
        ),
    )

    val NOVOSIBIRSK = RegionProfile(
        id = ID_NOVOSIBIRSK,
        title = "Новосибирск",
        description = "Блокировать новосибирские серверы: 146.185.199.0/24.",
        rules = listOf(
            "146.185.199.0/24" to "Новосибирск",
        ),
    )

    val FAR_EAST = RegionProfile(
        id = ID_FAR_EAST,
        title = "ДВ",
        description = "Блокировать всё, кроме ДВ: Москву, Новосибирск, Екатеринбург, Ростов, Абакан, Франкфурт, Тбилиси, Вьентьян. Хабаровск и Владивосток остаются.",
        rules = MOSCOW.rules + NOVOSIBIRSK.rules + listOf(
            "92.223.91.0/24" to "Екатеринбург",
            "146.185.197.0/24" to "Ростов-на-Дону",
            "90.189.118.10/32" to "Абакан",
            "148.153.82.0/24" to "Франкфурт, игровые пробы",
            "148.153.35.0/24" to "Франкфурт, игровые пробы",
            "148.153.50.0/24" to "Франкфурт, игровые пробы",
            "139.45.245.2/32" to "Тбилиси",
            "202.137.128.134/32" to "Вьентьян",
        ),
    )

    val all = listOf(FAR_EAST, MOSCOW, NOVOSIBIRSK)

    fun byId(id: String): RegionProfile? = all.firstOrNull { it.id == id }
}
