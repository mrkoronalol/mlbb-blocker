package ru.mlbb.blocker.data

import ru.mlbb.blocker.data.rules.RuleConfigRepository
import ru.mlbb.blocker.data.rules.RulesConfig

object GamePorts {

    /** Основной матч-порт MLBB (по умолчанию 10003). */
    val MATCH_PORT: Int
        get() = RuleConfigRepository.current.mainPort

    /** Диапазоны в синтаксисе sing-box: "5000:5058". */
    val MATCH_PORT_RANGES: List<String>
        get() = RuleConfigRepository.current.matchPortRanges

    val MATCH_PORTS: Set<Int>
        get() = (matchPortSet() + PortOverrides.matchPorts) - safePortSet()

    /** Отдельные порты (не покрытые диапазонами) для правил sing-box. */
    val MATCH_PORT_LIST: List<Int>
        get() {
            val covered = rangePorts()
            return (matchPortSet() + PortOverrides.matchPorts)
                .filterNot { it in covered }
                .minus(safePortSet())
                .sorted()
        }

    val SAFE_PORTS: Set<Int>
        get() = safePortSet()

    val SAFE_PORT_LIST: List<Int>
        get() = safePortSet().sorted()

    fun isGamePort(port: Int): Boolean {
        return port == 30190 || isMatchPort(port)
    }

    fun isMatchPort(port: Int): Boolean {
        if (port in safePortSet()) return false
        return port in matchPortSet() || port in PortOverrides.matchPorts
    }

    fun isSafePort(port: Int): Boolean {
        return port in safePortSet()
    }

    private fun matchPortSet(): Set<Int> = RuleConfigRepository.current.matchPortSet

    private fun safePortSet(): Set<Int> =
        RuleConfigRepository.current.safePortSet + PortOverrides.safePorts

    private fun rangePorts(): Set<Int> = RuleConfigRepository.current.let { config ->
        buildSet {
            config.matchPortRanges.forEach { range -> addAll(RulesConfig.expandRange(range)) }
        }
    }
}
