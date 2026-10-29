package ru.mlbb.blocker.data

import ru.mlbb.blocker.data.rules.RuleConfigRepository

object StartupEndpoints {

    val MAIN_IP: String
        get() = RuleConfigRepository.current.startup.firstOrNull()?.ip
            ?: ru.mlbb.blocker.data.rules.BuiltInRules.config.startup.first().ip

    val MAIN_PORT: Int
        get() = RuleConfigRepository.current.startup.firstOrNull()?.port
            ?: ru.mlbb.blocker.data.rules.BuiltInRules.config.startup.first().port

    val CIDRS: List<String>
        get() = RuleConfigRepository.current.startup.map { it.cidr }

    fun isMainConnection(ip: String, port: Int): Boolean {
        return RuleConfigRepository.current.startup.any { it.ip == ip && it.port == port }
    }
}
