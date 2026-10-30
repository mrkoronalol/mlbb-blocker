package ru.mlbb.blocker

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import ru.mlbb.blocker.config.BlockerConfig
import ru.mlbb.blocker.data.BlockPreset
import ru.mlbb.blocker.data.BlockRule
import ru.mlbb.blocker.data.BlockerRepository
import ru.mlbb.blocker.data.BlockerStatus
import ru.mlbb.blocker.data.BlocklistParser
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.data.AllowRule
import ru.mlbb.blocker.data.Cidr
import ru.mlbb.blocker.data.ConnectionStat
import ru.mlbb.blocker.data.GamePorts
import ru.mlbb.blocker.data.HealthIssue
import ru.mlbb.blocker.data.BlockerHealth
import ru.mlbb.blocker.data.InstallResult
import ru.mlbb.blocker.data.analytics.Analytics
import ru.mlbb.blocker.data.MlbbPackageInfo
import ru.mlbb.blocker.data.db.BlockedAttemptEntity
import ru.mlbb.blocker.data.db.MatchSessionEntity
import ru.mlbb.blocker.data.db.MonitorRepository
import ru.mlbb.blocker.data.geo.GeoIpRepository
import ru.mlbb.blocker.data.geo.GeoIpStatus
import ru.mlbb.blocker.data.NetworkPing
import ru.mlbb.blocker.data.Presets
import ru.mlbb.blocker.data.RegionProfiles
import ru.mlbb.blocker.data.UpdateChecker
import ru.mlbb.blocker.data.UpdateInfo
import ru.mlbb.blocker.data.UpdateDownloadState
import ru.mlbb.blocker.data.UpdateInstaller
import ru.mlbb.blocker.data.UpdateNotifier
import ru.mlbb.blocker.data.backup.BackupCodec
import ru.mlbb.blocker.data.backup.BackupData
import ru.mlbb.blocker.data.backup.BackupRepository
import ru.mlbb.blocker.data.backup.BackupSummary
import ru.mlbb.blocker.data.rules.RuleConfigRepository
import ru.mlbb.blocker.data.rules.RulesRefreshResult
import ru.mlbb.blocker.data.rules.RulesSource
import ru.mlbb.blocker.data.rules.RulesStatus
import ru.mlbb.blocker.data.remote.RemoteConfigRepository
import ru.mlbb.blocker.vpn.BlockerController
import ru.mlbb.blocker.vpn.MatchAlertManager

