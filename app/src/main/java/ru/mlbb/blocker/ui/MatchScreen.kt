package ru.mlbb.blocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.UiState
import ru.mlbb.blocker.data.KnownServers
import ru.mlbb.blocker.data.RegionProfiles
import ru.mlbb.blocker.data.db.MatchSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchScreen(
    ui: UiState,
    onStartGame: (String) -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Перед матчем",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Проверьте готовность и откройте игру.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                RegionProfiles.byId(ui.selectedProfile ?: "")?.badge ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.CyanLight,
            )
        }

        EsportsCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CheckRow("MLBB", game != null, game?.versionName ?: if (game != null) "установлена" else "не найдена")
                CheckRow(
                    "Разрешение VPN",
                    ui.vpnPermissionGranted,
                    if (ui.vpnPermissionGranted) "есть" else "нет",
                )
                CheckRow("Блокировка", ui.isRunning, statusText(ui.status))
                CheckRow(
                    "Профиль",
                    ui.selectedProfile != null,
                    RegionProfiles.byId(ui.selectedProfile ?: "")?.badge ?: "не выбран",
                )
                CheckRow(
                    "Карточка матча",
                    ui.matchAlerts,
                    when {
                        !ui.matchAlerts -> "выкл"
                        ui.overlayPermissionGranted -> "overlay"
                        else -> "уведомление"
                    },
                )
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

        OutlinedButton(
            onClick = if (discoveryActive) onStopDiscovery else onStartDiscovery,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (discoveryActive) "Остановить сбор IP" else "Собрать серверы 60 секунд",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (ui.currentMatch != null) {
            val match = ui.currentMatch
            EsportsCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Текущий матч",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.CyanLight,
                    )
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

        if (ui.activeMatchId != null) {
            EsportsCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Что произошло?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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

        if (ui.blockedAttempts.isNotEmpty()) {
            EsportsCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Заблокировано", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ui.blockedAttempts.take(4).forEach { attempt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${attempt.ip}:${attempt.port}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { onAllow(attempt.ip, 5) }) {
                                Text("Разрешить", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        if (ui.allowRules.isNotEmpty()) {
            EsportsCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Исключения", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    ui.allowRules.forEach { rule ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                rule.value,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (rule.expiresAt > 0) {
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(rule.expiresAt))
                                } else {
                                    "постоянно"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { onRemoveAllow(rule.id) }) {
                                Text("Убрать", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        if (ui.matchSessions.isNotEmpty()) {
            Text(
                "Последние матчи",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.CyanLight,
                modifier = Modifier.padding(top = 4.dp),
            )
            ui.matchSessions.take(3).forEach { session ->
                MatchRow(session)
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, ok: Boolean, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (ok) "✓" else "✕",
            color = if (ok) AppColors.Success else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            detail,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultButton(label: String, onResult: (String) -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = { onResult(label) }, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MatchRow(session: MatchSessionEntity) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.result ?: "не завершён",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "профили: ${session.profiles.ifBlank { "—" }} · ${formatAge(session.startedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
