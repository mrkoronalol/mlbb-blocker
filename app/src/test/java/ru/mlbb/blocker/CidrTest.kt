package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.Cidr

class CidrTest {

    @Test
    fun normalizesSingleIpv4() {
        assertEquals("92.223.80.21/32", Cidr.normalize("92.223.80.21"))
    }

    @Test
    fun normalizesNetworkAddress() {
        assertEquals("154.85.91.0/24", Cidr.normalize("154.85.91.153/24"))
        assertEquals("10.0.0.0/8", Cidr.normalize("10.1.2.3/8"))
    }

    @Test
    fun rejectsInvalidValues() {
        assertNull(Cidr.normalize(""))
        assertNull(Cidr.normalize("999.1.1.1"))
        assertNull(Cidr.normalize("1.2.3.4/33"))
        assertNull(Cidr.normalize("example.com"))
    }

    @Test
    fun detectsPrivateAddresses() {
        assertTrue(Cidr.isPrivate("10.1.2.3"))
        assertTrue(Cidr.isPrivate("192.168.0.1"))
        assertTrue(Cidr.isPrivate("172.16.5.5"))
        assertTrue(Cidr.isPrivate("127.0.0.1"))
        assertFalse(Cidr.isPrivate("92.223.80.21"))
        assertFalse(Cidr.isPrivate("154.85.91.153"))
    }

    @Test
    fun buildsSubnets() {
        assertEquals("154.85.91.0/24", Cidr.subnet24("154.85.91.153"))
        assertEquals("92.223.80.0/24", Cidr.subnet24("92.223.80.65"))
        assertNull(Cidr.subnet24("::1"))
    }
}
