package ru.mlbb.blocker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.mlbb.blocker.data.BlockerRepository
import ru.mlbb.blocker.data.BlockerStatus
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.vpn.BlockerController

/**
 * Кнопка «Заблокировать» в уведомлении о найденном матче.
 */
class MatchNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_BLOCK) return
        val ip = intent.getStringExtra(EXTRA_IP)?.takeIf { it.isNotBlank() } ?: return
        val port = intent.getIntExtra(EXTRA_PORT, 0)
        val value = if (ip.contains(':')) "$ip/128" else "$ip/32"
        val added = BlocklistStore(context).addRules(listOf(value), "из уведомления") > 0
        if (!added) return
        BlockerRepository.log("Сервер $ip:$port добавлен в блоклист")
        if (BlockerRepository.status.value is BlockerStatus.Running) {
            runCatching { BlockerController.reload(context) }
        }
    }

    companion object {
        const val ACTION_BLOCK = "ru.mlbb.blocker.action.BLOCK_FROM_NOTIFICATION"
        const val EXTRA_IP = "ip"
        const val EXTRA_PORT = "port"
    }
}
