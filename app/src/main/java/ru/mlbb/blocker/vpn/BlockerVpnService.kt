package ru.mlbb.blocker.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.IpPrefix
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.RoutePrefix
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.TunOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mlbb.blocker.BuildConfig
import ru.mlbb.blocker.MainActivity
import ru.mlbb.blocker.MlbbBlockerApp
import ru.mlbb.blocker.R
import ru.mlbb.blocker.config.BlockerConfig
import ru.mlbb.blocker.config.SingBoxConfigBuilder
import ru.mlbb.blocker.data.BlockerRepository
import ru.mlbb.blocker.data.BlockerStatus
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.data.GamePorts
import ru.mlbb.blocker.data.PortOverrides
import ru.mlbb.blocker.data.UnknownPortDetector
import ru.mlbb.blocker.data.analytics.Analytics
import ru.mlbb.blocker.data.geo.GeoIpRepository
import ru.mlbb.blocker.data.db.MonitorRepository
import java.io.File
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class BlockerVpnService : VpnService() {

    companion object {
        private const val TAG = "BlockerVpnService"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_START_ATTEMPTS = 1
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stopping = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val startAttempts = java.util.concurrent.atomic.AtomicInteger(0)

    private var commandServer: CommandServer? = null
    private var platformBridge: PlatformBridge? = null
    private var tunInterface: ParcelFileDescriptor? = null
    private var startJob: Job? = null
    private var connectionMonitor: ConnectionMonitor? = null
    private var matchAlertManager: MatchAlertManager? = null
    private var discoveryJob: Job? = null
    private var allowExpiryJob: Job? = null
    private var coreLogMonitor: CoreLogMonitor? = null

    private var sessionPort: Int = 0
    private var sessionSecret: String = ""
    private var lastRuleCount: Int = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            BlockerController.ACTION_START -> {
                if (started.compareAndSet(false, true)) {
                    stopping.set(false)
                    startAttempts.set(0)
                    sessionPort = intent.getIntExtra(BlockerController.EXTRA_PORT, 0)
                        .takeIf { it > 0 } ?: BlockerConfig.newPort()
                    sessionSecret = intent.getStringExtra(BlockerController.EXTRA_SECRET)
                        .takeIf { !it.isNullOrBlank() } ?: BlockerConfig.newSecret()
                    startBlocking()
                }
            }

            BlockerController.ACTION_STOP -> {
                if (!started.get() && commandServer == null) {
                    stopSelf()
                } else {
                    stopBlocking()
                }
            }

            BlockerController.ACTION_RELOAD -> {
                if (commandServer == null) {
                    stopSelf()
                } else {
                    reloadBlocking()
                }
            }

            BlockerController.ACTION_ALLOW_TEMP -> {
                val ip = intent.getStringExtra(BlockerController.EXTRA_IP)
                val ttl = intent.getLongExtra(BlockerController.EXTRA_TTL, 5 * 60 * 1000L)
                if (!ip.isNullOrBlank()) {
                    val value = if (ip.contains(':')) "$ip/128" else "$ip/32"
                    val added = BlocklistStore(this).addAllowRule(value, "временное разрешение", ttl)
                    if (added) {
                        BlockerRepository.log("Временное исключение: $value на ${ttl / 60000} мин")
                        if (commandServer == null) {
                            stopSelf()
                        } else {
                            reloadBlocking()
                        }
                    }
                }
            }

            else -> Unit
        }
        return START_NOT_STICKY
    }

    private fun startBlocking() {
        BlockerRepository.setStatus(BlockerStatus.Starting)
        MonitorRepository.initialize(this)
        val store = BlocklistStore(this)
        store.lastRunning = true
        PortOverrides.load(store)
        lastRuleCount = store.loadRules().count { it.enabled }

        startForegroundCompat(buildNotification(lastRuleCount))

        startJob = scope.launch {
            try {
                if (!BuildConfig.DEBUG && !BlockerController.isMlbbInstalled(this@BlockerVpnService)) {
                    throw IllegalStateException(getString(R.string.error_mlbb_missing))
                }
                if (stopping.get()) return@launch

                LibboxSetup.ensure(this@BlockerVpnService)
                val config = BlockerConfig.build(this@BlockerVpnService, sessionPort, sessionSecret)
                Libbox.checkConfig(config)
                File(filesDir, "current-config.json").writeText(config)

                lastRuleCount = BlocklistStore(this@BlockerVpnService).loadRules().count { it.enabled }
                BlockerRepository.setStatus(BlockerStatus.Starting)

                val bridge = PlatformBridge(this@BlockerVpnService)
                platformBridge = bridge
                val server = Libbox.newCommandServer(createHandler(), bridge)
                commandServer = server
                server.start()
                if (stopping.get()) {
                    runCatching { server.close() }
                    return@launch
                }
                server.startOrReloadService(config, OverrideOptions())
                if (stopping.get()) return@launch

                BlockerRepository.setStatus(BlockerStatus.Running)
                updateNotification(lastRuleCount)
                startAttempts.set(0)
                VpnNotifier.cancel(this@BlockerVpnService)
                BlockerRepository.log("Блокировка запущена. Правил: $lastRuleCount, режим обнаружения: ${BlocklistStore(this@BlockerVpnService).discoveryMode}")
                Analytics.track(
                    "vpn_enabled",
                    mapOf(
                        "profile" to (BlocklistStore(this@BlockerVpnService).selectedProfile ?: "none"),
                        "rules" to lastRuleCount,
                        "version" to BuildConfig.VERSION_NAME,
                    ),
                )
                scheduleDiscoveryEnd()
                scheduleAllowExpiry()
                matchAlertManager?.destroy()
                matchAlertManager = MatchAlertManager(
                    this@BlockerVpnService,
                    scope,
                    sessionPort,
                    sessionSecret,
                )
                startConnectionMonitor()
                startCoreLogMonitor()
            } catch (t: Throwable) {
                fail(t)
            }
        }
    }

    private fun reloadBlocking() {
        val server = commandServer ?: return
        BlockerRepository.log("Применение новых правил…")
        scope.launch {
            try {
                val config = BlockerConfig.build(this@BlockerVpnService, sessionPort, sessionSecret)
                Libbox.checkConfig(config)
                File(filesDir, "current-config.json").writeText(config)
                server.startOrReloadService(config, OverrideOptions())
                lastRuleCount = BlocklistStore(this@BlockerVpnService).loadRules().count { it.enabled }
                updateNotification(lastRuleCount)
                BlockerRepository.setStatus(BlockerStatus.Running)
                scheduleDiscoveryEnd()
                scheduleAllowExpiry()
                BlockerRepository.log("Правила обновлены. Активных правил: $lastRuleCount")
            } catch (t: Throwable) {
                val message = t.message ?: t.javaClass.simpleName
                BlockerRepository.log("Не удалось обновить правила: $message")
                BlockerRepository.setStatus(BlockerStatus.Error(message))
            }
        }
    }

    private fun stopBlocking() {
        if (!stopping.compareAndSet(false, true)) return
        started.set(false)
        BlocklistStore(this).lastRunning = false
        startJob?.cancel()
        startJob = null
        discoveryJob?.cancel()
        discoveryJob = null
        allowExpiryJob?.cancel()
        allowExpiryJob = null
        coreLogMonitor?.stop()
        coreLogMonitor = null
        matchAlertManager?.destroy()
        matchAlertManager = null
        connectionMonitor?.stop()
        connectionMonitor = null
        BlockerRepository.setStatus(BlockerStatus.Stopped)

        scope.launch {
            runCatching { commandServer?.closeService() }
            runCatching { commandServer?.close() }
            commandServer = null
            platformBridge = null
            closeTunInterface()
            DefaultNetworkMonitor.stop()

            withContext(Dispatchers.Main) {
                runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                stopSelf()
            }
            BlockerRepository.log("Блокировка остановлена")
        }
    }

    private fun fail(t: Throwable) {
        val message = t.message ?: t.javaClass.simpleName
        cleanupAfterFailure()
        val attempt = startAttempts.incrementAndGet()
        if (!stopping.get() && attempt <= MAX_START_ATTEMPTS) {
            BlockerRepository.setStatus(BlockerStatus.Starting)
            BlockerRepository.log("Ошибка запуска: $message. Повторная попытка $attempt")
            scope.launch {
                delay(1_500)
                if (stopping.get()) return@launch
                started.set(true)
                startBlocking()
            }
            return
        }

        BlockerRepository.setStatus(BlockerStatus.Error(message))
        BlockerRepository.log("Ошибка запуска: $message")
        started.set(false)
        scope.launch {
            withContext(Dispatchers.Main) {
                runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                stopSelf()
            }
        }
        VpnNotifier.notifyInterrupted(this, message)
    }

    private fun cleanupAfterFailure() {
        discoveryJob?.cancel()
        discoveryJob = null
        allowExpiryJob?.cancel()
        allowExpiryJob = null
        coreLogMonitor?.stop()
        coreLogMonitor = null
        matchAlertManager?.destroy()
        matchAlertManager = null
        connectionMonitor?.stop()
        connectionMonitor = null
        runCatching { commandServer?.closeService() }
        runCatching { commandServer?.close() }
        commandServer = null
        platformBridge = null
        closeTunInterface()
        DefaultNetworkMonitor.stop()
    }

    private fun createHandler(): CommandServerHandler {
        return object : CommandServerHandler {
            override fun connectSSHAgent(): Int = -1

            override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus()

            override fun serviceReload() {
                reloadBlocking()
            }

            override fun serviceStop() {
                stopBlocking()
            }

            override fun setSystemProxyEnabled(enabled: Boolean) = Unit

            override fun triggerNativeCrash() = Unit

            override fun writeDebugMessage(message: String) {
                if (message.isNotBlank()) {
                    BlockerRepository.log(message)
                }
            }
        }
    }

    fun createTun(options: TunOptions): Int {
        if (prepare(this) != null && !BuildConfig.DEBUG) {
            throw IllegalStateException(getString(R.string.error_vpn_permission))
        }
        closeTunInterface()

        val builder = Builder()
            .setSession("MLBB Blocker")
            .setMtu(options.mtu)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        addAddresses(builder, options)
        if (options.autoRoute) {
            addDns(builder, options)
            addRoutes(builder, options)
            addPackages(builder, options)
        }

        val pfd = builder.establish()
            ?: throw IllegalStateException(getString(R.string.error_vpn_permission))
        tunInterface = pfd
        return pfd.fd
    }

    private fun addAddresses(builder: Builder, options: TunOptions) {
        val inet4 = options.inet4Address
        if (inet4 != null) {
            while (inet4.hasNext()) {
                val prefix = inet4.next()
                builder.addAddress(prefix.address(), prefix.prefix())
            }
        }
        val inet6 = options.inet6Address
        if (inet6 != null) {
            while (inet6.hasNext()) {
                val prefix = inet6.next()
                builder.addAddress(prefix.address(), prefix.prefix())
            }
        }
    }

    private fun addDns(builder: Builder, options: TunOptions) {
        if (options.dnsMode?.value == Libbox.DNSModeDisabled) return
        var added = false
        val dns = options.dnsServerAddress
        if (dns != null) {
            while (dns.hasNext()) {
                val server = dns.next().substringBefore('%')
                if (server.isNotBlank()) {
                    builder.addDnsServer(server)
                    added = true
                }
            }
        }
        if (!added) {
            builder.addDnsServer("172.19.0.2")
        }
    }

    private fun addRoutes(builder: Builder, options: TunOptions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasV4 = addRouteIterator(builder, options.inet4RouteAddress)
            if (!hasV4) builder.addRoute("0.0.0.0", 0)

            val hasV6 = addRouteIterator(builder, options.inet6RouteAddress)
            if (!hasV6) builder.addRoute("::", 0)

            addExcludeRouteIterator(builder, options.inet4RouteExcludeAddress)
            addExcludeRouteIterator(builder, options.inet6RouteExcludeAddress)
        } else {
            val hasV4 = addRangeIterator(builder, options.inet4RouteRange)
            if (!hasV4) builder.addRoute("0.0.0.0", 0)
            val hasV6 = addRangeIterator(builder, options.inet6RouteRange)
            if (!hasV6) builder.addRoute("::", 0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun addRouteIterator(builder: Builder, iterator: io.nekohasekai.libbox.RoutePrefixIterator?): Boolean {
        if (iterator == null) return false
        var added = false
        while (iterator.hasNext()) {
            addRoutePrefix(builder, iterator.next())
            added = true
        }
        return added
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun addExcludeRouteIterator(builder: Builder, iterator: io.nekohasekai.libbox.RoutePrefixIterator?) {
        if (iterator == null) return
        while (iterator.hasNext()) {
            val prefix = iterator.next()
            runCatching {
                builder.excludeRoute(IpPrefix(InetAddress.getByName(prefix.address()), prefix.prefix()))
            }
        }
    }

    private fun addRangeIterator(builder: Builder, iterator: io.nekohasekai.libbox.RoutePrefixIterator?): Boolean {
        if (iterator == null) return false
        var added = false
        while (iterator.hasNext()) {
            val prefix = iterator.next()
            runCatching {
                builder.addRoute(prefix.address(), prefix.prefix())
                added = true
            }
        }
        return added
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun addRoutePrefix(builder: Builder, prefix: RoutePrefix) {
        runCatching {
            builder.addRoute(IpPrefix(InetAddress.getByName(prefix.address()), prefix.prefix()))
        }
    }

    private fun addPackages(builder: Builder, options: TunOptions) {
        val added = mutableSetOf<String>()
        var addedMlbb = 0
        fun addAllowed(packageName: String) {
            if (!added.add(packageName)) return
            val success = runCatching { builder.addAllowedApplication(packageName) }.isSuccess
            if (success && packageName in SingBoxConfigBuilder.MLBB_PACKAGES) {
                addedMlbb++
            }
        }

        val include = options.includePackage
        if (include != null) {
            while (include.hasNext()) {
                addAllowed(include.next())
            }
        }
        SingBoxConfigBuilder.MLBB_PACKAGES.forEach(::addAllowed)

        if (addedMlbb == 0 && !BuildConfig.DEBUG) {
            throw IllegalStateException(getString(R.string.error_mlbb_missing))
        }
        if (BuildConfig.DEBUG) {
            runCatching { builder.addAllowedApplication("com.android.shell") }
        }

        val exclude = options.excludePackage
        if (exclude != null) {
            while (exclude.hasNext()) {
                val packageName = exclude.next()
                runCatching { builder.addDisallowedApplication(packageName) }
            }
        }
    }

    private fun closeTunInterface() {
        runCatching {
            tunInterface?.close()
        }
        tunInterface = null
    }

    private fun scheduleDiscoveryEnd() {
        discoveryJob?.cancel()
        discoveryJob = null
        val store = BlocklistStore(this)
        val until = store.discoveryUntil
        val now = System.currentTimeMillis()
        if (until <= now) return
        val wait = until - now
        BlockerRepository.log("Сбор серверов: ещё ${wait / 1000} с")
        discoveryJob = scope.launch {
            delay(wait)
            if (stopping.get()) return@launch
            store.discoveryUntil = 0
            BlockerRepository.log("Сбор завершён, правила снова активны")
            reloadBlocking()
        }
    }

    private fun scheduleAllowExpiry() {
        allowExpiryJob?.cancel()
        allowExpiryJob = null
        val store = BlocklistStore(this)
        val rules = store.loadAllowRules().filter { it.expiresAt > 0 }
        val next = rules.minByOrNull { it.expiresAt } ?: return
        val wait = next.expiresAt - System.currentTimeMillis()
        if (wait <= 0) {
            store.activeAllowValues()
            return
        }
        allowExpiryJob = scope.launch {
            delay(wait + 250)
            if (stopping.get()) return@launch
            store.activeAllowValues()
            BlockerRepository.log("Временное исключение истекло: ${next.value}")
            reloadBlocking()
        }
    }

    private fun startConnectionMonitor() {
        connectionMonitor?.stop()
        connectionMonitor = ConnectionMonitor(
            scope,
            { message -> BlockerRepository.log(message) },
            { ip, port, network, blocked ->
                if (!stopping.get()) {
                    matchAlertManager?.onMatch(ip, port, network, blocked)
                }
            },
            { ip, port, network ->
                if (!stopping.get()) {
                    UnknownPortDetector.onConnection(this@BlockerVpnService, ip, port)
                    if (GamePorts.isMatchPort(port) || GamePorts.isSafePort(port)) {
                        val geo = GeoIpRepository.lookup(ip)
                        Analytics.recordServerIp(
                            ip = ip,
                            port = port,
                            protocol = network,
                            city = geo?.city,
                            country = geo?.country,
                        )
                    }
                }
            },
        ).also { it.start() }
    }

    private fun startCoreLogMonitor() {
        coreLogMonitor?.stop()
        coreLogMonitor = CoreLogMonitor(scope, File(filesDir, "singbox.log")) { line ->
            if (line.contains("connection to") || line.contains("reject") || line.contains("drop")) {
                BlockerRepository.log("core: ${line.take(500)}")
            }
        }.also { it.start() }
    }

    private fun startForegroundCompat(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Throwable) {
            runCatching { startForeground(NOTIFICATION_ID, notification) }
        }
    }

    private fun updateNotification(ruleCount: Int) {
        runCatching {
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, buildNotification(ruleCount))
        }
    }

    private fun buildNotification(ruleCount: Int): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, BlockerVpnService::class.java).setAction(BlockerController.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (ruleCount > 0) {
            getString(R.string.notification_text, ruleCount)
        } else {
            getString(R.string.status_running)
        }
        return NotificationCompat.Builder(this, MlbbBlockerApp.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.notification_stop), stopIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onRevoke() {
        BlockerRepository.log("VPN-разрешение отозвано системой")
        stopBlocking()
        super.onRevoke()
    }

    override fun onDestroy() {
        discoveryJob?.cancel()
        discoveryJob = null
        allowExpiryJob?.cancel()
        allowExpiryJob = null
        coreLogMonitor?.stop()
        coreLogMonitor = null
        matchAlertManager?.destroy()
        matchAlertManager = null
        connectionMonitor?.stop()
        connectionMonitor = null
        runCatching { commandServer?.closeService() }
        runCatching { commandServer?.close() }
        commandServer = null
        platformBridge = null
        closeTunInterface()
        DefaultNetworkMonitor.stop()
        scope.cancel()
        if (BlockerRepository.status.value is BlockerStatus.Running) {
            BlockerRepository.setStatus(BlockerStatus.Stopped)
        }
        super.onDestroy()
    }
}
