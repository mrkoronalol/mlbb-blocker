package ru.mlbb.blocker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ru.mlbb.blocker.MetaUiState
import ru.mlbb.blocker.MetaViewModel
import ru.mlbb.blocker.data.meta.MetaHero
import ru.mlbb.blocker.data.meta.MetaLane
import ru.mlbb.blocker.data.meta.MetaRepository
import ru.mlbb.blocker.data.meta.MetaTier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MetaScreen(viewModel: MetaViewModel = viewModel()) {
    val state by viewModel.ui.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MetaHeader(
                state = state,
                onRefresh = viewModel::refresh,
            )
        }
        item {
            RegionSelector(
                selected = state.region,
                onSelect = viewModel::selectRegion,
            )
        }

        val snapshot = state.snapshot
        if (snapshot != null && state.error != null) {
            item {
                EsportsCard(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = AppColors.Warning.copy(alpha = 0.10f),
                    ),
                    border = BorderStroke(1.dp, AppColors.Warning.copy(alpha = 0.35f)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = AppColors.Warning,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Нет связи с сайтом",
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColors.Warning,
                            )
                            Text(
                                "Показаны сохранённые данные. ${state.error}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (state.loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AppColors.Blue)
                }
            }
        } else if (snapshot == null) {
            item {
                EmptyStateCard(
                    icon = null,
                    title = "Нет связи с MLBB Meta",
                    text = state.error ?: "Проверьте подключение к интернету и повторите.",
                    actionLabel = "Повторить",
                    onAction = viewModel::refresh,
                )
            }
        } else {
            if (snapshot.regional) {
                item {
                    Text(
                        "Регион: ${MetaRepository.regionTitle(snapshot.region)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (state.region != MetaRepository.REGION_WORLD) {
                item {
                    Text(
                        "Сайт пока отдаёт общую мету для всех регионов — региональная статистика появится позже.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val grouped = snapshot.heroes.groupBy { it.tier }
            items(MetaTier.entries.toList(), key = { it.key }) { tier ->
                val heroes = grouped[tier].orEmpty()
                if (heroes.isNotEmpty()) {
                    TierSection(
                        tier = tier,
                        heroes = heroes,
                        expanded = tier in state.expandedTiers,
                        bans = state.bans,
                        onToggle = { viewModel.toggleTier(tier) },
                        onHeroClick = viewModel::openHero,
                    )
                }
            }

            item {
                Text(
                    "Данные: ${snapshot.updatedLabel}" +
                        (snapshot.updatedAt?.let { " · обновлено ${it.take(10)}" } ?: "") +
                        " · сохранено локально",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
            }
        }
    }

    val hero = state.selectedHero
    if (hero != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeHero,
            containerColor = AppColors.SurfaceHigh,
        ) {
            HeroDetailsSheet(
                hero = hero,
                state = state,
                onBanCounters = viewModel::banCounters,
                onToggleBan = viewModel::toggleBan,
            )
        }
    }
}

@Composable
private fun MetaHeader(
    state: MetaUiState,
    onRefresh: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "Мета",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                state.snapshot?.updatedLabel?.let { "Тир-лист · $it" } ?: "Тир-листы MLBB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.refreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = AppColors.Blue,
            )
            Spacer(Modifier.width(10.dp))
        }
        IconButton(onClick = onRefresh, enabled = !state.refreshing) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Обновить мету",
                tint = AppColors.CyanLight,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionSelector(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetaRepository.REGIONS.forEach { (id, title) ->
            val active = id == selected
            FilterChip(
                selected = active,
                onClick = { onSelect(id) },
                label = {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
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
                    selected = active,
                    borderColor = AppColors.Border,
                    selectedBorderColor = AppColors.Blue.copy(alpha = 0.55f),
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TierSection(
    tier: MetaTier,
    heroes: List<MetaHero>,
    expanded: Boolean,
    bans: Set<String>,
    onToggle: () -> Unit,
    onHeroClick: (MetaHero) -> Unit,
) {
    val accent = tierColor(tier)
    EsportsCard(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = AppColors.Surface,
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tier.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Тир ${tier.title}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${heroes.size} героев",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
                Text(
                    if (expanded) "−" else "+",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
        }

        if (expanded) {
            HorizontalDivider(color = accent.copy(alpha = 0.16f))
            val byLane = heroes.groupBy { it.displayLanes.firstOrNull() ?: MetaLane.EXP }
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MetaLane.entries.forEach { lane ->
                    val laneHeroes = byLane[lane].orEmpty()
                    if (laneHeroes.isEmpty()) return@forEach
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            lane.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent.copy(alpha = 0.85f),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            laneHeroes
                                .sortedBy { it.sortOrder }
                                .forEach { hero ->
                                    HeroTile(
                                        hero = hero,
                                        banned = hero.id in bans,
                                        onClick = { onHeroClick(hero) },
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroTile(hero: MetaHero, banned: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(78.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            AsyncImage(
                model = hero.imageUrl,
                contentDescription = hero.displayName,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = 1.dp,
                        color = if (banned) AppColors.Danger.copy(alpha = 0.7f) else AppColors.Border,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .background(AppColors.SurfaceHigher),
                placeholder = ColorPainter(AppColors.SurfaceHigher),
                error = ColorPainter(AppColors.SurfaceHigher),
            )
            if (banned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.Danger)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        "BAN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2A0509),
                    )
                }
            }
        }
        Text(
            hero.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroDetailsSheet(
    hero: MetaHero,
    state: MetaUiState,
    onBanCounters: () -> Unit,
    onToggleBan: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AsyncImage(
                model = hero.imageUrl,
                contentDescription = hero.displayName,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, tierColor(hero.tier).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .background(AppColors.Surface),
                placeholder = ColorPainter(AppColors.Surface),
                error = ColorPainter(AppColors.Surface),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    hero.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(hero.tier.title, tierColor(hero.tier))
                    if (hero.role.isNotBlank()) Pill(hero.role, AppColors.Cyan)
                }
                Text(
                    hero.displayLanes.joinToString(" · ") { it.title },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(color = AppColors.Border.copy(alpha = 0.5f))

        if (hero.hasStats) {
            StatRow("Winrate", hero.winRate, AppColors.Success)
            StatRow("Pickrate", hero.pickRate, AppColors.Cyan)
            StatRow("Banrate", hero.banRate, AppColors.Danger)
        } else {
            Text(
                "Сайт пока не отдаёт статистику по этому герою.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = AppColors.Border.copy(alpha = 0.5f))

        Text(
            "Контрпики",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        when {
            state.countersLoading -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = AppColors.Cyan,
            )

            state.countersError != null -> Text(
                state.countersError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.counters != null && state.counters.ids.isEmpty() -> Text(
                "Пока нет данных о контрпиках.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.counters != null -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.counters.names.forEach { name ->
                        Pill(name, AppColors.Danger)
                    }
                }
            }
        }

        Button(
            onClick = onBanCounters,
            enabled = state.counters?.ids?.isNotEmpty() == true,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Заблокировать контрпики")
        }

        TextButton(
            onClick = { onToggleBan(hero.id) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (hero.id in state.bans) "Убрать из бан-листа" else "Добавить героя в бан-лист",
                color = if (hero.id in state.bans) AppColors.Danger else AppColors.CyanLight,
            )
        }

        state.sheetMessage?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Success,
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: Double?, color: Color) {
    val percent = (value ?: 0.0).let { if (it in 0.0..1.0) it * 100 else it }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (value == null) "—" else String.format("%.1f%%", percent),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
        LinearProgressIndicator(
            progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.14f),
        )
    }
}


private fun tierColor(tier: MetaTier): Color = when (tier) {
    MetaTier.S -> AppColors.Purple
    MetaTier.A -> AppColors.Blue
    MetaTier.B -> AppColors.Cyan
    MetaTier.C -> AppColors.Gold
    MetaTier.D -> AppColors.TextMuted
}
