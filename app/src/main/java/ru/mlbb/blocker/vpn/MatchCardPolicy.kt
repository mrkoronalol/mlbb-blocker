package ru.mlbb.blocker.vpn

/**
 * Решение, показывать ли карточку матча. Вынесено из MatchAlertManager,
 * чтобы правила можно было проверить unit-тестами.
 */
object MatchCardPolicy {

    const val BLOCKED_DELAY_MS = 600L
    const val STARTUP_DELAY_MS = 2_500L
    const val STABLE_DELAY_MS = 6_000L

    data class Settings(
        val blocked: Boolean,
        val hideStartup: Boolean,
        val stableOnly: Boolean,
    )

    fun delayMs(settings: Settings): Long = when {
        settings.blocked -> BLOCKED_DELAY_MS
        settings.stableOnly -> STABLE_DELAY_MS
        settings.hideStartup -> STARTUP_DELAY_MS
        else -> 0L
    }

    /** Нужно ли перепроверять, что подключение ещё живо, прежде чем показывать карточку. */
    fun needsLivenessCheck(settings: Settings): Boolean {
        if (settings.blocked) return false
        return settings.stableOnly || settings.hideStartup
    }

    fun shouldShow(settings: Settings, muted: Boolean, stillActive: Boolean): Boolean {
        if (muted) return false
        if (settings.blocked) return true
        return if (needsLivenessCheck(settings)) stillActive else true
    }
}
