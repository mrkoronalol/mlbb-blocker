package ru.mlbb.blocker.vpn

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mlbb.blocker.MainActivity
import ru.mlbb.blocker.MlbbBlockerApp
import ru.mlbb.blocker.R
import ru.mlbb.blocker.data.BlockerRepository
import ru.mlbb.blocker.data.BlockerStatus
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.data.Cidr
import ru.mlbb.blocker.data.GamePorts
import ru.mlbb.blocker.data.KnownServers
import ru.mlbb.blocker.data.NetworkPing
import ru.mlbb.blocker.data.RegionProfiles
import ru.mlbb.blocker.data.StartupEndpoints
import ru.mlbb.blocker.data.geo.GeoIpRepository
import ru.mlbb.blocker.receiver.MatchNotificationReceiver

data class MatchAlertInfo(
    val ip: String,
    val port: Int,
    val network: String,
    val city: String?,
    val profile: String?,
    val blocked: Boolean,
    val pingText: String?,
    val country: String? = null,
    val demo: Boolean = false,
) {
    val key: String get() = "$network://$ip:$port"
    val address: String get() = if (ip.contains(':')) "[$ip]:$port" else "$ip:$port"
    val location: String get() = when {
        city == null && country == null -> "Сервер"
        city == null -> country ?: "Сервер"
        country == null -> city
        else -> "$city · $country"
    }
}

