package ru.mlbb.blocker.data.remote

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ru.mlbb.blocker.BuildConfig
import ru.mlbb.blocker.data.analytics.Analytics
import java.util.concurrent.TimeUnit

/**
 * Синхронизация удалённой конфигурации: раз в 6 часов и при запуске приложения.
 * При ошибке сети остаётся последний сохранённый конфиг.
 */
class ConfigSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        RemoteConfigRepository.initialize(applicationContext)
        val synced = RemoteConfigRepository.refresh(applicationContext)
        if (!synced) return Result.retry()

        val config = RemoteConfigRepository.state.value
        Analytics.initialize(applicationContext)
        Analytics.track(
            "config_sync",
            mapOf(
                "version" to BuildConfig.VERSION_NAME,
                "maintenance" to config.maintenance,
                "profiles" to config.profiles.size,
            ),
        )
        Analytics.flush(applicationContext)
        return Result.success()
    }

    companion object {
        private const val PERIODIC_NAME = "mlbb-config-sync"
        private const val NOW_NAME = "mlbb-config-sync-now"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ConfigSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Немедленная синхронизация при запуске приложения. */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<ConfigSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
