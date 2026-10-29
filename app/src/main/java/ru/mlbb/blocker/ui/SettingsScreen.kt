package ru.mlbb.blocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.BuildConfig
import ru.mlbb.blocker.R
import ru.mlbb.blocker.UiState
import ru.mlbb.blocker.data.geo.GeoIpStatus
import ru.mlbb.blocker.data.rules.RulesSource
import androidx.compose.ui.res.stringResource
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

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
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Режим обнаружения", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Не блокировать правила, а только записывать подключения MLBB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = ui.discovery, onCheckedChange = onDiscoveryChange)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Text(
                "Перехватывается только com.mobile.legends. Остальной трафик телефона идёт напрямую.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Правила с сервера", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.rules_settings_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                stringResource(
                    R.string.rules_status_line,
                    rulesSourceLabel(ui.rulesStatus.source),
                    ui.rulesStatus.updatedAt,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ui.rulesStatus.lastError?.let { error ->
                Text(
                    "Ошибка: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onRefreshRules,
                enabled = !ui.rulesStatus.checking,
            ) {
                Text(
                    if (ui.rulesStatus.checking) {
                        stringResource(R.string.rules_checking)
                    } else {
                        stringResource(R.string.rules_check_now)
                    },
                )
            }
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Быстрый отказ (RST)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Вместо тихого drop отправлять RST/ICMP — игра быстрее понимает, что сервер недоступен.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = ui.fastReject, onCheckedChange = onFastRejectChange)
            }
            Text(
                "Перехватываются com.mobile.legends и com.mobilelegends.mi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val context = LocalContext.current
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Карточка матча", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Показывать сервер и пинг поверх игры, когда MLBB подключается к матч-серверу.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = ui.matchAlerts, onCheckedChange = onMatchAlertsChange)
            }
            Text(
                if (ui.overlayPermissionGranted) {
                    "Поверх игры: разрешено"
                } else {
                    "Поверх игры: не разрешено — покажем обычное уведомление."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (ui.overlayPermissionGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ui.overlayPermissionGranted) {
                    TextButton(onClick = { openOverlaySettings(context) }) {
                        Text("Изменить разрешение")
                    }
                } else {
                    Button(onClick = { openOverlaySettings(context) }) {
                        Text("Разрешить показ поверх игры")
                    }
                }
                TextButton(onClick = onShowMatchDemo) {
                    Text("Проверить карточку")
                }
            }
            Text(
                "На Xiaomi/Redmi может понадобиться разрешение «Показывать всплывающие окна в фоне». Без overlay-разрешения показывается heads-up уведомление.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Скрывать стартовые подключения", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Не показывать карточку для разрешённых подключений, если они закрылись за 2,5 секунды. Заблокированные серверы показываются всегда.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = ui.hideStartupAlerts,
                    onCheckedChange = onHideStartupAlertsChange,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Автозапуск после перезагрузки", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Запускать VPN автоматически, если блокировка была включена до перезагрузки.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = ui.autoStart, onCheckedChange = onAutoStartChange)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Разрешать основной сервер .151", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "154.85.91.151:10003 будет идти напрямую и не показывать карточку. Выключите, чтобы блокировать его как московский.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = ui.allowStartupServer,
                    onCheckedChange = onAllowStartupServerChange,
                )
            }
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_unknown_ports_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.settings_unknown_ports_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = ui.unknownPortAlerts,
                    onCheckedChange = onUnknownPortAlertsChange,
                )
            }
        }

        DiagnosticsCard(ui = ui, onRefresh = onRefreshDiagnostics)

        SettingsCard {
            Text(
                "GeoIP база",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            val geoStatus = ui.geoIpStatus
            Text(
                when (geoStatus) {
                    GeoIpStatus.NotDownloaded -> "не загружена"
                    GeoIpStatus.Preparing -> "подготовка встроенной базы…"
                    is GeoIpStatus.Downloading -> "загрузка… ${geoStatus.progress}% (${geoStatus.received / 1024 / 1024} МБ)"
                    is GeoIpStatus.Ready -> "готова · ${geoStatus.month}"
                    is GeoIpStatus.Error -> "ошибка: ${geoStatus.message}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (geoStatus is GeoIpStatus.Error) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "База DB-IP Lite City (IPv4, ~59 МБ) уже встроена в APK и распаковывается при первом запуске. Кнопка обновляет её до свежей версии. Лицензия CC BY 4.0, источник: db-ip.com / sapics/ip-location-db.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onDownloadGeoIp,
                enabled = geoStatus !is GeoIpStatus.Downloading && geoStatus !is GeoIpStatus.Preparing,
            ) {
                Text(
                    when (geoStatus) {
                        is GeoIpStatus.Ready -> "Обновить базу"
                        GeoIpStatus.Preparing -> "Подготовка…"
                        else -> "Скачать базу"
                    },
                )
            }
        }

        SettingsCard {
            Text(
                "Резервная копия",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Профиль, блоклист, исключения, настройки, история IP, матчей и пинга. Файл JSON можно сохранить в Downloads или облако.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            if (ui.backupBusy) {
                Text(
                    "Работа с файлом…",
                    style = MaterialTheme.typography.bodySmall,
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
                                "IP: ${summary.endpoints} · блокировки: ${summary.blockedAttempts}\n" +
                                "Матчи: ${summary.matchSessions} · пинг: ${summary.pingSamples}",
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
                    TextButton(onClick = onCancelImport) {
                        Text("Отмена")
                    }
                },
            )
        }

        SettingsCard {
            Text(
                "Монитор и пинг",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Пока блокировка включена, приложение собирает все IP подключений MLBB и сохраняет их между запусками. Во вкладке «Монитор» можно фильтровать серверы по регионам, измерить TCP-пинг и сразу заблокировать IP или подсеть.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Журнал ядра",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClearLogs) {
                    Text("Очистить")
                }
            }
            if (ui.logs.isEmpty()) {
                Text(
                    "Журнал пуст",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ui.logs.takeLast(40).forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        SettingsCard {
            Text(
                "MLBB Blocker ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Локальный фильтр пакетов для Mobile Legends. Удалённого VPN-сервера нет, трафик не перенаправляется в другой регион. Блокировка IP не гарантирует смену игрового региона.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Использование может нарушать правила Moonton. Вы действуете на свой риск.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticsCard(ui: UiState, onRefresh: () -> Unit) {
    val context = LocalContext.current
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Диагностика",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRefresh) {
                Text("Проверить")
            }
        }
        Text(
            "Пользователь: ${ui.currentUserId} · VPN: ${if (ui.vpnPermissionGranted) "разрешён" else "нет"} · Сервис: ${statusText(ui.status)} · Правил: ${ui.effectiveRuleCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ui.mlbbPackages.forEach { info ->
            val found = info.installed && info.enabled
            Text(
                "${info.packageName}: ${if (found) "найден" else "не найден"}" +
                    (info.versionName?.let { " · $it" } ?: "") +
                    (info.uid?.let { " · uid $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = if (found) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        if (!ui.mlbbInstalled) {
            Text(
                "Если игра установлена в Dual Apps или рабочем профиле, приложение в основном профиле её не увидит — Android не даёт обычному приложению доступ к пакетам другого профиля.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        TextButton(
            onClick = {
                copyDiagnostics(context, ui)
                Toast.makeText(context, "Диагностика скопирована", Toast.LENGTH_SHORT).show()
            },
        ) {
            Text("Скопировать диагностику")
        }
    }
}

private fun rulesSourceLabel(source: RulesSource): String = when (source) {
    RulesSource.BUILT_IN -> "встроенные"
    RulesSource.ASSET -> "из APK"
    RulesSource.CACHED -> "сохранённые"
    RulesSource.REMOTE -> "с GitHub"
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "Не удалось открыть настройки разрешения", Toast.LENGTH_SHORT).show()
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

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}