class MatchAlertManager(
    context: Context,
    private val scope: CoroutineScope,
    private val clashPort: Int = 0,
    private val clashSecret: String = "",
) {
    private val app = context.applicationContext
    private val store = BlocklistStore(app)
    private val handler = Handler(Looper.getMainLooper())
    private val windowManager = app.getSystemService(WindowManager::class.java)
    private val notificationManager = app.getSystemService(NotificationManager::class.java)

    private val cooldownLock = Any()
    private val lastAlerts = mutableMapOf<String, Long>()
    private var lastGlobalAlert = 0L

    private var overlay: MatchOverlayView? = null
    private var hideRunnable: Runnable? = null
    private var current: MatchAlertInfo? = null
    private var pingJob: Job? = null
    private var notificationId = 0

    fun onMatch(ip: String, port: Int, network: String, blocked: Boolean) {
        if (!store.matchAlerts) return
        if (!GamePorts.isMatchPort(port) || Cidr.isPrivate(ip)) return
        if (store.allowStartupServer && StartupEndpoints.isMainConnection(ip, port)) return
        val key = "$ip:$port"
        if (store.isServerMuted(alertKey(ip, port, network))) return

        val settings = MatchCardPolicy.Settings(
            blocked = blocked,
            hideStartup = store.hideStartupAlerts,
            stableOnly = store.stableOnlyAlerts,
        )

        scope.launch(Dispatchers.IO) {
            val wait = MatchCardPolicy.delayMs(settings)
            if (wait > 0) delay(wait)

            if (MatchCardPolicy.needsLivenessCheck(settings) && clashPort > 0) {
                val active = runCatching {
                    ClashApiClient.fetch(clashPort, clashSecret)
                }.getOrNull()
                val stillActive = active?.any {
                    it.network == network && it.ip == ip && it.port == port
                } ?: true
                val muted = store.isServerMuted(alertKey(ip, port, network))
                if (!MatchCardPolicy.shouldShow(settings, muted, stillActive)) {
                    BlockerRepository.log("Карточка $ip:$port скрыта: соединение короткое")
                    return@launch
                }
            }
            if (store.isServerMuted(alertKey(ip, port, network))) return@launch
            if (!allowAlert(key)) return@launch

            val known = KnownServers.lookup(ip)
            val geo = GeoIpRepository.lookup(ip)
            val city = geo?.city ?: known?.city
            val country = geo?.country
            val profile = store.selectedProfile?.let(RegionProfiles::byId)?.title
            val info = MatchAlertInfo(
                ip = ip,
                port = port,
                network = network,
                city = city,
                profile = profile,
                blocked = blocked,
                pingText = known?.latency,
                country = country,
            )
            withContext(Dispatchers.Main) {
                present(info)
            }
        }
    }

    fun showDemo() {
        handler.post {
            present(
                MatchAlertInfo(
                    ip = "154.85.91.153",
                    port = 10003,
                    network = "tcp",
                    city = "Москва",
                    profile = "Москва",
                    blocked = false,
                    pingText = "126 мс · ±8",
                    country = "RU",
                    demo = true,
                ),
            )
        }
    }

    fun dismiss() {
        handler.post { dismissInternal() }
    }

    fun destroy() {
        pingJob?.cancel()
        pingJob = null
        handler.post { dismissInternal() }
    }

    private fun alertKey(ip: String, port: Int, network: String): String = "$network://$ip:$port"

    private fun allowAlert(key: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        synchronized(cooldownLock) {
            lastAlerts.entries.removeAll { now - it.value > 5 * 60_000L }
            val lastForKey = lastAlerts[key] ?: 0L
            if (now - lastForKey < 30_000L || now - lastGlobalAlert < 5_000L) {
                return false
            }
            lastAlerts[key] = now
            lastGlobalAlert = now
            return true
        }
    }

    private fun present(info: MatchAlertInfo) {
        if (isLockedOrScreenOff()) return
        dismissInternal()
        current = info

        val canOverlay = Settings.canDrawOverlays(app)
        if (canOverlay) {
            showOverlay(info)
        } else if (NotificationManagerCompat.from(app).areNotificationsEnabled()) {
            showNotification(info, updateOnly = false)
        } else {
            Toast.makeText(app, R.string.match_alert_demo_denied, Toast.LENGTH_LONG).show()
            current = null
            return
        }

        startPing(info)
    }

    private fun showOverlay(info: MatchAlertInfo) {
        val manager = windowManager
        if (manager == null) {
            showNotification(info, updateOnly = false)
            return
        }
        val view = MatchOverlayView(
            app,
            onClose = { dismiss() },
            onOpen = {
                dismiss()
                openApp()
            },
            onAllow = {
                allow(info)
                dismiss()
            },
            onBlock = {
                blockPermanently(info)
                dismiss()
            },
            onMute = {
                mute(info)
                dismiss()
            },
        ).apply { bind(info) }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val width = minOf(
            app.resources.displayMetrics.widthPixels - dp(24),
            dp(340),
        )
        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(64)
        }

        try {
            manager.addView(view, params)
            overlay = view
            scheduleHide(10_000L)
        } catch (_: Throwable) {
            overlay = null
            showNotification(info, updateOnly = false)
        }
    }

    private fun showNotification(info: MatchAlertInfo, updateOnly: Boolean) {
        val manager = notificationManager ?: return
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return
        val status = app.getString(
            if (info.blocked) R.string.match_found_blocked else R.string.match_found_allowed,
        )
        val title = "${app.getString(R.string.match_found_title)}: ${info.location}"
        val text = app.getString(
            R.string.match_found_text,
            info.address,
            info.profile ?: "без профиля",
            status,
        )
        val bigText = buildString {
            appendLine(title)
            appendLine(text)
            append("Пинг: ${info.pingText ?: "измеряется…"}")
        }

        val openIntent = PendingIntent.getActivity(
            app,
            10,
            Intent(app, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(app, MlbbBlockerApp.CHANNEL_MATCH)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(updateOnly)
            .setTimeoutAfter(15_000L)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .addAction(0, app.getString(R.string.match_notification_open), openIntent)

        if (info.blocked) {
            builder.addAction(
                0,
                app.getString(R.string.blocked_notification_allow),
                allowIntent(info),
            )
        } else {
            val blockIntent = PendingIntent.getBroadcast(
                app,
                40 + (info.key.hashCode() and 0x7FFFFFFF) % 1000,
                Intent(app, MatchNotificationReceiver::class.java)
                    .setAction(MatchNotificationReceiver.ACTION_BLOCK)
                    .putExtra(MatchNotificationReceiver.EXTRA_IP, info.ip)
                    .putExtra(MatchNotificationReceiver.EXTRA_PORT, info.port),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, app.getString(R.string.match_notification_block), blockIntent)
        }

        notificationId = 3000 + (info.key.hashCode() and 0x7FFFFFFF) % 1000
        runCatching { manager.notify(notificationId, builder.build()) }
    }

    private fun allowIntent(info: MatchAlertInfo): PendingIntent = PendingIntent.getService(
        app,
        20 + (info.ip.hashCode() and 0x7FFFFFFF) % 1000,
        Intent(app, BlockerVpnService::class.java)
            .setAction(BlockerController.ACTION_ALLOW_TEMP)
            .putExtra(BlockerController.EXTRA_IP, info.ip)
            .putExtra(BlockerController.EXTRA_ALLOW_PORT, info.port)
            .putExtra(BlockerController.EXTRA_TTL, 5 * 60 * 1000L),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun startPing(info: MatchAlertInfo) {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            val summary = NetworkPing.summarize(info.ip, info.port, timeoutMs = 1_200, attempts = 4)
            val text = summary.text
            withContext(Dispatchers.Main) {
                val active = current ?: return@withContext
                if (active.key != info.key) return@withContext
                val updated = active.copy(pingText = text)
                current = updated
                overlay?.bind(updated)
                if (notificationId != 0) {
                    showNotification(updated, updateOnly = true)
                }
            }
        }
    }

    private fun scheduleHide(delayMs: Long) {
        hideRunnable?.let(handler::removeCallbacks)
        val runnable = Runnable { dismiss() }
        hideRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    private fun dismissInternal() {
        hideRunnable?.let(handler::removeCallbacks)
        hideRunnable = null
        overlay?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlay = null
        if (notificationId != 0) {
            runCatching { notificationManager?.cancel(notificationId) }
            notificationId = 0
        }
        current = null
        pingJob?.cancel()
        pingJob = null
    }

    private fun openApp() {
        val intent = Intent(app, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        runCatching { app.startActivity(intent) }
    }

    private fun allow(info: MatchAlertInfo) {
        val intent = Intent(app, BlockerVpnService::class.java)
            .setAction(BlockerController.ACTION_ALLOW_TEMP)
            .putExtra(BlockerController.EXTRA_IP, info.ip)
            .putExtra(BlockerController.EXTRA_ALLOW_PORT, info.port)
            .putExtra(BlockerController.EXTRA_TTL, 5 * 60 * 1000L)
        runCatching { app.startService(intent) }
        Toast.makeText(app, "Сервер ${info.address} разрешён на 5 минут", Toast.LENGTH_SHORT).show()
    }

    /** Постоянно заблокировать сервер из карточки (один IP, не подсеть). */
    fun blockPermanently(info: MatchAlertInfo) {
        val value = if (info.ip.contains(':')) "${info.ip}/128" else "${info.ip}/32"
        val added = store.addRules(listOf(value), "из карточки: ${info.address}") > 0
        if (added) {
            BlockerRepository.log("Сервер ${info.address} добавлен в блоклист")
            if (BlockerRepository.status.value is BlockerStatus.Running) {
                runCatching { BlockerController.reload(app) }
            }
            Toast.makeText(app, "Сервер ${info.address} заблокирован", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(app, "Сервер уже в блоклисте", Toast.LENGTH_SHORT).show()
        }
    }

    /** Не показывать карточки этого сервера сутки. */
    fun mute(info: MatchAlertInfo) {
        store.muteServer(info.key)
        BlockerRepository.log("Карточки ${info.address} скрыты на 24 часа")
        Toast.makeText(app, "Карточки ${info.address} скрыты на сутки", Toast.LENGTH_SHORT).show()
    }

    private fun isLockedOrScreenOff(): Boolean {
        val keyguard = app.getSystemService(KeyguardManager::class.java)
        val power = app.getSystemService(PowerManager::class.java)
        return keyguard?.isKeyguardLocked == true || power?.isInteractive == false
    }

    private fun dp(value: Int): Int {
        return (value * app.resources.displayMetrics.density).toInt()
    }
}
