package ru.mlbb.blocker.data

import android.content.Context
import ru.mlbb.blocker.vpn.UnknownPortNotifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Замечает порты MLBB, которых нет ни в матч-, ни в сервисном наборе,
 * и один раз спрашивает пользователя, что с ними делать.
 */
object UnknownPortDetector {

    private val NOISE_PORTS = setOf(53, 80, 443, 853, 8080, 8443)

    private val announcedThisProcess = ConcurrentHashMap.newKeySet<Int>()

    fun onConnection(context: Context, ip: String, port: Int) {
        if (port < 1024) return
        if (port in NOISE_PORTS) return
        if (Cidr.isPrivate(ip)) return
        if (GamePorts.isMatchPort(port) || GamePorts.isSafePort(port)) return

        val store = BlocklistStore(context)
        if (!store.unknownPortAlerts) return
        if (port in store.extraMatchPorts) return
        if (port in store.extraSafePorts) return
        if (port in store.ignoredPorts) return
        if (!announcedThisProcess.add(port)) return
        if (!store.shouldAnnounceUnknownPort(port)) return

        UnknownPortNotifier.notify(context, port, ip)
    }

    fun forget(port: Int) {
        announcedThisProcess.remove(port)
    }
}
