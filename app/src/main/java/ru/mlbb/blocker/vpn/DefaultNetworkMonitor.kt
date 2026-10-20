package ru.mlbb.blocker.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import io.nekohasekai.libbox.InterfaceUpdateListener
import java.net.NetworkInterface

object DefaultNetworkMonitor {

    private var manager: ConnectivityManager? = null
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var listener: InterfaceUpdateListener? = null

    var defaultNetwork: Network? = null
        private set

    fun start(context: Context, updateListener: InterfaceUpdateListener) {
        stop()
        val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
        manager = connectivity
        listener = updateListener
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                defaultNetwork = network
                notifyListener()
            }

            override fun onLost(network: Network) {
                if (defaultNetwork == network) {
                    defaultNetwork = null
                    notifyListener()
                }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (defaultNetwork == network) {
                    notifyListener()
                }
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                if (defaultNetwork == network) {
                    notifyListener()
                }
            }
        }
        callback = networkCallback
        try {
            connectivity.registerDefaultNetworkCallback(networkCallback)
            defaultNetwork = connectivity.activeNetwork
        } catch (_: Exception) {
            defaultNetwork = null
        }
        notifyListener()
    }

    fun stop() {
        val connectivity = manager
        val networkCallback = callback
        if (connectivity != null && networkCallback != null) {
            try {
                connectivity.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {
                // ignore
            }
        }
        manager = null
        callback = null
        listener = null
        defaultNetwork = null
    }

    fun interfaceName(): String? {
        val network = defaultNetwork ?: return null
        return manager?.getLinkProperties(network)?.interfaceName
    }

    private fun notifyListener() {
        val updateListener = listener ?: return
        val network = defaultNetwork
        if (network == null) {
            updateListener.updateDefaultInterface("", -1, false, false)
            return
        }
        val linkProperties = manager?.getLinkProperties(network)
        val name = linkProperties?.interfaceName
        if (name.isNullOrBlank()) {
            updateListener.updateDefaultInterface("", -1, false, false)
            return
        }
        val index = try {
            NetworkInterface.getByName(name)?.index ?: -1
        } catch (_: Exception) {
            -1
        }
        updateListener.updateDefaultInterface(name, index, false, false)
    }
}
