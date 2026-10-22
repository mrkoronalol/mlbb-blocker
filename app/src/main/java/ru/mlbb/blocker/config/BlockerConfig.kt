package ru.mlbb.blocker.config

import android.content.Context
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.data.RegionProfiles
import ru.mlbb.blocker.data.StartupEndpoints
import java.net.InetAddress
import java.net.ServerSocket
import java.security.SecureRandom

object BlockerConfig {

    fun build(context: Context, port: Int, secret: String): String {
        val store = BlocklistStore(context)
        val profile = store.selectedProfile?.let(RegionProfiles::byId)
        val profileRules = profile?.rules?.map { it.first }.orEmpty()
        val rules = store.activeValues().distinct()
        val allowRules = (
            store.activeAllowValues() +
                profile?.allowedCidrs.orEmpty() +
                if (store.allowStartupServer) StartupEndpoints.CIDRS else emptyList()
            ).distinct()
        return SingBoxConfigBuilder.build(
            rules = rules,
            profileRules = profileRules,
            allowRules = allowRules,
            blockMatchPorts = profile?.blockOtherMatchServers == true,
            discoveryMode = store.discoveryMode || System.currentTimeMillis() < store.discoveryUntil,
            clashPort = port,
            clashSecret = secret,
            rejectMethod = if (store.fastReject) "default" else "drop",
            logOutput = java.io.File(context.filesDir, "singbox.log").absolutePath,
            logLevel = if (store.logBlockedAttempts) "debug" else "info",
        )
    }

    fun newPort(): Int {
        return try {
            ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { it.localPort }
        } catch (_: Exception) {
            39090
        }
    }

    fun newSecret(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
