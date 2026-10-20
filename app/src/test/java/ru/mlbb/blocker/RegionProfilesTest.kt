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
        }
    }

    @Test
    fun moscowProfileCoversAllObservedMoscowProbes() {
        val values = RegionProfiles.MOSCOW.rules.map { it.first }.toSet()
        assertTrue("154.85.91.0/24" in values)
        assertTrue("66.253.15.0/24" in values)
        assertTrue("81.177.97.0/24" in values)
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
    }

    @Test
    fun novosibirskProfileIsSeparate() {
        assertEquals(listOf("146.185.199.0/24"), RegionProfiles.NOVOSIBIRSK.rules.map { it.first })
    }
}
