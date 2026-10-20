package ru.mlbb.blocker.data

object GamePorts {
    fun isGamePort(port: Int): Boolean {
        return port == 30190 || isMatchPort(port)
    }

    fun isMatchPort(port: Int): Boolean {
        return port == 10003 || port in 5000..5054
    }
}
