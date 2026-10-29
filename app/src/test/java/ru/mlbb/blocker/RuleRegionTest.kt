package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.mlbb.blocker.data.RuleRegion
import ru.mlbb.blocker.data.RuleRegion.Kind

class RuleRegionTest {

    @Test
    fun detectsMoscowByIp() {
        assertEquals("МСК", RuleRegion.of("154.85.91.0/24")?.label)
        assertEquals(Kind.MOSCOW, RuleRegion.of("66.253.15.83")?.kind)
        assertEquals("МСК", RuleRegion.of("81.177.97.0/24")?.label)
    }

    @Test
    fun detectsKnownRegions() {
        assertEquals("НСК", RuleRegion.of("146.185.199.0/24")?.label)
        assertEquals("ДВ", RuleRegion.of("92.223.80.0/24")?.label)
        assertEquals("ЕКБ", RuleRegion.of("92.223.91.12")?.label)
        assertEquals("РНД", RuleRegion.of("146.185.197.0/24")?.label)
        assertEquals("ФРА", RuleRegion.of("148.153.82.0/24")?.label)
    }

    @Test
    fun fallsBackToNote() {
        assertEquals("МСК", RuleRegion.of("203.0.113.5/32", "Москва, матч-сервер")?.label)
        assertEquals("НСК", RuleRegion.of("203.0.113.6/32", "Новосибирск")?.label)
        assertEquals("SRV", RuleRegion.of("203.0.113.7/32", "служебный порт")?.label)
        assertEquals("IP", RuleRegion.of("203.0.113.8/32", "мой сервер")?.label)
    }

    @Test
    fun returnsNullForUnknownRuleWithoutNote() {
        assertNull(RuleRegion.of("203.0.113.9/32"))
    }
}
