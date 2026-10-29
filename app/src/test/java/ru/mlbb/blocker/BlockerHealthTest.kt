package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.BlockerHealth
import ru.mlbb.blocker.data.HealthIssue
import ru.mlbb.blocker.data.HealthSnapshot

class BlockerHealthTest {

    @Test
    fun reportsStoppedServiceWhenItWasExpected() {
        val issues = BlockerHealth.issues(
            HealthSnapshot(expectedRunning = true, running = false),
        )
        assertEquals(listOf(HealthIssue.SERVICE_STOPPED), issues)
    }

    @Test
    fun staysQuietWhenUserStoppedIt() {
        val issues = BlockerHealth.issues(
            HealthSnapshot(expectedRunning = false, running = false),
        )
        assertTrue(issues.isEmpty())
    }

    @Test
    fun staysQuietWhenVpnPermissionMissingOrMlbbGone() {
        assertTrue(
            BlockerHealth.issues(
                HealthSnapshot(expectedRunning = true, running = false, vpnPermissionGranted = false),
            ).isEmpty(),
        )
        assertTrue(
            BlockerHealth.issues(
                HealthSnapshot(expectedRunning = true, running = false, mlbbInstalled = false),
            ).isEmpty(),
        )
    }

    @Test
    fun reportsBrokenGeoIpAndRules() {
        val issues = BlockerHealth.issues(
            HealthSnapshot(
                expectedRunning = true,
                running = true,
                geoIpReady = false,
                rulesFallback = true,
            ),
        )
        assertEquals(listOf(HealthIssue.GEOIP_FAILED, HealthIssue.RULES_FALLBACK), issues)
    }

    @Test
    fun mapsIssuesToRecoveryActions() {
        assertEquals(
            BlockerHealth.RecoveryAction.START_VPN,
            BlockerHealth.recoveryAction(HealthIssue.SERVICE_STOPPED),
        )
        assertEquals(
            BlockerHealth.RecoveryAction.RELOAD_GEOIP,
            BlockerHealth.recoveryAction(HealthIssue.GEOIP_FAILED),
        )
        assertEquals(
            BlockerHealth.RecoveryAction.REFRESH_RULES,
            BlockerHealth.recoveryAction(HealthIssue.RULES_FALLBACK),
        )
    }
}
