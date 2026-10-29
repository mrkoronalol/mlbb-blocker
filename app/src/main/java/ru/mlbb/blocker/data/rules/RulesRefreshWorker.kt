package ru.mlbb.blocker.data.rules

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.mlbb.blocker.data.BlockerRepository
import ru.mlbb.blocker.data.BlockerStatus
import ru.mlbb.blocker.vpn.BlockerController

/**
 * Раз в сутки проверяет подписанные правила на GitHub-релизе.
 * Любая ошибка оставляет предыдущий рабочий набор.
 */
class RulesRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        RuleConfigRepository.initialize(applicationContext)
        val result = RuleConfigRepository.refresh(applicationContext)
        if (result is RulesRefreshResult.Updated &&
            BlockerRepository.status.value is BlockerStatus.Running
        ) {
            runCatching { BlockerController.reload(applicationContext) }
        }
        return when (result) {
            is RulesRefreshResult.NetworkError -> Result.retry()
            else -> Result.success()
        }
    }
}
