package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.Cidr
import ru.mlbb.blocker.data.RegionProfiles

class RegionProfilesTest {

    @Test
    fun allProfileRulesAreValid() {
        for (profile in RegionProfiles.all) {
            for ((value, _) in profile.rules) {
                assertEquals(value, Cidr.normalize(value))
            }
            for (value in profile.allowedCidrs) {
                assertEquals(value, Cidr.normalize(value))
            }
        }
    }

    @Test
    fun moscowProfileAllowsMoscowAndBlocksEverythingElse() {
        val allowed = RegionProfiles.MOSCOW.allowedCidrs.toSet()
        assertEquals(
            setOf("154.85.91.0/24", "66.253.15.0/24", "81.177.97.0/24"),
            allowed,
        )
        val blocked = RegionProfiles.MOSCOW.rules.map { it.first }.toSet()
        assertTrue(blocked.none { it in allowed })
        assertTrue("146.185.199.0/24" in blocked)
        assertTrue("92.223.80.0/24" in blocked)
        assertTrue(RegionProfiles.MOSCOW.blockOtherMatchServers)
    }

    @Test
    fun farEastProfileBlocksOtherRegionsButNotKhabarovsk() {
        val values = RegionProfiles.FAR_EAST.rules.map { it.first }.toSet()
        assertTrue("154.85.91.0/24" in values)
        assertTrue("146.185.199.0/24" in values)
        assertTrue("92.223.91.0/24" in values)
        assertTrue("148.153.82.0/24" in values)
        assertTrue("148.153.35.0/24" in values)
        assertTrue("148.153.50.0/24" in values)
        assertFalse(values.any { it.startsWith("92.223.80.") })
        assertFalse(RegionProfiles.FAR_EAST.blockOtherMatchServers)
    }

    @Test
    fun novosibirskProfileAllowsOnlyNovosibirsk() {
        assertEquals(listOf("146.185.199.0/24"), RegionProfiles.NOVOSIBIRSK.allowedCidrs)
        val blocked = RegionProfiles.NOVOSIBIRSK.rules.map { it.first }.toSet()
        assertFalse("146.185.199.0/24" in blocked)
        assertTrue("154.85.91.0/24" in blocked)
        assertTrue(RegionProfiles.NOVOSIBIRSK.blockOtherMatchServers)
    }
}
