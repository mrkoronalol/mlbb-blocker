package ru.mlbb.blocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.GamePorts

class GamePortsTest {

    @Test
    fun recognizesObservedMatchAndRelayPorts() {
        listOf(10003, 10011, 10013, 10063, 5003, 5053, 5507, 5557, 5058).forEach { port ->
            assertTrue("port $port should be a match port", GamePorts.isMatchPort(port))
        }
        assertFalse(GamePorts.isMatchPort(443))
        assertFalse(GamePorts.isMatchPort(30190))
    }

    @Test
    fun servicePortsRemainSafe() {
        listOf(30021, 30071, 30190, 30220).forEach { port ->
            assertTrue(GamePorts.isSafePort(port))
        }
    }
}
