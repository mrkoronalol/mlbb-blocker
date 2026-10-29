package ru.mlbb.blocker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import ru.mlbb.blocker.config.BlockerConfig
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.vpn.BlockerController
import ru.mlbb.blocker.vpn.VpnNotifier

/**
 * Кнопка «Включить» в уведомлении о том, что защита была остановлена системой.
 */
class ProtectionRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESTART) return
        if (!BlocklistStore(context).lastRunning) return
        val permissionReady = runCatching { VpnService.prepare(context) == null }.getOrDefault(false)
        if (!permissionReady) return
        if (!BlockerController.isMlbbInstalled(context)) return
        val started = runCatching {
            BlockerController.start(context, BlockerConfig.newPort(), BlockerConfig.newSecret())
        }.isSuccess
        if (started) VpnNotifier.cancel(context)
    }

    companion object {
        const val ACTION_RESTART = "ru.mlbb.blocker.action.RESTART_FROM_NOTIFICATION"
    }
}
