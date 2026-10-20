package ru.mlbb.blocker.data

object StartupEndpoints {
    const val MAIN_IP = "154.85.91.151"
    const val MAIN_PORT = 10003

    val CIDRS = listOf("$MAIN_IP/32")

    fun isMainConnection(ip: String, port: Int): Boolean {
        return ip == MAIN_IP && port == MAIN_PORT
    }
}
