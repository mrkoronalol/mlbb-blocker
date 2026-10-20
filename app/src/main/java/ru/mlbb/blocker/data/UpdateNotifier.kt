package ru.mlbb.blocker.data

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.mlbb.blocker.MlbbBlockerApp
import ru.mlbb.blocker.R

object UpdateNotifier {

    private const val PREFS = "mlbb_updates"
    private const val KEY_NOTIFIED_VERSION = "notified_version"
    private const val NOTIFICATION_ID = 5001

    @SuppressLint("MissingPermission")
    fun notifyIfNeeded(context: Context, info: UpdateInfo) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_NOTIFIED_VERSION, null) == info.version) return
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return

        val downloadIntent = PendingIntent.getActivity(
            app,
            NOTIFICATION_ID,
            Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = app.getString(R.string.update_notification_title, info.version)
        val notification = NotificationCompat.Builder(app, MlbbBlockerApp.CHANNEL_UPDATE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(app.getString(R.string.update_notification_text))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    app.getString(R.string.update_notification_text) +
                        if (info.notes.isNotBlank()) "\n\n${info.notes.take(400)}" else "",
                ),
            )
            .setContentIntent(downloadIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .addAction(0, app.getString(R.string.update_download), downloadIntent)
            .build()

        runCatching { NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, notification) }
        prefs.edit().putString(KEY_NOTIFIED_VERSION, info.version).apply()
    }
}
