package ru.mlbb.blocker.data

import java.net.Inet6Address
import java.net.InetAddress

object Cidr {

    fun normalize(input: String): String? {
        val raw = input.trim()
        if (raw.isEmpty()) return null
        val slash = raw.indexOf('/')
        if (slash != raw.lastIndexOf('/')) return null

        val addressPart = if (slash >= 0) raw.substring(0, slash) else raw
        val prefixPart = if (slash >= 0) raw.substring(slash + 1) else null
        if (addressPart.isEmpty()) return null

        if (isIpv4(addressPart)) {
            val prefix = prefixPart?.toIntOrNull() ?: if (slash >= 0) return null else 32
            if (prefix !in 0..32) return null
            val value = ipv4ToLong(addressPart) ?: return null
            val mask = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
            return "${longToIpv4(value and mask)}/$prefix"
        }

        if (addressPart.contains(':')) {
            val prefix = prefixPart?.toIntOrNull() ?: if (slash >= 0) return null else 128
            if (prefix !in 0..128) return null
            if (!addressPart.matches(Regex("[0-9a-fA-F:.]+"))) return null
            val address = try {
                InetAddress.getByName(addressPart)
            } catch (_: Exception) {
                return null
            }
            if (address !is Inet6Address) return null
            val host = address.hostAddress?.substringBefore('%') ?: return null
            return "$host/$prefix"
        }

        return null
    }

    fun withPrefix(ip: String, prefix: Int): String? = normalize("$ip/$prefix")

    fun addressOf(cidr: String): String? = cidr.substringBefore('/').takeIf { it.isNotBlank() }

    fun isPrivate(ip: String): Boolean {
        if (isIpv4(ip)) {
            val value = ipv4ToLong(ip) ?: return false
            return value in ipv4ToLong("10.0.0.0")!!..ipv4ToLong("10.255.255.255")!! ||
                value in ipv4ToLong("172.16.0.0")!!..ipv4ToLong("172.31.255.255")!! ||
                value in ipv4ToLong("192.168.0.0")!!..ipv4ToLong("192.168.255.255")!! ||
                value in ipv4ToLong("127.0.0.0")!!..ipv4ToLong("127.255.255.255")!! ||
                value in ipv4ToLong("169.254.0.0")!!..ipv4ToLong("169.254.255.255")!! ||
                value == 0L
        }
        if (ip.contains(':')) {
            val lower = ip.lowercase()
            return lower == "::1" || lower == "::" ||
                lower.startsWith("fc") || lower.startsWith("fd") ||
                lower.startsWith("fe8") || lower.startsWith("fe9") ||
                lower.startsWith("fea") || lower.startsWith("feb")
        }
        return false
    }

    fun isIpv4(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() &&
                part.length <= 3 &&
                part.all { it.isDigit() } &&
                part.toIntOrNull() in 0..255
        }
    }

    fun ipv4ToLong(value: String): Long? {
        val parts = value.split('.')
        if (parts.size != 4) return null
        var result = 0L
        for (part in parts) {
            val number = part.toIntOrNull() ?: return null
            if (number !in 0..255) return null
            result = (result shl 8) or number.toLong()
        }
        return result
    }

    fun longToIpv4(value: Long): String {
        return "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"
    }

    fun subnet24(ip: String): String? {
        if (!isIpv4(ip)) return null
        return withPrefix(ip, 24)
    }

    fun subnet64(ip: String): String? {
        if (isIpv4(ip) || !ip.contains(':')) return null
        return withPrefix(ip, 64)
    }
}
