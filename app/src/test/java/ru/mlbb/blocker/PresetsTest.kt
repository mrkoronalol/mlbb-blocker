package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.BlocklistParser
import ru.mlbb.blocker.data.Cidr
import ru.mlbb.blocker.data.KnownServers
import ru.mlbb.blocker.data.KnownServerKind
import ru.mlbb.blocker.data.Presets

class PresetsTest {

    @Test
    fun allPresetRulesAreValid() {
        for (preset in listOf(Presets.MOSCOW, Presets.NOVOSIBIRSK, Presets.NON_FAR_EAST)) {
            for ((value, _) in preset.rules) {
                assertEquals(value, Cidr.normalize(value))
            }
        }
    }

    @Test
    fun nonFarEastPresetDoesNotBlockKhabarovskSubnet() {
        assertFalse(Presets.NON_FAR_EAST.rules.any { it.first.startsWith("92.223.80.") })
        assertTrue(Presets.NON_FAR_EAST.rules.any { it.first == "154.85.91.0/24" })
    }

    @Test
    fun parsesBlocklistLinesAndIgnoresComments() {
        val values = BlocklistParser.parseLines(
            """
            # Moscow
            154.85.91.0/24
            66.253.15.83 # match server
            // invalid
            not-an-ip
            154.85.91.0/24
            """.trimIndent(),
        )
        assertEquals(listOf("154.85.91.0/24", "66.253.15.83/32"), values)
    }

    @Test
    fun recognizesKnownServerCities() {
        assertEquals(KnownServerKind.FAR_EAST, KnownServers.lookup("92.223.80.65")?.kind)
        assertEquals("Хабаровск", KnownServers.lookup("92.223.80.21")?.city)
        assertEquals(KnownServerKind.MOSCOW, KnownServers.lookup("154.85.91.153")?.kind)
        assertEquals(KnownServerKind.OTHER_REGION, KnownServers.lookup("146.185.199.88")?.kind)
    }
}
