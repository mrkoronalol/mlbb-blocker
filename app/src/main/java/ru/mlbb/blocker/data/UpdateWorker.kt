package ru.mlbb.blocker.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.mlbb.blocker.BuildConfig

class UpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val info = UpdateChecker.checkLatest(BuildConfig.VERSION_NAME)
        if (info != null) {
            UpdateNotifier.notifyIfNeeded(applicationContext, info)
        }
        return Result.success()
    }
}
