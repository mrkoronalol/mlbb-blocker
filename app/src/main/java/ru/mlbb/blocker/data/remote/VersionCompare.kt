package ru.mlbb.blocker.data.remote

/**
 * Сравнение версий приложения ("0.18.0", "1.2", "v0.19.0-beta").
 */
object VersionCompare {

    fun isLower(current: String, minimum: String): Boolean {
        if (current.isBlank() || minimum.isBlank()) return false
        val left = parts(current)
        val right = parts(minimum)
        val size = maxOf(left.size, right.size)
        for (index in 0 until size) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a < b
        }
        return false
    }

    private fun parts(value: String): List<Int> = value
        .trim()
        .removePrefix("v")
        .removePrefix("V")
        .split('.', '-', '+')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
