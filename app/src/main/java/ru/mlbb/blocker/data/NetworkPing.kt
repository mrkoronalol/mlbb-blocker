package ru.mlbb.blocker.data

import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

data class PingResult(
    val ms: Long?,
    val jitterMs: Long?,
    val kind: String,
)

/**
 * Итог серии TCP-проб: среднее, минимум/максимум, джиттер и доля потерянных проб.
 */
data class PingSummary(
    val avgMs: Long?,
    val minMs: Long?,
    val maxMs: Long?,
    val jitterMs: Long?,
    val lossPercent: Int,
    val kind: String,
) {
    val text: String
        get() {
            val average = avgMs ?: return kind
            return buildString {
                append("$average мс")
                jitterMs?.takeIf { it > 0 }?.let { append(" · ±$it") }
                if (lossPercent > 0) append(" · потери $lossPercent%")
            }
        }
}

object NetworkPing {

    fun tcp(ip: String, port: Int, timeoutMs: Int = 1500, attempts: Int = 4): PingResult {
        val summary = summarize(ip, port, timeoutMs, attempts)
        return PingResult(summary.avgMs, summary.jitterMs, summary.kind)
    }

    fun summarize(ip: String, port: Int, timeoutMs: Int = 1500, attempts: Int = 4): PingSummary {
        val samples = mutableListOf<Long>()
        var kind = "таймаут"
        repeat(attempts) {
            val start = System.nanoTime()
            try {
                Socket().use { socket ->
                    socket.tcpNoDelay = true
                    socket.connect(InetSocketAddress(ip, port), timeoutMs)
                }
                samples.add((System.nanoTime() - start) / 1_000_000)
                kind = "TCP"
            } catch (_: ConnectException) {
                val elapsed = (System.nanoTime() - start) / 1_000_000
                if (elapsed < timeoutMs - 100) {
                    samples.add(elapsed)
                    kind = "RST"
                }
            } catch (_: SocketTimeoutException) {
                // try next attempt
            } catch (_: Exception) {
                // try next attempt
            }
        }
        if (samples.isNotEmpty()) {
            val min = samples.min()
            val max = samples.max()
            val average = samples.average().toLong()
            return PingSummary(
                avgMs = average,
                minMs = min,
                maxMs = max,
                jitterMs = (max - min).coerceAtLeast(0),
                lossPercent = ((attempts - samples.size) * 100 / attempts).coerceIn(0, 100),
                kind = kind,
            )
        }

        val start = System.nanoTime()
        return try {
            if (InetAddress.getByName(ip).isReachable(1200)) {
                PingSummary(
                    avgMs = (System.nanoTime() - start) / 1_000_000,
                    minMs = null,
                    maxMs = null,
                    jitterMs = null,
                    lossPercent = 100,
                    kind = "ICMP",
                )
            } else {
                PingSummary(null, null, null, null, 100, "нет ответа")
            }
        } catch (_: Exception) {
            PingSummary(null, null, null, null, 100, "нет ответа")
        }
    }
}
