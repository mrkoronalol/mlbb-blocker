package ru.mlbb.blocker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.BuildConfig
import ru.mlbb.blocker.R
import ru.mlbb.blocker.UiState
import ru.mlbb.blocker.data.BlockPreset
import ru.mlbb.blocker.data.BlockerStatus
import ru.mlbb.blocker.data.ConnectionStat
import ru.mlbb.blocker.data.HealthIssue
import ru.mlbb.blocker.data.RegionProfile
import ru.mlbb.blocker.data.RegionProfiles
import ru.mlbb.blocker.data.UpdateDownloadState
import ru.mlbb.blocker.data.UpdateInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockerApp(
    ui: UiState,
    onToggle: () -> Unit,
    onDiscoveryChange: (Boolean) -> Unit,
    onSelectProfile: (String) -> Unit,
    onAddRule: (String, String) -> Boolean,
    onAddRules: (String) -> Int,
    onAddPreset: (BlockPreset) -> Unit,
    onRemoveRule: (String) -> Unit,
    onSetRuleEnabled: (String, Boolean) -> Unit,
    onClearRules: () -> Unit,
    onAddConnection: (ConnectionStat, Boolean) -> Unit,
    onPing: (ConnectionStat) -> Unit,
    onPingAll: () -> Unit,
    onClearConnections: () -> Unit,
    onClearLogs: () -> Unit,
    onFastRejectChange: (Boolean) -> Unit,
    onMatchAlertsChange: (Boolean) -> Unit,
    onHideStartupAlertsChange: (Boolean) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onAllowStartupServerChange: (Boolean) -> Unit,
    onFinishOnboarding: () -> Unit,
    onShowMatchDemo: () -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onDownloadGeoIp: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onApplyImport: (Boolean) -> Unit,
    onCancelImport: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onStartGame: (String) -> Unit,
    onMatchResult: (String) -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onAllow: (String, Int) -> Unit,
    onRemoveAllow: (String) -> Unit,
    onMessageShown: () -> Unit,
    onShowMessage: (String) -> Unit,
    onHealthAction: () -> Unit,
    onRefreshRules: () -> Unit,
    onUnknownPortAlertsChange: (Boolean) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.message) {
        val message = ui.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onMessageShown()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showAbout) "О приложении" else stringResource(R.string.main_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (showAbout) {
                        IconButton(onClick = { showAbout = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (!showAbout) {
                        IconButton(onClick = { showAbout = true }) {
                            Icon(Icons.Default.Info, contentDescription = "О приложении")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            if (!showAbout) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationItem(0, tab, "Главная", Icons.Default.Home) {
                        showAbout = false
                        tab = it
                    }
                    NavigationItem(1, tab, "Матч", Icons.Default.PlayArrow) {
                        showAbout = false
                        tab = it
                    }
                    NavigationItem(2, tab, "Монитор", Icons.Default.Search) {
                        showAbout = false
                        tab = it
                    }
                    NavigationItem(3, tab, "Блоклист", Icons.Default.Lock) {
                        showAbout = false
                        tab = it
                    }
                    NavigationItem(4, tab, "Настройки", Icons.Default.Settings) {
                        showAbout = false
                        tab = it
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showAbout) {
                AboutScreen()
            } else {
                when (tab) {
                    0 -> HomeScreen(
                        ui = ui,
                        onToggle = onToggle,
                        onDiscoveryChange = onDiscoveryChange,
                        onSelectProfile = onSelectProfile,
                        onFinishOnboarding = onFinishOnboarding,
                        onDownloadUpdate = onDownloadUpdate,
                        onInstallUpdate = onInstallUpdate,
                        onOpenAbout = { showAbout = true },
                        onHealthAction = onHealthAction,
                    )
                    1 -> MatchScreen(
                        ui = ui,
                        onStartGame = onStartGame,
                        onSelectProfile = onSelectProfile,
                        onFastRejectChange = onFastRejectChange,
                        onStartDiscovery = onStartDiscovery,
                        onStopDiscovery = onStopDiscovery,
                        onMatchResult = onMatchResult,
                        onAllow = onAllow,
                        onRemoveAllow = onRemoveAllow,
                    )
                    2 -> ConnectionsScreen(
                        connections = ui.connections,
                        selectedProfile = ui.selectedProfile,
                        pingingKeys = ui.pingingKeys,
                        onPing = onPing,
                        onPingAll = onPingAll,
                        onBlock = onAddConnection,
                        onClear = onClearConnections,
                        onShowMessage = onShowMessage,
                    )
                    3 -> BlocklistScreen(
                        rules = ui.rules,
                        onAddRule = onAddRule,
                        onAddRules = onAddRules,
                        onAddPreset = onAddPreset,
                        onRemoveRule = onRemoveRule,
                        onSetRuleEnabled = onSetRuleEnabled,
                        onClearRules = onClearRules,
                        onShowMessage = onShowMessage,
                    )
                    else -> SettingsScreen(
                        ui = ui,
                        onDiscoveryChange = onDiscoveryChange,
                        onFastRejectChange = onFastRejectChange,
                        onMatchAlertsChange = onMatchAlertsChange,
                        onHideStartupAlertsChange = onHideStartupAlertsChange,
                        onAutoStartChange = onAutoStartChange,
                        onAllowStartupServerChange = onAllowStartupServerChange,
                        onShowMatchDemo = onShowMatchDemo,
                        onRefreshDiagnostics = onRefreshDiagnostics,
                        onDownloadGeoIp = onDownloadGeoIp,
                        onExportBackup = onExportBackup,
                        onImportBackup = onImportBackup,
                        onApplyImport = onApplyImport,
                        onCancelImport = onCancelImport,
                        onClearLogs = onClearLogs,
                        onRefreshRules = onRefreshRules,
                        onUnknownPortAlertsChange = onUnknownPortAlertsChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavigationItem(
    index: Int,
    selected: Int,
    label: String,
    icon: ImageVector,
    onClick: (Int) -> Unit,
) {
    NavigationBarItem(
        selected = selected == index,
        onClick = { onClick(index) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
    )
}

@Composable
private fun HomeScreen(
    ui: UiState,
    onToggle: () -> Unit,
    onDiscoveryChange: (Boolean) -> Unit,
    onSelectProfile: (String) -> Unit,
    onFinishOnboarding: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenAbout: () -> Unit,
    onHealthAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.size(14.dp))
            Column {
                Text(
                    "MLBB Blocker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.main_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ui.updateInfo?.let { update ->
            UpdateBanner(
                update = update,
                download = ui.updateDownload,
                installing = ui.updateInstalling,
                onDownload = onDownloadUpdate,
                onInstall = onInstallUpdate,
            )
        }

        ui.healthIssues.firstOrNull()?.let { issue ->
            HealthBanner(issue = issue, onFix = onHealthAction)
        }

        if (ui.showOnboarding) {
            OnboardingCard(onDone = onFinishOnboarding)
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor(ui.status)),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        statusText(ui.status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.rules_count, ui.effectiveRuleCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                ToggleRow(
                    title = stringResource(R.string.only_mlbb),
                    description = stringResource(R.string.only_mlbb_desc),
                    checked = true,
                    enabled = false,
                    onCheckedChange = {},
                )
                ToggleRow(
                    title = stringResource(R.string.discovery_mode),
                    description = stringResource(R.string.discovery_mode_desc),
                    checked = ui.discovery,
                    enabled = true,
                    onCheckedChange = onDiscoveryChange,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Профиль блокировки",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Выберите один профиль до поиска матча. «Москва» и «Новосибирск» разрешают только выбранный регион и блокируют остальные матч-серверы.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RegionProfiles.all.forEach { profile ->
                ProfileCard(
                    profile = profile,
                    selected = ui.selectedProfile == profile.id,
                    onClick = { onSelectProfile(profile.id) },
                )
            }
        }

        if (!ui.mlbbInstalled) {
            NoticeCard(
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.error,
                title = "MLBB не найдена",
                text = stringResource(R.string.error_mlbb_missing),
            )
        }

        val error = ui.status as? BlockerStatus.Error
        if (error != null) {
            NoticeCard(
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.error,
                title = "Ошибка",
                text = error.message,
            )
        }

        if (ui.discovery) {
            NoticeCard(
                icon = Icons.Default.Info,
                color = MaterialTheme.colorScheme.secondary,
                title = "Обнаружение включено",
                text = "Правила не применяются. Зайдите в игру, затем откройте вкладку «Монитор» и заблокируйте ненужные IP.",
            )
        } else if (ui.selectedProfile == null) {
            NoticeCard(
                icon = Icons.Default.Warning,
                color = Color(0xFFFFC857),
                title = "Профиль не выбран",
                text = "Выберите профиль блокировки: Москва, ДВ или Новосибирск.",
            )
        }

        if (ui.isRunning) {
            OutlinedButton(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Выключить блокировку", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = onToggle,
                enabled = !ui.isBusy && ui.mlbbInstalled,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (ui.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.size(10.dp))
                    Text("Запуск…")
                } else {
                    Text("Включить блокировку", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Правила", ui.effectiveRuleCount.toString(), Modifier.weight(1f))
            StatCard("Собрано IP", ui.connections.size.toString(), Modifier.weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Как это работает",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Приложение перехватывает трафик только Mobile Legends. Пакеты к IP из блоклиста отбрасываются, остальной трафик игры идёт напрямую. Удалённого VPN-сервера нет.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Пока блокировка включена, приложение собирает все IP, к которым подключается MLBB, и позволяет проверить пинг до каждого сервера.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Перед включением полностью закройте MLBB, чтобы игра заново проверила серверы.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Блокировка не гарантирует смену региона: MLBB может выбрать сервер по аккаунту или матчмейкингу.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedButton(
            onClick = onOpenAbout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("О приложении и возможностях")
        }
    }
}

@Composable
private fun HealthBanner(issue: HealthIssue, onFix: () -> Unit) {
    val title = when (issue) {
        HealthIssue.SERVICE_STOPPED -> stringResource(R.string.health_service_stopped_title)
        HealthIssue.GEOIP_FAILED -> stringResource(R.string.health_geoip_failed_title)
        HealthIssue.RULES_FALLBACK -> stringResource(R.string.health_rules_failed_title)
    }
    val text = when (issue) {
        HealthIssue.SERVICE_STOPPED -> stringResource(R.string.health_service_stopped_text)
        HealthIssue.GEOIP_FAILED -> stringResource(R.string.health_geoip_failed_text)
        HealthIssue.RULES_FALLBACK -> stringResource(R.string.health_rules_failed_text)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onFix) {
                Text(stringResource(R.string.health_fix))
            }
        }
    }
}

@Composable
private fun OnboardingCard(onDone: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Быстрый старт",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "1. Выдайте разрешение VPN при первом включении.\n" +
                    "2. Разрешите уведомления и показ поверх игры.\n" +
                    "3. Выберите профиль и включите блокировку.\n" +
                    "4. Запустите MLBB.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onDone) {
                Text("Понятно")
            }
        }
    }
}

@Composable
private fun UpdateBanner(
    update: UpdateInfo,
    download: UpdateDownloadState,
    installing: Boolean,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        stringResource(R.string.update_available_title, update.version),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.update_available_text, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    update.size?.let { bytes ->
                        Text(
                            "Размер APK: ${bytes / 1024 / 1024} МБ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            when (download) {
                UpdateDownloadState.Idle -> {
                    Button(onClick = onDownload) {
                        Text("Скачать и установить")
                    }
                }

                is UpdateDownloadState.Downloading -> {
                    Text(
                        "Загрузка: ${download.progress}% · ${download.received / 1024 / 1024} МБ",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                is UpdateDownloadState.Ready -> {
                    Button(onClick = onInstall, enabled = !installing) {
                        Text(if (installing) "Открываем установщик…" else "Установить")
                    }
                    Text(
                        "APK проверен по SHA256 и подписи",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                is UpdateDownloadState.Error -> {
                    Text(
                        "Не удалось скачать: ${download.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onDownload) {
                        Text("Повторить")
                    }
                }
            }

            TextButton(onClick = { openUrl(context, update.releaseUrl) }) {
                Text(stringResource(R.string.update_release_page))
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun profileAccent(profile: RegionProfile): Color = when (profile.id) {
    RegionProfiles.ID_MOSCOW -> Color(0xFF4C7DFF)
    RegionProfiles.ID_FAR_EAST -> Color(0xFF2FA36B)
    else -> Color(0xFFE08A2E)
}

@Composable
internal fun ProfileCard(
    profile: RegionProfile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = profileAccent(profile)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    profile.badge,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        profile.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (selected) {
                        Text(
                            "Активен",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                        )
                    }
                }
                Text(
                    profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun NoticeCard(
    icon: ImageVector,
    color: Color,
    title: String,
    text: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
