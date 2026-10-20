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

object NetworkPing {

    fun tcp(ip: String, port: Int, timeoutMs: Int = 1500, attempts: Int = 4): PingResult {
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
            return PingResult(
                ms = average,
                jitterMs = (max - min).coerceAtLeast(0),
                kind = kind,
            )
        }

        val start = System.nanoTime()
        return try {
            if (InetAddress.getByName(ip).isReachable(1200)) {
                PingResult(
                    ms = (System.nanoTime() - start) / 1_000_000,
                    jitterMs = null,
                    kind = "ICMP",
                )
            } else {
                PingResult(null, null, "нет ответа")
            }
        } catch (_: Exception) {
            PingResult(null, null, "нет ответа")
        }
    }
}
