package ru.mlbb.blocker.vpn

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.system.OsConstants
import io.nekohasekai.libbox.BridgeOptions
import io.nekohasekai.libbox.BridgeSession
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.ShellSession
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import io.nekohasekai.libbox.NetworkInterface as LibboxNetworkInterface
import ru.mlbb.blocker.data.BlockerRepository
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface

class PlatformBridge(private val service: BlockerVpnService) : PlatformInterface {

    private val connectivity: ConnectivityManager? =
        service.getSystemService(ConnectivityManager::class.java)

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    override fun autoDetectInterfaceControl(fd: Int) {
        if (!service.protect(fd)) {
            BlockerRepository.log("Не удалось защитить сокет ядра (fd=$fd)")
        }
    }

    override fun openTun(options: TunOptions): Int = service.createTun(options)

    override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int,
    ): ConnectionOwner {
        val owner = ConnectionOwner()
        owner.userId = Process.INVALID_UID
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return owner
        val manager = connectivity ?: return owner
        return try {
            val uid = manager.getConnectionOwnerUid(
                ipProtocol,
                InetSocketAddress(sourceAddress, sourcePort),
                InetSocketAddress(destinationAddress, destinationPort),
            )
            owner.userId = uid
            if (uid != Process.INVALID_UID) {
                val packages = service.packageManager.getPackagesForUid(uid).orEmpty().toList()
                owner.userName = packages.firstOrNull().orEmpty()
                owner.setAndroidPackageNames(StringArray(packages))
            }
            owner
        } catch (_: Exception) {
            owner
        }
    }

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        DefaultNetworkMonitor.start(service, listener)
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        DefaultNetworkMonitor.stop()
    }

    override fun getInterfaces(): NetworkInterfaceIterator {
        val interfaces = mutableListOf<LibboxNetworkInterface>()
        val manager = connectivity ?: return InterfaceArray(emptyList())
        val systemInterfaces = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        try {
            for (network in manager.allNetworks) {
                val capabilities = manager.getNetworkCapabilities(network) ?: continue
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
                if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) continue

                val linkProperties = manager.getLinkProperties(network) ?: continue
                val name = linkProperties.interfaceName?.takeIf { it.isNotBlank() } ?: continue
                if (name == "lo" || name.startsWith("tun") || name.startsWith("ppp")) continue

                val networkInterface = systemInterfaces.firstOrNull { it.name == name }
                    ?: runCatching { NetworkInterface.getByName(name) }.getOrNull()
                    ?: continue

                val boxInterface = LibboxNetworkInterface().apply {
                    this.name = name
                    index = networkInterface.index
                    mtu = runCatching { networkInterface.mtu }.getOrDefault(1500)
                    dnsServer = StringArray(linkProperties.dnsServers.mapNotNull { it.hostAddress })
                    gateway = StringArray(
                        linkProperties.routes
                            .filter { it.destination.prefixLength == 0 }
                            .mapNotNull { it.gateway }
                            .filterNot { it.isAnyLocalAddress }
                            .mapNotNull { it.hostAddress },
                    )
                    type = when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                        else -> Libbox.InterfaceTypeOther
                    }
                    addresses = StringArray(
                        networkInterface.interfaceAddresses.mapNotNull { it.toPrefix() },
                    )
                    var interfaceFlags = OsConstants.IFF_UP or OsConstants.IFF_RUNNING
                    if (networkInterface.isLoopback) interfaceFlags = interfaceFlags or OsConstants.IFF_LOOPBACK
                    if (networkInterface.isPointToPoint) interfaceFlags = interfaceFlags or OsConstants.IFF_POINTOPOINT
                    if (runCatching { networkInterface.supportsMulticast() }.getOrDefault(false)) {
                        interfaceFlags = interfaceFlags or OsConstants.IFF_MULTICAST
                    }
                    flags = interfaceFlags
                    metered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                }
                interfaces.add(boxInterface)
            }
        } catch (_: Exception) {
            // return what we have
        }
        return InterfaceArray(interfaces)
    }

    override fun underNetworkExtension(): Boolean = false

    override fun includeAllNetworks(): Boolean = false

    override fun clearDNSCache() = Unit

    override fun readWIFIState(): WIFIState? = null

    override fun localDNSTransport(): LocalDNSTransport = LocalResolver

    override fun sendNotification(notification: Notification) = Unit

    override fun cancelNotification(tag: String, id: Int) = Unit

    override fun checkPlatformShell() = Unit

    override fun startNeighborMonitor(listener: NeighborUpdateListener) = Unit

    override fun closeNeighborMonitor(listener: NeighborUpdateListener) = Unit

    override fun createBridge(options: BridgeOptions?): BridgeSession? = null

    override fun lookupSFTPServer(): String = ""

    override fun lookupUser(name: String): PlatformUser? = null

    override fun openShellSession(
        user: PlatformUser?,
        cmd: String,
        args: StringIterator?,
        dir: String,
        width: Int,
        height: Int,
    ): ShellSession? = null

    override fun readSystemSSHHostKey(): String = ""

    override fun registerMyInterface(name: String) = Unit

    override fun tailscaleHostname(): String = ""

    override fun usePlatformBridge(): Boolean = false

    override fun usePlatformShell(): Boolean = false

    private fun InterfaceAddress.toPrefix(): String? {
        val inetAddress = address ?: return null
        val host = if (inetAddress is Inet6Address) {
            Inet6Address.getByAddress(inetAddress.address).hostAddress
        } else {
            inetAddress.hostAddress
        }
        return "$host/$networkPrefixLength"
    }

    private class InterfaceArray(
        private val items: List<LibboxNetworkInterface>,
    ) : NetworkInterfaceIterator {
        private var index = 0
        override fun hasNext(): Boolean = index < items.size
        override fun next(): LibboxNetworkInterface = items[index++]
    }

    class StringArray(private val items: List<String>) : StringIterator {
        private var index = 0
        override fun len(): Int = items.size
        override fun hasNext(): Boolean = index < items.size
        override fun next(): String = items[index++]
    }
}
