package ru.mlbb.blocker.vpn

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.mlbb.blocker.MlbbBlockerApp
import ru.mlbb.blocker.R
import ru.mlbb.blocker.receiver.UnknownPortActionReceiver

/**
 * Одно уведомление на незнакомый порт с выбором «матч-порт / сервисный / игнорировать».
 */
object UnknownPortNotifier {

    private const val BASE_ID = 1200

    fun notify(context: Context, port: Int, ip: String) {
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return

        val builder = NotificationCompat.Builder(app, MlbbBlockerApp.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(app.getString(R.string.unknown_port_title, port))
            .setContentText(app.getString(R.string.unknown_port_text, ip, port))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(app.getString(R.string.unknown_port_text, ip, port)),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        addAction(app, builder, port, UnknownPortActionReceiver.DECISION_MATCH, R.string.unknown_port_match)
        addAction(app, builder, port, UnknownPortActionReceiver.DECISION_SERVICE, R.string.unknown_port_service)
        addAction(app, builder, port, UnknownPortActionReceiver.DECISION_IGNORE, R.string.unknown_port_ignore)

        runCatching {
            NotificationManagerCompat.from(app).notify(notificationId(port), builder.build())
        }
    }

    fun cancel(context: Context, port: Int) {
        runCatching {
            context.getSystemService(NotificationManager::class.java)?.cancel(notificationId(port))
        }
    }

    private fun notificationId(port: Int): Int = BASE_ID + port % 800

    private fun addAction(
        app: Context,
        builder: NotificationCompat.Builder,
        port: Int,
        decision: String,
        label: Int,
    ) {
        val intent = Intent(app, UnknownPortActionReceiver::class.java)
            .setAction(UnknownPortActionReceiver.ACTION_DECIDE)
            .putExtra(UnknownPortActionReceiver.EXTRA_PORT, port)
            .putExtra(UnknownPortActionReceiver.EXTRA_DECISION, decision)
        val index = when (decision) {
            UnknownPortActionReceiver.DECISION_MATCH -> 0
            UnknownPortActionReceiver.DECISION_SERVICE -> 1
            else -> 2
        }
        val pending = PendingIntent.getBroadcast(
            app,
            port * 10 + index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(0, app.getString(label), pending)
    }
}
