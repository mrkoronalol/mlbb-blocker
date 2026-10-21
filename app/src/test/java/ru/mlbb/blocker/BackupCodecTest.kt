package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.AllowRule
import ru.mlbb.blocker.data.BlockRule
import ru.mlbb.blocker.data.backup.BackupCodec
import ru.mlbb.blocker.data.backup.BackupData
import ru.mlbb.blocker.data.backup.BackupSettings
import ru.mlbb.blocker.data.db.BlockedAttemptEntity
import ru.mlbb.blocker.data.db.EndpointEntity
import ru.mlbb.blocker.data.db.MatchSessionEntity
import ru.mlbb.blocker.data.db.PingSampleEntity

class BackupCodecTest {

    @Test
    fun roundTripKeepsSettingsAndHistory() {
        val data = BackupData(
            appVersion = "0.12.0",
            createdAt = 1_700_000_000_000L,
            selectedProfile = "moscow",
            settings = BackupSettings(
                discoveryMode = false,
                fastReject = true,
                matchAlerts = true,
                hideStartupAlerts = true,
                autoStart = true,
                onboardingDone = true,
            ),
            rules = listOf(BlockRule("1", "154.85.91.0/24", "Moscow")),
            allowRules = listOf(AllowRule("2", "154.85.91.151/32", "server", 0)),
            endpoints = listOf(
                EndpointEntity(
                    network = "tcp",
                    ip = "154.85.91.151",
                    port = 10003,
                    host = null,
                    firstSeen = 1,
                    lastSeen = 2,
                    sessions = 3,
                    pingMs = 126,
                    pingJitterMs = 4,
                    pingKind = "TCP",
                    pingAt = 2,
                    blockedAttempts = 0,
                    city = "Москва",
                    country = "RU",
                    geoAt = 2,
                ),
            ),
            blockedAttempts = listOf(
                BlockedAttemptEntity(
                    timestamp = 2,
                    ip = "92.223.91.7",
                    port = 30190,
                    network = "udp",
                    matchedProfile = "sink",
                ),
            ),
            matchSessions = listOf(
                MatchSessionEntity(
                    startedAt = 3,
                    endedAt = 4,
                    profiles = "moscow",
                    gamePackage = "com.mobile.legends",
                    result = "win",
                ),
            ),
            pingSamples = listOf(
                PingSampleEntity(
                    endpointKey = "tcp://154.85.91.151:10003",
                    timestamp = 4,
                    ms = 126,
                    jitterMs = 4,
                    kind = "TCP",
                ),
            ),
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(data))
        assertEquals("0.12.0", decoded.appVersion)
        assertEquals("moscow", decoded.selectedProfile)
        assertEquals(1, decoded.rules.size)
        assertEquals(1, decoded.allowRules.size)
        assertEquals(1, decoded.endpoints.size)
        assertEquals(1, decoded.blockedAttempts.size)
        assertEquals(1, decoded.matchSessions.size)
        assertEquals(1, decoded.pingSamples.size)
        assertEquals("Москва", decoded.endpoints.first().city)
        assertTrue(decoded.settings.autoStart)
    }
}
