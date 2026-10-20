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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
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
import ru.mlbb.blocker.UiState
import ru.mlbb.blocker.data.geo.GeoIpStatus
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

@Composable
fun SettingsScreen(
    ui: UiState,
    onDiscoveryChange: (Boolean) -> Unit,
    onFastRejectChange: (Boolean) -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onDownloadGeoIp: () -> Unit,
    onClearLogs: () -> Unit,
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
                "Мониторинг и пинг",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Пока блокировка включена, приложение собирает все IP подключений MLBB и сохраняет их между запусками. Во вкладке «Мониторинг» можно измерить TCP-пинг до сервера и сразу заблокировать IP или подсеть.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsCard {
            Text(
                "Из вашего PCAP",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Хабаровск: 92.223.80.21 — 26 мс, 92.223.80.65 — 38 мс\n" +
                    "Новосибирск: 146.185.199.88 — 84 мс\n" +
                    "Екатеринбург: 92.223.91.7 — 116 мс\n" +
                    "Москва: 154.85.91.153 — 126 мс (матч-сервер)\n" +
                    "Ростов-на-Дону: 146.185.197.81 — 148 мс",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "В PCAP игра установила матч-соединение с Москвой, хотя Хабаровск отвечал за 26 мс. Пресеты блокировки находятся во вкладке «Блоклист».",
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

private fun copyDiagnostics(context: Context, ui: UiState) {
    val text = buildString {
        appendLine("MLBB Blocker ${BuildConfig.VERSION_NAME}")
        appendLine("user=${ui.currentUserId}")
        appendLine("vpnPermission=${ui.vpnPermissionGranted}")
        appendLine("status=${ui.status}")
        appendLine("rules=${ui.effectiveRuleCount}")
        appendLine("profile=${ui.selectedProfile ?: "none"}")
        appendLine("fastReject=${ui.fastReject}")
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
