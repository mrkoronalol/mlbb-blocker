package ru.mlbb.blocker.vpn

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.mlbb.blocker.MainActivity
import ru.mlbb.blocker.MlbbBlockerApp
import ru.mlbb.blocker.R
import ru.mlbb.blocker.receiver.ProtectionRestartReceiver

/**
 * Уведомления о том, что защита перестала работать не по желанию пользователя:
 * после обновления APK, падения сервиса или отзыва разрешения VPN.
 */
object VpnNotifier {

    private const val NOTIFICATION_ID = 1102

    fun notifyInterrupted(context: Context, reason: String) {
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return

        val openIntent = PendingIntent.getActivity(
            app,
            101,
            Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val restartIntent = PendingIntent.getBroadcast(
            app,
            102,
            Intent(app, ProtectionRestartReceiver::class.java)
                .setAction(ProtectionRestartReceiver.ACTION_RESTART),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(app, MlbbBlockerApp.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(app.getString(R.string.notification_interrupted_title))
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .addAction(0, app.getString(R.string.notification_interrupted_restart), restartIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, notification)
        }
    }

    fun cancel(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }
}
