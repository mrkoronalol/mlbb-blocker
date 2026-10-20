package ru.mlbb.blocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.UiState
import ru.mlbb.blocker.data.db.MatchSessionEntity
import ru.mlbb.blocker.data.KnownServers
import ru.mlbb.blocker.data.RegionProfiles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchScreen(
    ui: UiState,
    onStartGame: (String) -> Unit,
    onSelectProfile: (String) -> Unit,
    onFastRejectChange: (Boolean) -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onMatchResult: (String) -> Unit,
    onAllow: (String, Int) -> Unit,
    onRemoveAllow: (String) -> Unit,
) {
    val game = ui.mlbbPackages.firstOrNull { it.installed && it.enabled }
    val discoveryActive = ui.discoveryActive || ui.discoveryUntil > System.currentTimeMillis()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Перед матчем",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Выберите профиль и включите блокировку до поиска матча. Во время матча настройки не меняйте.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Проверка", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                CheckRow("MLBB найдена", game != null, game?.let { "${it.packageName} · ${it.versionName}" } ?: "не найдена")
                CheckRow(
                    "Разрешение VPN",
                    ui.vpnPermissionGranted,
                    if (ui.vpnPermissionGranted) "выдано" else "не выдано",
                )
                CheckRow(
                    "Блокировка",
                    ui.isRunning,
                    statusText(ui.status),
                )
                CheckRow(
                    "Профиль",
                    ui.selectedProfile != null,
                    RegionProfiles.byId(ui.selectedProfile ?: "")?.title ?: "не выбран",
                )
                CheckRow("Быстрый отказ", ui.fastReject, if (ui.fastReject) "RST/ICMP" else "drop")
                if (game == null) {
                    Text(
                        "Если игра установлена в Dual Apps или рабочем профиле, приложение в основном профиле её не увидит.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Профиль", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                RegionProfiles.all.forEach { profile ->
                    ProfileCard(
                        profile = profile,
                        selected = ui.selectedProfile == profile.id,
                        onClick = { onSelectProfile(profile.id) },
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Быстрый отказ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "RST/ICMP вместо тихого drop",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = ui.fastReject, onCheckedChange = onFastRejectChange)
                }
            }
        }

        if (ui.currentMatch != null) {
            val match = ui.currentMatch
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Текущий матч", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        match.displayAddress,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val city = match.city ?: KnownServers.lookup(match.ip)?.city
                    Text(
                        listOfNotNull(
                            city,
                            match.pingMs?.let { ms -> "$ms мс" + (match.pingJitterMs?.let { " ±$it" } ?: "") },
                            "обновлено ${formatAge(match.lastSeen)}",
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (ui.blockedAttempts.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Заблокированные попытки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ui.blockedAttempts.take(6).forEach { attempt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${attempt.ip}:${attempt.port}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                val city = KnownServers.lookup(attempt.ip)?.city
                                Text(
                                    listOfNotNull(city, attempt.network.uppercase(), formatAge(attempt.timestamp))
                                        .joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            TextButton(onClick = { onAllow(attempt.ip, 5) }) {
                                Text("Разрешить 5 мин")
                            }
                        }
                    }
                }
            }
        }

        if (ui.allowRules.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Исключения", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ui.allowRules.forEach { rule ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.value, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (rule.expiresAt > 0) {
                                        "до ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(rule.expiresAt))}"
                                    } else {
                                        "постоянно"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { onRemoveAllow(rule.id) }) {
                                Text("Удалить")
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { game?.let { onStartGame(it.packageName) } },
            enabled = game != null && !ui.isBusy,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                if (ui.isRunning) "Открыть MLBB" else "Включить блокировку и открыть MLBB",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            "Перед нажатием полностью закройте MLBB, если она уже запущена.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = if (discoveryActive) onStopDiscovery else onStartDiscovery,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (discoveryActive) {
                    val end = if (ui.discoveryUntil > 0) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ui.discoveryUntil))
                    } else {
                        "до остановки"
                    }
                    "Остановить сбор (до $end)"
                } else {
                    "Сбор 60 секунд"
                },
            )
        }

        if (ui.activeMatchId != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Матч начат", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "После возврата в приложение отметьте, что произошло:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ResultButton("ДВ", onMatchResult, Modifier.weight(1f))
                        ResultButton("Москва", onMatchResult, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ResultButton("Поиск сорвался", onMatchResult, Modifier.weight(1f))
                        ResultButton("Не знаю", onMatchResult, Modifier.weight(1f))
                    }
                }
            }
        }

        if (ui.matchSessions.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Последние матчи", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ui.matchSessions.take(5).forEach { session ->
                        MatchRow(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, ok: Boolean, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (ok) "✓" else "✕",
            color = if (ok) Color(0xFF31D0AA) else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultButton(label: String, onResult: (String) -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = { onResult(label) }, modifier = modifier) {
        Text(label)
    }
}

@Composable
private fun MatchRow(session: MatchSessionEntity) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.result ?: "не завершён",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "профили: ${session.profiles.ifBlank { "—" }} · ${formatAge(session.startedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
