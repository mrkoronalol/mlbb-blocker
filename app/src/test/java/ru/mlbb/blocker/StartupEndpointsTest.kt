package ru.mlbb.blocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.StartupEndpoints

class StartupEndpointsTest {

    @Test
    fun recognizesMainConnectionServer() {
        assertTrue(StartupEndpoints.isMainConnection("154.85.91.151", 10003))
        assertFalse(StartupEndpoints.isMainConnection("154.85.91.151", 443))
        assertFalse(StartupEndpoints.isMainConnection("154.85.91.150", 10003))
    }
}
