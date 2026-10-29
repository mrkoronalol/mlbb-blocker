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
 * После обновления APK система убивает процесс, и VPN-сервис не поднимается сам.
 * Если защита была включена до обновления — возвращаем её в рабочее состояние.
 */
class PackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val store = BlocklistStore(context)
        if (!store.lastRunning) return

        val permissionReady = runCatching { VpnService.prepare(context) == null }.getOrDefault(false)
        if (!permissionReady) {
            VpnNotifier.notifyInterrupted(
                context,
                context.getString(ru.mlbb.blocker.R.string.notification_update_permission_lost),
            )
            return
        }
        if (!BlockerController.isMlbbInstalled(context)) return

        val started = runCatching {
            BlockerController.start(context, BlockerConfig.newPort(), BlockerConfig.newSecret())
        }.isSuccess
        if (!started) {
            VpnNotifier.notifyInterrupted(
                context,
                context.getString(ru.mlbb.blocker.R.string.notification_update_stopped),
            )
        }
    }
}
