package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.RegionProfiles
import ru.mlbb.blocker.data.remote.ProfileIds
import ru.mlbb.blocker.data.remote.RemoteAppConfig
import ru.mlbb.blocker.data.remote.VersionCompare

class RemoteConfigTest {

    private val payload = """
        {
          "app_version": "0.18.0",
          "maintenance": false,
          "notice": "Внимание! Обновлены профили Москвы",
          "feature_meta_enabled": true,
          "min_app_version": "0.18.0",
          "update_required": true,
          "profiles": [
            { "id": "p1", "name": "Москва", "description": "МСК", "ips": ["154.85.91.0/24", "66.253.15.0/24"] },
            { "id": "p2", "name": "ДВ", "description": "", "ips": ["92.223.80.0/24"] },
            { "id": "p3", "name": "Токио", "description": "Азия", "ips": ["203.0.113.0/24"] }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesRemoteConfig() {
        val config = RemoteAppConfig.parse(payload)
        requireNotNull(config)
        assertFalse(config.maintenance)
        assertEquals("Внимание! Обновлены профили Москвы", config.notice)
        assertTrue(config.featureMetaEnabled)
        assertEquals("0.18.0", config.minAppVersion)
        assertTrue(config.updateRequired)
        assertEquals(3, config.profiles.size)
        assertEquals(listOf("154.85.91.0/24", "66.253.15.0/24"), config.profiles.first().ips)
    }

    @Test
    fun mapsRemoteProfilesToStableRegionIds() {
        val config = requireNotNull(RemoteAppConfig.parse(payload))
        val profiles = config.regionProfiles()
        assertEquals(RegionProfiles.ID_MOSCOW, profiles[0].id)
        assertEquals("МСК", profiles[0].badge)
        assertEquals(listOf("154.85.91.0/24", "66.253.15.0/24"), profiles[0].allowedCidrs)
        assertTrue(profiles[0].blockOtherMatchServers)
        assertEquals(RegionProfiles.ID_FAR_EAST, profiles[1].id)
        assertEquals("ДВ", profiles[1].badge)
        assertEquals("tokio", profiles[2].id)
    }

    @Test
    fun keepsDefaultsForEmptyResponse() {
        val config = requireNotNull(RemoteAppConfig.parse("{}"))
        assertFalse(config.maintenance)
        assertTrue(config.featureMetaEnabled)
        assertEquals("", config.notice)
        assertTrue(config.profiles.isEmpty())
    }

    @Test
    fun survivesBrokenJson() {
        assertEquals(null, RemoteAppConfig.parse("<html>oops"))
    }

    @Test
    fun mapsKnownProfileNames() {
        assertEquals(RegionProfiles.ID_MOSCOW, ProfileIds.fromName("Москва"))
        assertEquals(RegionProfiles.ID_NOVOSIBIRSK, ProfileIds.fromName("Новосибирск"))
        assertEquals(RegionProfiles.ID_FAR_EAST, ProfileIds.fromName("ДВ"))
        assertEquals("spb", ProfileIds.fromName("СПб"))
        assertEquals("МСК", ProfileIds.badge("Москва"))
        assertEquals("СПБ", ProfileIds.badge("СПб"))
    }

    @Test
    fun comparesVersions() {
        assertTrue(VersionCompare.isLower("0.17.0", "0.18.0"))
        assertFalse(VersionCompare.isLower("0.18.0", "0.18.0"))
        assertFalse(VersionCompare.isLower("0.18.1", "0.18.0"))
        assertFalse(VersionCompare.isLower("", "0.18.0"))
        assertFalse(VersionCompare.isLower("0.18.0", ""))
        assertTrue(VersionCompare.isLower("0.9.9", "0.10.0"))
    }
}
