package ru.mlbb.blocker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.data.db.MonitorRepository
import ru.mlbb.blocker.data.geo.GeoIpRepository
import ru.mlbb.blocker.data.Presets

class MlbbBlockerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        MonitorRepository.initialize(this)
        GeoIpRepository.initialize(this)
        seedFirstRun()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SERVICE,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    setShowBadge(false)
                },
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_BLOCKED,
                    "Заблокированные подключения",
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
    }

    private fun seedFirstRun() {
        val prefs = getSharedPreferences("mlbb_blocker", MODE_PRIVATE)
        if (prefs.getBoolean("first_run_seeded", false)) return
        val store = BlocklistStore(this)
        if (store.loadRules().isEmpty()) {
            store.addRulesDetailed(Presets.MOSCOW.rules)
            store.discoveryMode = false
        }
        prefs.edit().putBoolean("first_run_seeded", true).apply()
    }

    companion object {
        const val CHANNEL_SERVICE = "mlbb_blocker_service"
        const val CHANNEL_BLOCKED = "mlbb_blocker_blocked"

        lateinit var instance: MlbbBlockerApp
            private set
    }
}
