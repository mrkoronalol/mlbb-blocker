package ru.mlbb.blocker.data

object GamePorts {
    const val MATCH_PORT = 10003
    const val MATCH_PORT_RANGE = "5000:5054"

    val MATCH_PORTS: Set<Int> = setOf(MATCH_PORT) + (5000..5054)
    val SAFE_PORTS: Set<Int> = setOf(30021, 30071, 30190, 30220)
    val SAFE_PORT_LIST: List<Int> = listOf(30190, 30021, 30071, 30220)

    fun isGamePort(port: Int): Boolean {
        return port == 30190 || isMatchPort(port)
    }

    fun isMatchPort(port: Int): Boolean {
        return port in MATCH_PORTS
    }

    fun isSafePort(port: Int): Boolean {
        return port in SAFE_PORTS
    }
}
