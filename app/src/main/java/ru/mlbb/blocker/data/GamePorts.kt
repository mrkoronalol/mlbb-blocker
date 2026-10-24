package ru.mlbb.blocker.data

object GamePorts {
    const val MATCH_PORT = 10003

    val MATCH_PORT_RANGES: List<String> = listOf(
        "5000:5058",
        "5500:5599",
        "10011:10013",
        "10063:10063",
    )

    val MATCH_PORTS: Set<Int> = buildSet {
        add(MATCH_PORT)
        addAll(5000..5058)
        addAll(5500..5599)
        addAll(10011..10013)
        add(10063)
    }

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
