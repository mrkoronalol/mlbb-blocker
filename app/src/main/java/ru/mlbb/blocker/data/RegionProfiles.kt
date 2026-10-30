package ru.mlbb.blocker.data

import ru.mlbb.blocker.data.rules.BuiltInRules
import ru.mlbb.blocker.data.rules.RuleConfigRepository
import ru.mlbb.blocker.data.remote.RemoteConfigRepository

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

    /** Пресеты для блоклиста остаются встроенными: это удобные заготовки, а не правила маршрутизации. */
    val MOSCOW_SERVERS: List<Pair<String, String>> get() = BuiltInRules.MOSCOW_SERVERS
    val NOVOSIBIRSK_SERVERS: List<Pair<String, String>> get() = BuiltInRules.NOVOSIBIRSK_SERVERS

    val all: List<RegionProfile>
        get() = RemoteConfigRepository.profiles.ifEmpty {
            RuleConfigRepository.current.profiles
        }

    fun byId(id: String): RegionProfile? = all.firstOrNull { it.id == id }

    val MOSCOW: RegionProfile
        get() = byId(ID_MOSCOW) ?: BuiltInRules.MOSCOW

    val FAR_EAST: RegionProfile
        get() = byId(ID_FAR_EAST) ?: BuiltInRules.FAR_EAST

    val NOVOSIBIRSK: RegionProfile
        get() = byId(ID_NOVOSIBIRSK) ?: BuiltInRules.NOVOSIBIRSK
}
