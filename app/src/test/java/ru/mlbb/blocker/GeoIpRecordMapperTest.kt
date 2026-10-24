package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mlbb.blocker.data.geo.GeoIpRecordMapper

class GeoIpRecordMapperTest {

    @Test
    fun mapsFlatDbIpRecord() {
        val record = mapOf(
            "country_code" to "RU",
            "city" to "Moscow",
        )
        assertEquals("RU", GeoIpRecordMapper.countryCode(record))
        assertEquals("Москва", GeoIpRecordMapper.city(record))
    }

    @Test
    fun mapsNestedMaxMindRecord() {
        val record = mapOf(
            "country" to mapOf("iso_code" to "RU"),
            "city" to mapOf("names" to mapOf("en" to "Novosibirsk")),
        )
        assertEquals("RU", GeoIpRecordMapper.countryCode(record))
        assertEquals("Новосибирск", GeoIpRecordMapper.city(record))
    }

    @Test
    fun stripsCityDistrictAndTranslates() {
        assertEquals(
            "Москва",
            GeoIpRecordMapper.city(mapOf("city" to "Moscow (Tsentralnyy administrativnyy okrug)")),
        )
        assertEquals(
            "Франкфурт",
            GeoIpRecordMapper.city(mapOf("city" to "Frankfurt am Main (Innenstadt III)")),
        )
    }

    @Test
    fun returnsNullForMissingFields() {
        assertEquals(null, GeoIpRecordMapper.countryCode(emptyMap<String, Any>()))
        assertEquals(null, GeoIpRecordMapper.city(emptyMap<String, Any>()))
    }
}
