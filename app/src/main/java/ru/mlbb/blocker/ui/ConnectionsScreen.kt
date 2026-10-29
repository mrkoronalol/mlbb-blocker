package ru.mlbb.blocker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.data.ConnectionStat
import ru.mlbb.blocker.data.KnownServer
import ru.mlbb.blocker.data.KnownServerKind
import ru.mlbb.blocker.data.KnownServers
import ru.mlbb.blocker.data.RegionProfiles

@Composable
fun ConnectionsScreen(
    connections: List<ConnectionStat>,
    selectedProfile: String?,
    pingingKeys: Set<String>,
    onPing: (ConnectionStat) -> Unit,
    onPingAll: () -> Unit,
    onBlock: (ConnectionStat, Boolean) -> Unit,
    onClear: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("recent") }
    var sortMenu by rememberSaveable { mutableStateOf(false) }

    val profileCategory = when (selectedProfile) {
        RegionProfiles.ID_MOSCOW -> "moscow"
        RegionProfiles.ID_FAR_EAST -> "far_east"
        RegionProfiles.ID_NOVOSIBIRSK -> "novosibirsk"
        else -> null
    }
    val filters = buildList {
        add("all" to "Все")
        listOf(
            "moscow" to "Москва",
            "far_east" to "ДВ",
            "novosibirsk" to "Новосибирск",
            "other" to "Другие",
        ).forEach { (id, label) ->
            val count = connections.count { regionCategory(it) == id }
            if (count > 0) add(id to label)
        }
        connections.mapNotNull { it.country?.takeIf { value -> value.isNotBlank() } }
            .distinct()
            .take(4)
            .forEach { country -> add("geo:$country" to country) }
    }

    val searched = connections.filter { connection ->
        val value = query.trim()
        value.isEmpty() || listOf(connection.ip, connection.host, connection.city, connection.country)
            .any { it?.contains(value, ignoreCase = true) == true }
    }
    val filtered = searched.filter { matchesFilter(it, filter) }
    val sorted = when (sort) {
        "ping" -> filtered.sortedWith(compareBy(nullsLast()) { connection: ConnectionStat -> connection.pingMs })
        "region" -> filtered.sortedBy { (it.city ?: it.country ?: "").lowercase() }
        else -> filtered.sortedByDescending { it.lastSeen }
    }
    val preferred = if (profileCategory != null) {
        sorted.filter { regionCategory(it) == profileCategory }.ifEmpty { sorted }
    } else {
        sorted
    }
    val best = preferred.filter { it.pingMs != null }.minByOrNull { it.pingMs!! }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Монитор серверов",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Собрано: ${connections.size}. Все регионы.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onPingAll,
                enabled = connections.isNotEmpty() && pingingKeys.isEmpty(),
            ) {
                Text("Пинг")
            }
            TextButton(onClick = onClear, enabled = connections.isNotEmpty()) {
                Text("Очистить")
            }
        }

        Text(
            "Здесь собираются все IP, к которым подключается MLBB, независимо от региона. Город берётся из GeoIP, а пинг можно измерить для любого сервера.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Поиск по IP, хосту или городу") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                TextButton(onClick = { sortMenu = true }) {
                    Text(
                        when (sort) {
                            "ping" -> "Сортировка: пинг"
                            "region" -> "Сортировка: регион"
                            else -> "Сортировка: новые"
                        },
                    )
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Сначала новые") },
                        onClick = { sort = "recent"; sortMenu = false },
                    )
                    DropdownMenuItem(
                        text = { Text("По пингу") },
                        onClick = { sort = "ping"; sortMenu = false },
                    )
                    DropdownMenuItem(
                        text = { Text("По региону") },
                        onClick = { sort = "region"; sortMenu = false },
                    )
                }
            }
            Text(
                "Показано: ${sorted.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (connections.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filters.forEach { (id, label) ->
                    val count = if (id == "all") {
                        connections.size
                    } else {
                        connections.count { matchesFilter(it, id) }
                    }
                    FilterChip(
                        selected = filter == id,
                        onClick = { filter = id },
                        label = { Text("$label $count") },
                    )
                }
            }
        }

        if (best != null) {
            EsportsCard(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Лучший по пингу",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${best.city ?: best.ip} · ${best.displayAddress} · ${best.pingMs} мс",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(
                        onClick = { onPing(best) },
                        enabled = best.key !in pingingKeys,
                    ) {
                        Text("Пинг")
                    }
                }
            }
        }

        when {
            connections.isEmpty() -> {
                EmptyMonitor()
            }

            sorted.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (query.isNotBlank()) "Ничего не найдено" else "В этой категории пока нет серверов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    copyToClipboard(
                                        context,
                                        sorted.joinToString("\n") { it.displayAddress },
                                    )
                                    onShowMessage("IP скопированы в буфер обмена")
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Экспорт IP")
                            }
                        }
                    }
                    items(sorted, key = { it.key }) { connection ->
                        ConnectionCard(
                            connection = connection,
                            pingInProgress = connection.key in pingingKeys,
                            onPing = { onPing(connection) },
                            onBlock = onBlock,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMonitor() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
            Text("Пока ничего не собрано", style = MaterialTheme.typography.titleMedium)
            Text(
                "Включите блокировку и запустите игру. Все IP будут сохраняться здесь автоматически.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    connection: ConnectionStat,
    pingInProgress: Boolean,
    onPing: () -> Unit,
    onBlock: (ConnectionStat, Boolean) -> Unit,
) {
    val known = KnownServers.lookup(connection.ip)
    EsportsCard(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        connection.displayAddress,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!connection.host.isNullOrBlank()) {
                        Text(
                            connection.host,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    connection.network.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            val city = connection.city ?: known?.city
            val location = listOfNotNull(city, connection.country)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" · ")
            if (location.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        location,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (known != null) knownColor(known) else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (known?.latency != null) {
                        Text(
                            "ориентир ${known.latency}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "замечен ${connection.observations} раз · ${formatAge(connection.lastSeen)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    pingLabel(connection, pingInProgress),
                    style = MaterialTheme.typography.labelLarge,
                    color = pingColor(connection),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPing,
                    enabled = !pingInProgress,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (pingInProgress) "Пинг…" else "Пинг")
                }
                OutlinedButton(
                    onClick = { onBlock(connection, false) },
                    modifier = Modifier.weight(1.6f),
                ) {
                    Text("Заблокировать IP")
                }
                OutlinedButton(
                    onClick = { onBlock(connection, true) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (connection.ip.contains(':')) "/64" else "/24")
                }
            }
        }
    }
}

private fun regionCategory(connection: ConnectionStat): String {
    val known = KnownServers.lookup(connection.ip)
    val city = connection.city ?: known?.city ?: ""
    return when {
        known?.kind == KnownServerKind.MOSCOW || city.contains("Москв", ignoreCase = true) -> "moscow"
        city.contains("Новосиб", ignoreCase = true) -> "novosibirsk"
        known?.kind == KnownServerKind.FAR_EAST ||
            city.contains("Хабаров", ignoreCase = true) ||
            city.contains("Владивост", ignoreCase = true) -> "far_east"
        else -> "other"
    }
}

private fun matchesFilter(connection: ConnectionStat, filter: String): Boolean {
    if (filter == "all") return true
    if (filter.startsWith("geo:")) {
        val country = filter.removePrefix("geo:")
        return connection.country == country || connection.city == country
    }
    return regionCategory(connection) == filter
}

private fun pingLabel(connection: ConnectionStat, inProgress: Boolean): String {
    if (inProgress) return "пинг…"
    val ms = connection.pingMs
    val jitter = connection.pingJitterMs
    val kind = connection.pingKind
    return when {
        ms != null -> {
            val jitterText = if (jitter != null && jitter > 0) " ±$jitter" else ""
            "$ms мс$jitterText ${kind ?: ""}".trim()
        }
        !kind.isNullOrBlank() -> kind
        else -> "пинг не проверен"
    }
}

@Composable
private fun pingColor(connection: ConnectionStat): Color {
    val ms = connection.pingMs ?: return MaterialTheme.colorScheme.onSurfaceVariant
    return when {
        ms < 50 -> Color(0xFF31D0AA)
        ms < 100 -> Color(0xFFFFC857)
        else -> Color(0xFFFF6B6B)
    }
}

private fun knownColor(known: KnownServer): Color {
    return when (known.kind) {
        KnownServerKind.FAR_EAST -> Color(0xFF31D0AA)
        KnownServerKind.MOSCOW -> Color(0xFF6EA8FF)
        KnownServerKind.OTHER_REGION -> Color(0xFFE0A458)
        KnownServerKind.SERVICE -> Color(0xFF7DD3FC)
        KnownServerKind.CDN -> Color(0xFFA9B7C4)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("MLBB IP", text))
}
