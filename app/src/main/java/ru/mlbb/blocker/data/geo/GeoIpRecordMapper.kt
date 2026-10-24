package ru.mlbb.blocker.data.geo

import java.util.Locale

object GeoIpRecordMapper {

    fun city(record: Map<*, *>): String? {
        val city = record["city"]
        if (city is String) return translateCity(city)
        val cityMap = city as? Map<*, *> ?: return null
        val names = cityMap["names"] as? Map<*, *>
        val ru = names?.get("ru") as? String
        val en = names?.get("en") as? String
        return translateCity(ru ?: en)
    }

    fun countryCode(record: Map<*, *>): String? {
        return ((record["country"] as? Map<*, *>)?.get("iso_code") as? String)
            ?: record["country_code"] as? String
    }

    fun translateCity(name: String?): String? {
        if (name.isNullOrBlank()) return null
        return when (name.substringBefore(" (").trim().lowercase(Locale.ROOT)) {
            "moscow" -> "Москва"
            "novosibirsk" -> "Новосибирск"
            "yekaterinburg", "ekaterinburg" -> "Екатеринбург"
            "rostov-on-don", "rostov na donu" -> "Ростов-на-Дону"
            "khabarovsk" -> "Хабаровск"
            "vladivostok" -> "Владивосток"
            "abakan" -> "Абакан"
            "frankfurt am main", "frankfurt" -> "Франкфурт"
            "tbilisi" -> "Тбилиси"
            "vientiane" -> "Вьентьян"
            else -> name
        }
    }
}
