package ru.mlbb.blocker

import com.maxmind.db.Reader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.InetAddress

class GeoIpDatabaseTest {

    @Test
    fun bundledDatabaseResolvesObservedMoscowMatchServer() {
        val file = listOf(
            File("src/main/assets/geoip/dbip-city-ipv4.mmdb"),
            File("app/src/main/assets/geoip/dbip-city-ipv4.mmdb"),
        ).firstOrNull { it.isFile }
        assertNotNull("bundled geoip database not found", file)
        assertTrue(file!!.length() > 1_000_000)

        Reader(file).use { reader ->
            val record = reader.get(
                InetAddress.getByName("154.85.91.153"),
                Map::class.java,
            ) as? Map<*, *>
            assertNotNull(record)
            assertEquals("RU", record!!["country_code"])
            assertEquals("Moscow", record["city"])
        }
    }
}
