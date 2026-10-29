package ru.mlbb.blocker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.BuildConfig
import ru.mlbb.blocker.R
import ru.mlbb.blocker.UiState
import ru.mlbb.blocker.data.geo.GeoIpStatus
import ru.mlbb.blocker.data.meta.MetaRepository
import ru.mlbb.blocker.data.rules.RulesSource

@Composable
fun SettingsScreen(
    ui: UiState,
    onDiscoveryChange: (Boolean) -> Unit,
    onFastRejectChange: (Boolean) -> Unit,
    onMatchAlertsChange: (Boolean) -> Unit,
    onHideStartupAlertsChange: (Boolean) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onAllowStartupServerChange: (Boolean) -> Unit,
    onShowMatchDemo: () -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onDownloadGeoIp: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onApplyImport: (Boolean) -> Unit,
    onCancelImport: () -> Unit,
    onClearLogs: () -> Unit,
    onRefreshRules: () -> Unit,
    onUnknownPortAlertsChange: (Boolean) -> Unit,
    onStableOnlyAlertsChange: (Boolean) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(setOf("blocking")) }
    var showApiDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun toggle(id: String) {
        expanded = if (id in expanded) expanded - id else expanded + id
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        ExpandableCard(
            title = "Блокировка",
            subtitle = "Обнаружение, отказ, автозапуск",
            expanded = "blocking" in expanded,
            onToggle = { toggle("blocking") },
        ) {
            CompactSwitchRow(
                title = stringResource(R.string.discovery_mode),
                checked = ui.discovery,
                onCheckedChange = onDiscoveryChange,
            )
            CompactSwitchRow(
                title = "Быстрый отказ (RST)",
                checked = ui.fastReject,
                onCheckedChange = onFastRejectChange,
            )
            CompactSwitchRow(
                title = "Автозапуск после перезагрузки",
                checked = ui.autoStart,
                onCheckedChange = onAutoStartChange,
            )
        }

        ExpandableCard(
            title = "Уведомления",
            subtitle = "Карточка матча и фильтры",
            expanded = "alerts" in expanded,
            onToggle = { toggle("alerts") },
        ) {
            CompactSwitchRow(
                title = "Карточка матча",
                checked = ui.matchAlerts,
                onCheckedChange = onMatchAlertsChange,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (ui.overlayPermissionGranted) {
                        "Поверх игры: разрешено"
                    } else {
                        "Поверх игры: нет доступа"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ui.overlayPermissionGranted) {
                        AppColors.Success
                    } else {
                        AppColors.Warning
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { openOverlaySettings(context) }) {
                    Text(
                        if (ui.overlayPermissionGranted) "Изменить" else "Разрешить",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                TextButton(onClick = onShowMatchDemo) {
                    Text("Тест", style = MaterialTheme.typography.labelSmall)
                }
            }
            CompactSwitchRow(
                title = "Скрывать стартовые подключения",
                checked = ui.hideStartupAlerts,
                onCheckedChange = onHideStartupAlertsChange,
            )
            CompactSwitchRow(
                title = stringResource(R.string.settings_stable_alerts_title),
                checked = ui.stableOnlyAlerts,
                onCheckedChange = onStableOnlyAlertsChange,
            )
            CompactSwitchRow(
                title = stringResource(R.string.settings_unknown_ports_title),
                checked = ui.unknownPortAlerts,
                onCheckedChange = onUnknownPortAlertsChange,
            )
        }

        ExpandableCard(
            title = "Серверы",
            subtitle = "Основной сервер и GeoIP",
            expanded = "servers" in expanded,
            onToggle = { toggle("servers") },
        ) {
            CompactSwitchRow(
                title = "Разрешать основной сервер .151",
                checked = ui.allowStartupServer,
                onCheckedChange = onAllowStartupServerChange,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "GeoIP: " + when (val status = ui.geoIpStatus) {
                        GeoIpStatus.NotDownloaded -> "не загружена"
                        GeoIpStatus.Preparing -> "подготовка"
                        is GeoIpStatus.Downloading -> "${status.progress}%"
                        is GeoIpStatus.Ready -> status.month
                        is GeoIpStatus.Error -> "ошибка"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ui.geoIpStatus is GeoIpStatus.Error) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = onDownloadGeoIp,
                    enabled = ui.geoIpStatus !is GeoIpStatus.Downloading &&
                        ui.geoIpStatus !is GeoIpStatus.Preparing,
                ) {
                    Text("Обновить", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        EsportsCard(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Правила: ${ui.rulesStatus.updatedAt}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        rulesSourceLabel(ui.rulesStatus.source),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (ui.rulesStatus.checking) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.padding(12.dp).heightIn(max = 18.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.Blue,
                    )
                } else {
                    IconButton(onClick = onRefreshRules) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Обновить правила",
                            tint = AppColors.CyanLight,
                        )
                    }
                }
            }
            ui.rulesStatus.lastError?.let { error ->
                Text(
                    error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        ExpandableCard(
            title = "Данные",
            subtitle = "Резервная копия",
            expanded = "data" in expanded,
            onToggle = { toggle("data") },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExportBackup,
                    enabled = !ui.backupBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Экспорт")
                }
                OutlinedButton(
                    onClick = onImportBackup,
                    enabled = !ui.backupBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Импорт")
                }
            }
            Text(
                "Профиль, блоклист, исключения и история в одном JSON.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ExpandableCard(
            title = "Диагностика",
            subtitle = "Проверка, журнал",
            expanded = "diagnostics" in expanded,
            onToggle = { toggle("diagnostics") },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Пользователь ${ui.currentUserId} · VPN ${if (ui.vpnPermissionGranted) "да" else "нет"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRefreshDiagnostics) {
                    Text("Проверить", style = MaterialTheme.typography.labelSmall)
                }
            }
            ui.mlbbPackages.forEach { info ->
                val found = info.installed && info.enabled
                Text(
                    "${info.packageName}: ${if (found) "найден" else "нет"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (found) AppColors.Success else MaterialTheme.colorScheme.error,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Журнал ядра",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClearLogs) {
                    Text("Очистить", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(
                    onClick = {
                        copyDiagnostics(context, ui)
                        Toast.makeText(context, "Диагностика скопирована", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text("Скопировать", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (ui.logs.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ui.logs.takeLast(30).forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        ExpandableCard(
            title = "Для разработчиков",
            subtitle = "Адрес API меты",
            expanded = "dev" in expanded,
            onToggle = { toggle("dev") },
        ) {
            Text(
                MetaRepository.apiBase(context),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = AppColors.CyanLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = { showApiDialog = true }) {
                Text("Изменить адрес API", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                "Версия ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    ui.pendingImport?.let { summary ->
        AlertDialog(
            onDismissRequest = onCancelImport,
            title = { Text("Восстановить данные?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Версия: ${summary.appVersion.ifBlank { "неизвестна" }}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Правила: ${summary.rules} · исключения: ${summary.allowRules}\n" +
                            "IP: ${summary.endpoints} · матчи: ${summary.matchSessions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { onApplyImport(true) }) {
                        Text("Объединить с текущими")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onApplyImport(false) }) {
                    Text("Заменить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelImport) { Text("Отмена") }
            },
        )
    }

    if (showApiDialog) {
        ApiBaseDialog(
            current = MetaRepository.apiBase(context),
            onDismiss = { showApiDialog = false },
            onSave = {
                MetaRepository.setApiBase(context, it)
                showApiDialog = false
            },
        )
    }
}

@Composable
private fun ApiBaseDialog(
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Адрес API меты") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("https://example.com") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private fun rulesSourceLabel(source: RulesSource): String = when (source) {
    RulesSource.BUILT_IN -> "встроенные · обновление не применялось"
    RulesSource.ASSET -> "из APK · проверка раз в сутки"
    RulesSource.CACHED -> "сохранённые · проверка раз в сутки"
    RulesSource.REMOTE -> "с GitHub · проверка раз в сутки"
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(
                context,
                "Не удалось открыть настройки разрешения",
                Toast.LENGTH_SHORT,
            ).show()
        }
}

private fun copyDiagnostics(context: Context, ui: UiState) {
    val text = buildString {
        appendLine("MLBB Blocker ${BuildConfig.VERSION_NAME}")
        appendLine("user=${ui.currentUserId}")
        appendLine("vpnPermission=${ui.vpnPermissionGranted}")
        appendLine("status=${ui.status}")
        appendLine("rules=${ui.effectiveRuleCount}")
        appendLine("profile=${ui.selectedProfile ?: "none"}")
        appendLine("fastReject=${ui.fastReject}")
        appendLine("matchAlerts=${ui.matchAlerts} overlay=${ui.overlayPermissionGranted}")
        ui.mlbbPackages.forEach { info ->
            appendLine(
                "${info.packageName}: installed=${info.installed} enabled=${info.enabled} " +
                    "version=${info.versionName} uid=${info.uid}",
            )
        }
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("MLBB Blocker diagnostics", text))
}
