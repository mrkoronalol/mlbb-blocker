package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.vpn.MatchCardPolicy
import ru.mlbb.blocker.vpn.MatchCardPolicy.Settings

class MatchCardPolicyTest {

    private fun settings(
        blocked: Boolean = false,
        hideStartup: Boolean = false,
        stableOnly: Boolean = false,
    ) = Settings(blocked, hideStartup, stableOnly)

    @Test
    fun blockedServerIsShownAfterShortDelayWithoutLivenessCheck() {
        val blocked = settings(blocked = true, hideStartup = true, stableOnly = true)
        assertEquals(MatchCardPolicy.BLOCKED_DELAY_MS, MatchCardPolicy.delayMs(blocked))
        assertFalse(MatchCardPolicy.needsLivenessCheck(blocked))
        assertTrue(MatchCardPolicy.shouldShow(blocked, muted = false, stillActive = false))
    }

    @Test
    fun allowedServerIsShownImmediatelyWhenFiltersDisabled() {
        val plain = settings()
        assertEquals(0L, MatchCardPolicy.delayMs(plain))
        assertFalse(MatchCardPolicy.needsLivenessCheck(plain))
        assertTrue(MatchCardPolicy.shouldShow(plain, muted = false, stillActive = false))
    }

    @Test
    fun startupFilterUsesShortDelay() {
        val startup = settings(hideStartup = true)
        assertEquals(MatchCardPolicy.STARTUP_DELAY_MS, MatchCardPolicy.delayMs(startup))
        assertTrue(MatchCardPolicy.needsLivenessCheck(startup))
        assertFalse(MatchCardPolicy.shouldShow(startup, muted = false, stillActive = false))
        assertTrue(MatchCardPolicy.shouldShow(startup, muted = false, stillActive = true))
    }

    @Test
    fun stableOnlyFilterWaitsLongerAndWinsOverStartupFilter() {
        val stable = settings(hideStartup = true, stableOnly = true)
        assertEquals(MatchCardPolicy.STABLE_DELAY_MS, MatchCardPolicy.delayMs(stable))
        assertTrue(MatchCardPolicy.needsLivenessCheck(stable))
        assertFalse(MatchCardPolicy.shouldShow(stable, muted = false, stillActive = false))
    }

    @Test
    fun mutedServerNeverShowsCard() {
        assertFalse(MatchCardPolicy.shouldShow(settings(), muted = true, stillActive = true))
        assertFalse(
            MatchCardPolicy.shouldShow(settings(hideStartup = true), muted = true, stillActive = true),
        )
    }

    @Test
    fun stableDelayIsLongerThanStartupDelay() {
        assertTrue(MatchCardPolicy.STABLE_DELAY_MS > MatchCardPolicy.STARTUP_DELAY_MS)
        assertTrue(MatchCardPolicy.STARTUP_DELAY_MS > MatchCardPolicy.BLOCKED_DELAY_MS)
    }
}