data class UiState(
    val status: BlockerStatus = BlockerStatus.Stopped,
    val rules: List<BlockRule> = emptyList(),
    val discovery: Boolean = true,
    val connections: List<ConnectionStat> = emptyList(),
    val logs: List<String> = emptyList(),
    val mlbbInstalled: Boolean = true,
    val message: String? = null,
    val pingingKeys: Set<String> = emptySet(),
    val selectedProfile: String? = null,
    val profileRuleCount: Int = 0,
    val effectiveRuleCount: Int = 0,
    val fastReject: Boolean = true,
    val mlbbPackages: List<MlbbPackageInfo> = emptyList(),
    val vpnPermissionGranted: Boolean = false,
    val currentUserId: Int = 0,
    val activeMatchId: Long? = null,
    val matchSessions: List<MatchSessionEntity> = emptyList(),
    val discoveryUntil: Long = 0L,
    val discoveryActive: Boolean = false,
    val geoIpStatus: GeoIpStatus = GeoIpStatus.NotDownloaded,
    val allowRules: List<AllowRule> = emptyList(),
    val blockedAttempts: List<BlockedAttemptEntity> = emptyList(),
    val currentMatch: ConnectionStat? = null,
    val matchAlerts: Boolean = true,
    val hideStartupAlerts: Boolean = true,
    val autoStart: Boolean = true,
    val allowStartupServer: Boolean = true,
    val showOnboarding: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val updateChecking: Boolean = false,
    val updateDownload: UpdateDownloadState = UpdateDownloadState.Idle,
    val updateInstalling: Boolean = false,
    val pendingImport: BackupSummary? = null,
    val backupBusy: Boolean = false,
    val healthIssues: List<HealthIssue> = emptyList(),
    val rulesStatus: RulesStatus = RulesStatus(),
    val unknownPortAlerts: Boolean = true,
    val stableOnlyAlerts: Boolean = false,
    val maintenance: Boolean = false,
    val remoteNotice: String = "",
    val featureMetaEnabled: Boolean = true,
    val updateRequired: Boolean = false,
    val minAppVersion: String = "",
) {
    val isBusy: Boolean get() = status is BlockerStatus.Starting
    val isRunning: Boolean get() = status is BlockerStatus.Running
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = BlocklistStore(application)
    private var demoMatchAlert: MatchAlertManager? = null
    private var pendingBackupData: BackupData? = null
    private val _ui = MutableStateFlow(
        UiState(
            rules = store.loadRules(),
            discovery = store.discoveryMode,
            mlbbInstalled = BlockerController.isMlbbInstalled(application),
            selectedProfile = store.selectedProfile,
            profileRuleCount = profileValues().size,
            effectiveRuleCount = effectiveRuleCount(),
            fastReject = store.fastReject,
            mlbbPackages = loadPackageInfo(),
            vpnPermissionGranted = runCatching { VpnService.prepare(application) == null }.getOrDefault(false),
            currentUserId = android.os.Process.myUid() / 100000,
            discoveryUntil = store.discoveryUntil,
            discoveryActive = store.discoveryMode || store.discoveryUntil > System.currentTimeMillis(),
            geoIpStatus = GeoIpRepository.status.value,
            allowRules = store.loadAllowRules(),
            matchAlerts = store.matchAlerts,
            hideStartupAlerts = store.hideStartupAlerts,
            autoStart = store.autoStart,
            allowStartupServer = store.allowStartupServer,
            showOnboarding = !store.onboardingDone,
            overlayPermissionGranted = runCatching { Settings.canDrawOverlays(application) }
                .getOrDefault(false),
            unknownPortAlerts = store.unknownPortAlerts,
            stableOnlyAlerts = store.stableOnlyAlerts,
        ),
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            BlockerRepository.status.collect { status ->
                _ui.update { it.copy(status = status) }
                refreshHealth()
            }
        }
        viewModelScope.launch {
            MonitorRepository.observeEndpoints().collect { connections ->
                val now = System.currentTimeMillis()
                val current = connections
                    .filter { GamePorts.isMatchPort(it.port) && now - it.lastSeen < 15_000 && it.observations > 0 }
                    .maxByOrNull { it.lastSeen }
                _ui.update { it.copy(connections = connections, currentMatch = current) }
            }
        }
        viewModelScope.launch {
            BlockerRepository.logs.collect { logs ->
                _ui.update { it.copy(logs = logs) }
            }
        }
        viewModelScope.launch {
            MonitorRepository.observeMatchSessions().collect { sessions ->
                _ui.update { it.copy(matchSessions = sessions) }
            }
        }
        viewModelScope.launch {
            MonitorRepository.observeBlockedAttempts().collect { attempts ->
                _ui.update { it.copy(blockedAttempts = attempts) }
            }
        }
        viewModelScope.launch {
            RuleConfigRepository.status.collect { status ->
                _ui.update { it.copy(rulesStatus = status) }
            }
        }
        viewModelScope.launch {
            RemoteConfigRepository.state.collect { config ->
                _ui.update {
                    it.copy(
                        maintenance = config.maintenance,
                        remoteNotice = config.notice,
                        featureMetaEnabled = config.featureMetaEnabled,
                        updateRequired = config.updateRequired,
                        minAppVersion = config.minAppVersion,
                    )
                }
            }
        }
        var geoEnriched = false
        viewModelScope.launch {
            GeoIpRepository.status.collect { status ->
                _ui.update { it.copy(geoIpStatus = status) }
                if (status is GeoIpStatus.Ready && !geoEnriched) {
                    geoEnriched = true
                    MonitorRepository.enrichGeoInfo { GeoIpRepository.lookup(it) }
                }
            }
        }
        viewModelScope.launch {
            if (BlockerRepository.status.value is BlockerStatus.Running) {
                refreshRules()
            }
        }
        checkForUpdates()
        refreshHealth()
    }

    fun checkForUpdates() {
        if (_ui.value.updateChecking) return
        _ui.update { it.copy(updateChecking = true) }
        viewModelScope.launch {
            val info = UpdateChecker.checkLatest(BuildConfig.VERSION_NAME)
            _ui.update { it.copy(updateInfo = info, updateChecking = false) }
            if (info != null) {
                UpdateNotifier.notifyIfNeeded(getApplication(), info)
            }
        }
    }

    fun downloadUpdate() {
        val info = _ui.value.updateInfo ?: return
        if (_ui.value.updateDownload is UpdateDownloadState.Downloading) return
        _ui.update {
            it.copy(updateDownload = UpdateDownloadState.Downloading(0, 0, info.size ?: 0))
        }
        viewModelScope.launch {
            try {
                BackupRepository.writeAutoBackup(getApplication())
                val file = UpdateInstaller.download(getApplication(), info) { progress, received, total ->
                    _ui.update {
                        it.copy(
                            updateDownload = UpdateDownloadState.Downloading(
                                progress,
                                received,
                                total,
                            ),
                        )
                    }
                }
                _ui.update { it.copy(updateDownload = UpdateDownloadState.Ready(file)) }
                showMessage("APK загружен и проверен")
            } catch (t: Throwable) {
                _ui.update {
                    it.copy(
                        updateDownload = UpdateDownloadState.Error(
                            t.message ?: t.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    }

    fun installUpdate() {
        val ready = _ui.value.updateDownload as? UpdateDownloadState.Ready ?: return
        if (_ui.value.updateInstalling) return
        _ui.update { it.copy(updateInstalling = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                UpdateInstaller.install(getApplication(), ready.file)
            }
            _ui.update { it.copy(updateInstalling = false) }
            when (result) {
                InstallResult.Started -> showMessage("Открыт системный установщик")
                InstallResult.NeedPermission ->
                    showMessage("Разрешите установку из этого источника и нажмите снова")
                InstallResult.SignatureMismatch ->
                    showMessage("Подпись APK не совпадает. Сохраните копию и переустановите вручную")
                is InstallResult.Error -> showMessage("Ошибка установки: ${result.message}")
            }
        }
    }

    fun exportBackup(uri: Uri) {
        _ui.update { it.copy(backupBusy = true) }
        viewModelScope.launch {
            try {
                val summary = BackupRepository.export(getApplication(), uri)
                showMessage("Резервная копия сохранена: ${summary.rules} правил, ${summary.endpoints} IP")
            } catch (t: Throwable) {
                showMessage("Не удалось сохранить копию: ${t.message ?: "ошибка"}")
            } finally {
                _ui.update { it.copy(backupBusy = false) }
            }
        }
    }

    fun prepareImport(uri: Uri) {
        _ui.update { it.copy(backupBusy = true) }
        viewModelScope.launch {
            try {
                val data = BackupRepository.read(getApplication(), uri)
                pendingBackupData = data
                _ui.update { it.copy(pendingImport = BackupCodec.summary(data)) }
            } catch (t: Throwable) {
                showMessage("Не удалось прочитать копию: ${t.message ?: "ошибка"}")
            } finally {
                _ui.update { it.copy(backupBusy = false) }
            }
        }
    }

    fun applyImport(merge: Boolean) {
        val data = pendingBackupData ?: return
        _ui.update { it.copy(backupBusy = true) }
        viewModelScope.launch {
            try {
                BackupRepository.apply(getApplication(), data, merge)
                pendingBackupData = null
                _ui.update { it.copy(pendingImport = null) }
                refreshRules()
                showMessage(if (merge) "Данные объединены" else "Данные восстановлены")
            } catch (t: Throwable) {
                showMessage("Не удалось восстановить копию: ${t.message ?: "ошибка"}")
            } finally {
                _ui.update { it.copy(backupBusy = false) }
            }
        }
    }

    fun cancelImport() {
        pendingBackupData = null
        _ui.update { it.copy(pendingImport = null) }
    }

    fun startVpn() {
        val context = getApplication<Application>()
        if (!BlockerController.isMlbbInstalled(context)) {
            showMessage(context.getString(R.string.error_mlbb_missing))
            return
        }
        val port = BlockerConfig.newPort()
        val secret = BlockerConfig.newSecret()
        BlockerController.start(context, port, secret)
        _ui.update { it.copy(message = null) }
    }

    fun stopVpn() {
        BlockerController.stop(getApplication())
    }

    fun toggleDiscovery(enabled: Boolean) {
        store.discoveryMode = enabled
        store.discoveryUntil = 0
        _ui.update {
            it.copy(
                discovery = enabled,
                discoveryUntil = 0,
                discoveryActive = enabled,
            )
        }
        if (isRunning()) {
            BlockerController.reload(getApplication())
        }
    }

    fun startDiscovery(seconds: Int = 60) {
        val until = System.currentTimeMillis() + seconds * 1000L
        store.discoveryUntil = until
        _ui.update { it.copy(discoveryUntil = until, discoveryActive = true) }
        if (isRunning()) {
            BlockerController.reload(getApplication())
            showMessage("Сбор серверов на $seconds секунд")
        } else {
            showMessage("Сбор включён. Включите блокировку, чтобы начать.")
        }
    }

    fun stopDiscovery() {
        store.discoveryUntil = 0
        store.discoveryMode = false
        _ui.update { it.copy(discovery = false, discoveryUntil = 0, discoveryActive = false) }
        reloadIfRunning()
    }

    fun startMatch(gamePackage: String) {
        viewModelScope.launch {
            val profileIds = store.selectedProfile?.let { setOf(it) } ?: emptySet()
            val id = MonitorRepository.startMatchSession(gamePackage, profileIds)
            _ui.update { it.copy(activeMatchId = id.takeIf { value -> value > 0 }) }
        }
    }

    fun downloadGeoIp() {
        viewModelScope.launch {
            val success = GeoIpRepository.download()
            if (success) {
                GeoIpRepository.status.value.let { status ->
                    if (status is GeoIpStatus.Ready) {
                        _ui.update { it.copy(geoIpStatus = status) }
                    }
                }
                MonitorRepository.enrichGeoInfo { GeoIpRepository.lookup(it) }
                showMessage("GeoIP база загружена")
            } else {
                showMessage("Не удалось загрузить GeoIP базу")
            }
        }
    }

    fun refreshRemoteRules() {
        if (_ui.value.rulesStatus.checking) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = RuleConfigRepository.refresh(context)
            val message = when (result) {
                is RulesRefreshResult.Updated ->
                    context.getString(R.string.rules_updated, result.updatedAt)

                RulesRefreshResult.UpToDate -> context.getString(R.string.rules_up_to_date)
                is RulesRefreshResult.Rejected ->
                    context.getString(R.string.rules_rejected, result.reason)

                is RulesRefreshResult.NetworkError ->
                    context.getString(R.string.rules_network_error, result.reason)
            }
            showMessage(message)
            if (result is RulesRefreshResult.Updated) {
                refreshRules()
                reloadIfRunning()
            }
            refreshHealth()
        }
    }

    fun setUnknownPortAlerts(enabled: Boolean) {
        store.unknownPortAlerts = enabled
        _ui.update { it.copy(unknownPortAlerts = enabled) }
    }

    fun setStableOnlyAlerts(enabled: Boolean) {
        store.stableOnlyAlerts = enabled
        _ui.update { it.copy(stableOnlyAlerts = enabled) }
    }

    /** Кнопка «Исправить» в баннере состояния защиты. */
    fun runHealthAction() {
        val issue = _ui.value.healthIssues.firstOrNull() ?: return
        when (BlockerHealth.recoveryAction(issue)) {
            BlockerHealth.RecoveryAction.START_VPN -> startVpn()
            BlockerHealth.RecoveryAction.RELOAD_GEOIP -> downloadGeoIp()
            BlockerHealth.RecoveryAction.REFRESH_RULES -> refreshRemoteRules()
        }
    }

    fun refreshHealth() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = BlockerHealth.inspect(
                context,
                expectedRunning = BlockerHealth.isExpectedRunning(context),
                running = BlockerRepository.status.value is BlockerStatus.Running,
            )
            val issues = BlockerHealth.issues(snapshot)
            _ui.update { it.copy(healthIssues = issues) }
        }
    }

    fun allowTemporarily(ip: String, minutes: Int = 5) {
        val value = if (ip.contains(':')) "$ip/128" else "$ip/32"
        if (store.addAllowRule(value, "временное разрешение", minutes * 60_000L)) {
            refreshRules()
            reloadIfRunning()
            showMessage("IP $ip разрешён на $minutes мин")
        }
    }

    fun addAllowRule(value: String, note: String = "", permanent: Boolean = false): Boolean {
        val added = store.addAllowRule(value, note, if (permanent) 0 else 5 * 60_000L)
        if (added) {
            refreshRules()
            reloadIfRunning()
            showMessage("Исключение добавлено")
        }
        return added
    }

    fun removeAllowRule(id: String) {
        store.removeAllowRule(id)
        refreshRules()
        reloadIfRunning()
    }

    fun finishMatch(result: String) {
        val id = _ui.value.activeMatchId ?: return
        MonitorRepository.finishMatchSession(id, result)
        _ui.update { it.copy(activeMatchId = null) }
        showMessage("Результат матча сохранён")
    }

    fun selectProfile(profileId: String) {
        val profile = RegionProfiles.byId(profileId) ?: return
        if (store.selectedProfile == profileId) return
        store.selectProfile(profileId)
        Analytics.track("profile_selected", mapOf("profile" to profileId))
        refreshRules()
        reloadIfRunning()
        showMessage(
            getApplication<Application>().getString(R.string.region_enabled, profile.title),
        )
    }

    fun addRule(value: String, note: String = ""): Boolean {
        val added = store.addRules(listOf(value), note)
        if (added > 0) {
            refreshRules()
            reloadIfRunning()
            showMessage(getApplication<Application>().getString(R.string.rule_added))
            return true
        }
        return false
    }

    fun addRules(text: String): Int {
        val values = BlocklistParser.parseLines(text)
        if (values.isEmpty()) {
            showMessage(getApplication<Application>().getString(R.string.rules_not_added))
            return 0
        }
        val added = store.addRules(values)
        if (added > 0) {
            refreshRules()
            reloadIfRunning()
            showMessage(getApplication<Application>().getString(R.string.rules_added, added))
        } else {
            showMessage(getApplication<Application>().getString(R.string.rules_not_added))
        }
        return added
    }

    fun addPreset(preset: BlockPreset) {
        val added = store.addRulesDetailed(preset.rules)
        if (added > 0) {
            refreshRules()
            reloadIfRunning()
            showMessage(
                getApplication<Application>().getString(R.string.preset_added, preset.title, added),
            )
        } else {
            showMessage(getApplication<Application>().getString(R.string.rules_not_added))
        }
    }

    fun addConnection(connection: ConnectionStat, subnet: Boolean) {
        val value = if (subnet) {
            val ipv4 = Cidr.subnet24(connection.ip)
            ipv4 ?: Cidr.subnet64(connection.ip)
        } else {
            Cidr.withPrefix(connection.ip, if (connection.ip.contains(':')) 128 else 32)
        }
        if (value == null) {
            showMessage(getApplication<Application>().getString(R.string.invalid_cidr))
        } else {
            addRule(value, "из журнала: ${connection.displayAddress}")
        }
    }

    fun setRuleEnabled(id: String, enabled: Boolean) {
        store.setEnabled(id, enabled)
        refreshRules()
        reloadIfRunning()
    }

    fun removeRule(id: String) {
        store.removeRule(id)
        refreshRules()
        reloadIfRunning()
    }

    fun clearRules() {
        store.clearRules()
        refreshRules()
        reloadIfRunning()
        showMessage(getApplication<Application>().getString(R.string.blocklist_cleared))
    }

    fun clearConnections() {
        MonitorRepository.clearMonitoring()
        showMessage(getApplication<Application>().getString(R.string.connections_cleared))
    }

    fun ping(connection: ConnectionStat) {
        if (connection.key in _ui.value.pingingKeys) return
        viewModelScope.launch {
            pingOne(connection)
        }
    }

    fun pingAll() {
        val gameServers = _ui.value.connections.filter { GamePorts.isGamePort(it.port) }
        val list = gameServers.ifEmpty { _ui.value.connections }
        if (list.isEmpty()) return
        viewModelScope.launch {
            val semaphore = Semaphore(4)
            coroutineScope {
                list.map { connection ->
                    async {
                        semaphore.withPermit {
                            pingOne(connection)
                        }
                    }
                }.awaitAll()
            }
        }
    }

    private suspend fun pingOne(connection: ConnectionStat) {
        val key = connection.key
        _ui.update { it.copy(pingingKeys = it.pingingKeys + key) }
        try {
            val result = withContext(Dispatchers.IO) {
                NetworkPing.tcp(connection.ip, connection.port)
            }
            MonitorRepository.updatePing(connection, result)
            Analytics.recordServerIp(
                ip = connection.ip,
                port = connection.port,
                protocol = connection.network,
                city = connection.city,
                country = connection.country,
                pingMs = result.ms,
            )
        } finally {
            _ui.update { it.copy(pingingKeys = it.pingingKeys - key) }
        }
    }

    fun clearLogs() {
        BlockerRepository.clearLogs()
    }

    fun showMessage(message: String) {
        _ui.update { it.copy(message = message) }
    }

    fun consumeMessage() {
        _ui.update { it.copy(message = null) }
    }

    fun refreshMlbbInstalled() {
        val packages = loadPackageInfo()
        _ui.update {
            it.copy(
                mlbbInstalled = packages.any { info -> info.installed && info.enabled },
                mlbbPackages = packages,
                vpnPermissionGranted = runCatching { VpnService.prepare(getApplication()) == null }
                    .getOrDefault(false),
                currentUserId = android.os.Process.myUid() / 100000,
                matchAlerts = store.matchAlerts,
                hideStartupAlerts = store.hideStartupAlerts,
                autoStart = store.autoStart,
                allowStartupServer = store.allowStartupServer,
                showOnboarding = !store.onboardingDone,
                overlayPermissionGranted = runCatching {
                    Settings.canDrawOverlays(getApplication())
                }.getOrDefault(false),
            )
        }
    }

    fun setFastReject(enabled: Boolean) {
        store.fastReject = enabled
        _ui.update { it.copy(fastReject = enabled) }
        reloadIfRunning()
    }

    fun setMatchAlerts(enabled: Boolean) {
        store.matchAlerts = enabled
        _ui.update { it.copy(matchAlerts = enabled) }
        showMessage(if (enabled) "Уведомления о матче включены" else "Уведомления о матче выключены")
    }

    fun setHideStartupAlerts(enabled: Boolean) {
        store.hideStartupAlerts = enabled
        _ui.update { it.copy(hideStartupAlerts = enabled) }
    }

    fun setAutoStart(enabled: Boolean) {
        store.autoStart = enabled
        _ui.update { it.copy(autoStart = enabled) }
    }

    fun setAllowStartupServer(enabled: Boolean) {
        store.allowStartupServer = enabled
        _ui.update { it.copy(allowStartupServer = enabled) }
        reloadIfRunning()
    }

    fun finishOnboarding() {
        store.onboardingDone = true
        _ui.update { it.copy(showOnboarding = false) }
    }

    fun showMatchDemo() {
        demoMatchAlert?.destroy()
        demoMatchAlert = MatchAlertManager(getApplication(), viewModelScope).also { it.showDemo() }
    }

    override fun onCleared() {
        demoMatchAlert?.destroy()
        demoMatchAlert = null
        super.onCleared()
    }

    private fun loadPackageInfo(): List<MlbbPackageInfo> {
        val packageManager = getApplication<Application>().packageManager
        return ru.mlbb.blocker.config.SingBoxConfigBuilder.MLBB_PACKAGES.map { packageName ->
            try {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                MlbbPackageInfo(
                    packageName = packageName,
                    installed = applicationInfo.flags and ApplicationInfo.FLAG_INSTALLED != 0,
                    enabled = applicationInfo.enabled,
                    versionName = packageInfo.versionName,
                    uid = applicationInfo.uid,
                )
            } catch (_: PackageManager.NameNotFoundException) {
                MlbbPackageInfo(packageName, installed = false, enabled = false, versionName = null, uid = null)
            }
        }
    }

    private fun refreshRules() {
        _ui.update {
            it.copy(
                rules = store.loadRules(),
                discovery = store.discoveryMode,
                selectedProfile = store.selectedProfile,
                profileRuleCount = profileValues().size,
                effectiveRuleCount = effectiveRuleCount(),
                fastReject = store.fastReject,
                discoveryUntil = store.discoveryUntil,
                discoveryActive = store.discoveryMode || store.discoveryUntil > System.currentTimeMillis(),
                allowRules = store.loadAllowRules(),
                matchAlerts = store.matchAlerts,
                hideStartupAlerts = store.hideStartupAlerts,
                autoStart = store.autoStart,
                allowStartupServer = store.allowStartupServer,
                showOnboarding = !store.onboardingDone,
                unknownPortAlerts = store.unknownPortAlerts,
                stableOnlyAlerts = store.stableOnlyAlerts,
            )
        }
    }

    private fun profileValues(): Set<String> {
        return store.selectedProfile
            ?.let(RegionProfiles::byId)
            ?.rules
            ?.map { it.first }
            ?.toSet()
            ?: emptySet()
    }

    private fun effectiveRuleCount(): Int {
        val userValues = store.activeValues().toSet()
        return (userValues + profileValues()).size
    }

    private fun reloadIfRunning() {
        if (isRunning()) {
            BlockerController.reload(getApplication())
        }
    }

    private fun isRunning(): Boolean = BlockerRepository.status.value is BlockerStatus.Running
}
