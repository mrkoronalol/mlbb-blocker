package ru.mlbb.blocker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.data.db.MonitorRepository
import ru.mlbb.blocker.data.geo.GeoIpRepository
import ru.mlbb.blocker.data.RegionProfiles

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
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MATCH,
                    getString(R.string.match_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    enableVibration(true)
                },
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_UPDATE,
                    getString(R.string.update_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    private fun seedFirstRun() {
        val prefs = getSharedPreferences("mlbb_blocker", MODE_PRIVATE)
        if (prefs.getBoolean("first_run_seeded", false)) return
        val store = BlocklistStore(this)
        if (store.loadRules().isEmpty() && store.selectedProfile == null) {
            store.selectProfile(RegionProfiles.ID_FAR_EAST)
            store.discoveryMode = false
        }
        prefs.edit().putBoolean("first_run_seeded", true).apply()
    }

    companion object {
        const val CHANNEL_SERVICE = "mlbb_blocker_service"
        const val CHANNEL_BLOCKED = "mlbb_blocker_blocked"
        const val CHANNEL_MATCH = "mlbb_blocker_match"
        const val CHANNEL_UPDATE = "mlbb_blocker_update"

        lateinit var instance: MlbbBlockerApp
            private set
    }
}
