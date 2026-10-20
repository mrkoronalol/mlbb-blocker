package ru.mlbb.blocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import ru.mlbb.blocker.config.BlockerConfig
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.vpn.BlockerController

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = BlocklistStore(context)
        if (!store.autoStart || !store.lastRunning) return
        if (!BlockerController.isMlbbInstalled(context)) return
        if (VpnService.prepare(context) != null) return
        runCatching {
            BlockerController.start(
                context,
                BlockerConfig.newPort(),
                BlockerConfig.newSecret(),
            )
        }
    }
}
