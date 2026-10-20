package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.vpn.ClashApiClient

class ClashApiClientTest {

    @Test
    fun parsesDestinationsAndFiltersNoise() {
        val payload = """
            {
              "connections": [
                {
                  "id": "a",
                  "metadata": {
                    "network": "tcp",
                    "destinationIP": "154.85.91.153",
                    "destinationPort": "10003",
                    "host": ""
                  }
                },
                {
                  "id": "b",
                  "metadata": {
                    "network": "udp",
                    "destinationIP": "92.223.80.65",
                    "destinationPort": 30190,
                    "sniffHost": ""
                  }
                },
                {
                  "id": "c",
                  "metadata": {
                    "network": "udp",
                    "destinationIP": "10.215.173.2",
                    "destinationPort": 53
                  }
                },
                {
                  "id": "d",
                  "metadata": {
                    "network": "tcp",
                    "destinationIP": "192.168.0.1",
                    "destinationPort": 443
                  }
                }
              ]
            }
        """.trimIndent()

        val result = ClashApiClient.parse(payload)
        assertEquals(2, result.size)
        assertTrue(result.any { it.ip == "154.85.91.153" && it.port == 10003 })
        assertTrue(result.any { it.ip == "92.223.80.65" && it.port == 30190 })
    }
}
