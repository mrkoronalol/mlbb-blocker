package ru.mlbb.blocker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.mlbb.blocker.data.BlockerRepository
import ru.mlbb.blocker.data.BlockerStatus
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.data.PortOverrides
import ru.mlbb.blocker.data.UnknownPortDetector
import ru.mlbb.blocker.vpn.BlockerController
import ru.mlbb.blocker.vpn.UnknownPortNotifier

/**
 * Решение пользователя по незнакомому порту из уведомления.
 * Применяется сразу: правила перезагружаются, если VPN запущен.
 */
class UnknownPortActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DECIDE) return
        val port = intent.getIntExtra(EXTRA_PORT, 0)
        if (port <= 0) return
        val decision = intent.getStringExtra(EXTRA_DECISION) ?: DECISION_IGNORE
        val store = BlocklistStore(context)

        when (decision) {
            DECISION_MATCH -> {
                store.addExtraMatchPort(port)
                BlockerRepository.log("Порт $port добавлен как матч-порт")
            }

            DECISION_SERVICE -> {
                store.addExtraSafePort(port)
                BlockerRepository.log("Порт $port помечен как сервисный")
            }

            else -> {
                store.ignorePort(port)
                BlockerRepository.log("Порт $port игнорируется")
            }
        }

        PortOverrides.load(store)
        UnknownPortDetector.forget(port)
        UnknownPortNotifier.cancel(context, port)

        if (decision != DECISION_IGNORE &&
            BlockerRepository.status.value is BlockerStatus.Running
        ) {
            runCatching { BlockerController.reload(context) }
        }
    }

    companion object {
        const val ACTION_DECIDE = "ru.mlbb.blocker.action.UNKNOWN_PORT_DECISION"
        const val EXTRA_PORT = "port"
        const val EXTRA_DECISION = "decision"
        const val DECISION_MATCH = "match"
        const val DECISION_SERVICE = "service"
        const val DECISION_IGNORE = "ignore"
    }
}
