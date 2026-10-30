package ru.mlbb.blocker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.data.ConnectionStat
import ru.mlbb.blocker.data.KnownServer
import ru.mlbb.blocker.data.KnownServerKind
import ru.mlbb.blocker.data.KnownServers
import ru.mlbb.blocker.data.RegionProfiles

@OptIn(ExperimentalMaterial3Api::class)
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
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

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
            "novosibirsk" to "НСК",
            "other" to "Другие",
        ).forEach { (id, label) ->
            val count = connections.count { regionCategory(it) == id }
            if (count > 0) add(id to label)
        }
        connections.mapNotNull { it.country?.takeIf { value -> value.isNotBlank() } }
            .distinct()
            .take(3)
            .forEach { country -> add("geo:$country" to country) }
    }

    val searched = connections.filter { connection ->
        val value = query.trim()
        value.isEmpty() || listOf(connection.ip, connection.host, connection.city, connection.country)
            .any { it?.contains(value, ignoreCase = true) == true }
    }
    val sorted = when (sort) {
        "ping" -> searched.filter { matchesFilter(it, filter) }
            .sortedWith(compareBy(nullsLast()) { connection: ConnectionStat -> connection.pingMs })

        "region" -> searched.filter { matchesFilter(it, filter) }
            .sortedBy { (it.city ?: it.country ?: "").lowercase() }

        else -> searched.filter { matchesFilter(it, filter) }.sortedByDescending { it.lastSeen }
    }
    val preferred = if (profileCategory != null) {
        sorted.filter { regionCategory(it) == profileCategory }.ifEmpty { sorted }
    } else {
        sorted
    }
    val best = preferred.filter { it.pingMs != null }.minByOrNull { it.pingMs!! }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Монитор",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (connections.isEmpty()) {
                        "пока пусто"
                    } else {
                        "${connections.size} серверов" + (best?.pingMs?.let { " · лучший $it мс" } ?: "")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(
                onClick = onPingAll,
                enabled = connections.isNotEmpty() && pingingKeys.isEmpty(),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Измерить пинг у всех")
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Ещё")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Скопировать IP") },
                        enabled = connections.isNotEmpty(),
                        onClick = {
                            menuOpen = false
                            copyToClipboard(
                                context,
                                sorted.joinToString("\n") { it.displayAddress },
                            )
                            onShowMessage("IP скопированы в буфер обмена")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Очистить журнал") },
                        enabled = connections.isNotEmpty(),
                        onClick = {
                            menuOpen = false
                            onClear()
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = { Text("Поиск: IP, город, хост", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                TextButton(onClick = { sortMenu = true }) {
                    Text(
                        when (sort) {
                            "ping" -> "по пингу ▾"
                            "region" -> "по региону ▾"
                            else -> "новые ▾"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.CyanLight,
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        label = {
                            Text(
                                "$label $count",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AppColors.Surface,
                            labelColor = AppColors.TextMuted,
                            selectedContainerColor = AppColors.Blue.copy(alpha = 0.18f),
                            selectedLabelColor = AppColors.BlueLight,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filter == id,
                            borderColor = AppColors.Border,
                            selectedBorderColor = AppColors.Blue.copy(alpha = 0.55f),
                        ),
                    )
                }
            }
        }

        when {
            connections.isEmpty() -> EmptyMonitor()

            sorted.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (query.isNotBlank()) "Ничего не найдено" else "В этой категории пусто",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (best != null) {
                    item {
                        BestServerRow(
                            best = best,
                            pinging = best.key in pingingKeys,
                            onPing = { onPing(best) },
                        )
                    }
                }
                items(sorted, key = { it.key }) { connection ->
                    ConnectionRow(
                        connection = connection,
                        pingInProgress = connection.key in pingingKeys,
                        onClick = { selected = connection.key },
                    )
                }
            }
        }
    }

    val selectedConnection = connections.firstOrNull { it.key == selected }
    if (selectedConnection != null) {
        ConnectionSheet(
            connection = selectedConnection,
            pingInProgress = selectedConnection.key in pingingKeys,
            onDismiss = { selected = null },
            onPing = {
                onPing(selectedConnection)
            },
            onBlock = { subnet ->
                onBlock(selectedConnection, subnet)
                selected = null
            },
        )
    }
}

@Composable
private fun BestServerRow(best: ConnectionStat, pinging: Boolean, onPing: () -> Unit) {
    EsportsCard(
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Success.copy(alpha = 0.08f),
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            AppColors.Success.copy(alpha = 0.35f),
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Лучший",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.Success,
            )
            Text(
                "  ${best.city ?: best.ip} · ${best.displayAddress}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${best.pingMs} мс",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.Success,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onPing, enabled = !pinging) {
                Text(
                    if (pinging) "…" else "Пинг",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ConnectionRow(
    connection: ConnectionStat,
    pingInProgress: Boolean,
    onClick: () -> Unit,
) {
    val known = KnownServers.lookup(connection.ip)
    val city = connection.city ?: known?.city
    val location = listOfNotNull(city, connection.country)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" · ")
    EsportsCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    connection.displayAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    location.ifBlank { connection.host ?: connection.network.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (known != null) knownColor(known) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                pingLabel(connection, pingInProgress),
                style = MaterialTheme.typography.labelMedium,
                color = pingColor(connection),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionSheet(
    connection: ConnectionStat,
    pingInProgress: Boolean,
    onDismiss: () -> Unit,
    onPing: () -> Unit,
    onBlock: (Boolean) -> Unit,
) {
    val known = KnownServers.lookup(connection.ip)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                connection.displayAddress,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            val city = connection.city ?: known?.city
            Text(
                listOfNotNull(
                    city,
                    connection.country,
                    connection.host,
                    connection.network.uppercase(),
                ).filter { it.isNotBlank() }.distinct().joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (connection.pingMs != null || !connection.pingKind.isNullOrBlank()) {
                    Pill(
                        pingLabel(connection, pingInProgress),
                        pingColor(connection),
                    )
                }
                if (known?.latency != null) Pill("ориентир ${known.latency}", AppColors.TextMuted)
                Pill("замечен ${connection.observations}", AppColors.Cyan)
            }
            Text(
                "последняя активность ${formatAge(connection.lastSeen)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPing,
                    enabled = !pingInProgress,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (pingInProgress) "Пинг…" else "Пинг")
                }
                OutlinedButton(
                    onClick = { onBlock(false) },
                    modifier = Modifier.weight(1.6f),
                ) {
                    Text("Заблокировать IP")
                }
                OutlinedButton(
                    onClick = { onBlock(true) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (connection.ip.contains(':')) "/64" else "/24")
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Закрыть", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Blue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = AppColors.CyanLight,
                )
            }
            Text("Пока ничего не собрано", style = MaterialTheme.typography.titleSmall)
            Text(
                "Включите блокировку и запустите игру — IP появятся здесь.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        else -> "—"
    }
}

@Composable
private fun pingColor(connection: ConnectionStat): androidx.compose.ui.graphics.Color {
    val ms = connection.pingMs ?: return MaterialTheme.colorScheme.onSurfaceVariant
    return when {
        ms < 50 -> AppColors.Success
        ms < 100 -> AppColors.Warning
        else -> AppColors.Danger
    }
}

private fun knownColor(known: KnownServer): androidx.compose.ui.graphics.Color {
    return when (known.kind) {
        KnownServerKind.FAR_EAST -> AppColors.Success
        KnownServerKind.MOSCOW -> AppColors.BlueLight
        KnownServerKind.OTHER_REGION -> AppColors.Gold
        KnownServerKind.SERVICE -> AppColors.CyanLight
        KnownServerKind.CDN -> AppColors.TextMuted
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("MLBB IP", text))
}
