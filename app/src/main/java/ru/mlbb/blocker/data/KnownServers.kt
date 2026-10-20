package ru.mlbb.blocker.data

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

object KnownServers {

    private val exact = mapOf(
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

    private val subnet24 = mapOf(
        "92.223.80" to KnownServer("Хабаровск", "≈26–38 мс", KnownServerKind.FAR_EAST),
        "154.85.91" to KnownServer("Москва", "≈126 мс", KnownServerKind.MOSCOW),
        "92.223.91" to KnownServer("Екатеринбург", "≈116 мс", KnownServerKind.OTHER_REGION),
        "146.185.199" to KnownServer("Новосибирск", "≈84 мс", KnownServerKind.OTHER_REGION),
        "146.185.197" to KnownServer("Ростов-на-Дону", "≈148 мс", KnownServerKind.OTHER_REGION),
    )

    fun lookup(ip: String): KnownServer? {
        exact[ip]?.let { return it }
        if (!Cidr.isIpv4(ip)) return null
        val prefix = ip.substringBeforeLast('.')
        return subnet24[prefix]
    }
}
