package ru.mlbb.blocker.data

/**
 * Короткий региональный бейдж для строки блоклиста: длинные подписи правил
 * в списке не показываются, вместо них — 3–4 символа.
 */
object RuleRegion {

    enum class Kind {
        MOSCOW,
        FAR_EAST,
        NOVOSIBIRSK,
        OTHER,
        SERVICE,
    }

    data class Badge(val label: String, val kind: Kind)

    fun of(value: String, note: String = ""): Badge? {
        val prefix = value.substringBefore('/')
        val byIp = when {
            prefix.startsWith("154.85.91") -> Badge("МСК", Kind.MOSCOW)
            prefix.startsWith("66.253.15") -> Badge("МСК", Kind.MOSCOW)
            prefix.startsWith("81.177.97") -> Badge("МСК", Kind.MOSCOW)
            prefix.startsWith("146.185.199") -> Badge("НСК", Kind.NOVOSIBIRSK)
            prefix.startsWith("92.223.80") -> Badge("ДВ", Kind.FAR_EAST)
            prefix.startsWith("92.223.91") -> Badge("ЕКБ", Kind.OTHER)
            prefix.startsWith("146.185.197") -> Badge("РНД", Kind.OTHER)
            prefix.startsWith("148.153") -> Badge("ФРА", Kind.OTHER)
            prefix.startsWith("139.45.245") -> Badge("ТБИ", Kind.OTHER)
            prefix.startsWith("300") -> Badge("SRV", Kind.SERVICE)
            else -> null
        }
        if (byIp != null) return byIp

        val text = note.lowercase()
        return when {
            text.contains("моск") -> Badge("МСК", Kind.MOSCOW)
            text.contains("новосиб") -> Badge("НСК", Kind.NOVOSIBIRSK)
            text.contains("хабаров") || text.contains("владивосток") || text.contains("дв") ->
                Badge("ДВ", Kind.FAR_EAST)

            text.contains("сервис") || text.contains("служеб") -> Badge("SRV", Kind.SERVICE)
            text.isNotBlank() -> Badge("IP", Kind.OTHER)
            else -> null
        }
    }
}
