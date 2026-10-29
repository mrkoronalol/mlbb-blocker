package ru.mlbb.blocker.data

/**
 * Пользовательские решения по незнакомым портам — дополняют набор из rules.json.
 * Заполняется UnknownPortActionReceiver, читается при запуске приложения.
 */
object PortOverrides {

    @Volatile
    var matchPorts: Set<Int> = emptySet()
        private set

    @Volatile
    var safePorts: Set<Int> = emptySet()
        private set

    fun load(store: BlocklistStore) {
        matchPorts = store.extraMatchPorts
        safePorts = store.extraSafePorts
    }

    fun update(match: Set<Int>, safe: Set<Int>) {
        matchPorts = match
        safePorts = safe
    }
}
