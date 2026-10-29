package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mlbb.blocker.data.PingSummary

class PingSummaryTest {

    @Test
    fun formatsAverageOnly() {
        val summary = PingSummary(avgMs = 126, minMs = 120, maxMs = 130, jitterMs = 0, lossPercent = 0, kind = "TCP")
        assertEquals("126 мс", summary.text)
    }

    @Test
    fun addsJitterAndLoss() {
        val summary = PingSummary(avgMs = 84, minMs = 80, maxMs = 92, jitterMs = 12, lossPercent = 25, kind = "TCP")
        assertEquals("84 мс · ±12 · потери 25%", summary.text)
    }

    @Test
    fun fallsBackToKindWhenNoSamples() {
        val summary = PingSummary(avgMs = null, minMs = null, maxMs = null, jitterMs = null, lossPercent = 100, kind = "нет ответа")
        assertEquals("нет ответа", summary.text)
    }
}
