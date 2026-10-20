package ru.mlbb.blocker.vpn

import io.nekohasekai.libbox.ExchangeContext
import io.nekohasekai.libbox.LocalDNSTransport
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

object LocalResolver : LocalDNSTransport {

    override fun raw(): Boolean = false

    override fun exchange(context: ExchangeContext, message: ByteArray) {
        throw UnsupportedOperationException("raw DNS is disabled")
    }

    override fun lookup(context: ExchangeContext, network: String, domain: String) {
        try {
            val addresses = InetAddress.getAllByName(domain)
            val filtered = when {
                network.endsWith("4") -> addresses.filterIsInstance<Inet4Address>()
                network.endsWith("6") -> addresses.filterIsInstance<Inet6Address>()
                else -> addresses.toList()
            }
            if (filtered.isEmpty()) {
                context.errorCode(3)
            } else {
                context.success(filtered.joinToString("\n") { it.hostAddress.orEmpty() })
            }
        } catch (_: Exception) {
            context.errorCode(3)
        }
    }
}
