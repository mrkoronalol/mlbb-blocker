package ru.mlbb.blocker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.data.BlockPreset
import ru.mlbb.blocker.data.BlockRule
import ru.mlbb.blocker.data.Cidr
import ru.mlbb.blocker.data.Presets
import ru.mlbb.blocker.data.RuleRegion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistScreen(
    rules: List<BlockRule>,
    onAddRule: (String, String) -> Boolean,
    onAddRules: (String) -> Int,
    onAddPreset: (BlockPreset) -> Unit,
    onRemoveRule: (String) -> Unit,
    onSetRuleEnabled: (String, Boolean) -> Unit,
    onClearRules: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedRule by remember { mutableStateOf<BlockRule?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Блоклист",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (rules.isEmpty()) "пусто" else "правил: ${rules.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить правило")
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Ещё")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Импорт списка") },
                        onClick = {
                            showMenu = false
                            showImport = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Скопировать список") },
                        onClick = {
                            showMenu = false
                            copyToClipboard(
                                context,
                                rules.joinToString("\n") { rule ->
                                    if (rule.note.isBlank()) rule.value else "${rule.value} # ${rule.note}"
                                },
                            )
                            onShowMessage("Список скопирован в буфер обмена")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Очистить блоклист") },
                        enabled = rules.isNotEmpty(),
                        onClick = {
                            showMenu = false
                            showClear = true
                        },
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (rules.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Lock,
                        title = "Блоклист пуст",
                        text = "Добавьте IP вручную или выберите готовый региональный пресет.",
                        actionLabel = "Добавить правило",
                        onAction = { showAdd = true },
                    )
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    SwipeableRuleRow(
                        rule = rule,
                        onClick = { selectedRule = rule },
                        onEnabledChange = { onSetRuleEnabled(rule.id, it) },
                        onRemove = {
                            onRemoveRule(rule.id)
                            onShowMessage("Правило ${rule.value} удалено")
                        },
                    )
                }
                item {
                    Text(
                        "Свайп влево — удалить",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, start = 2.dp),
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddRuleSheet(
            onDismiss = { showAdd = false },
            onAdd = { value, note ->
                onAddRule(value, note)
                showAdd = false
            },
            onAddPreset = {
                onAddPreset(it)
                showAdd = false
            },
        )
    }

    selectedRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { selectedRule = null },
            title = { Text(rule.value, fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        rule.note.ifBlank { "Без комментария" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (rule.enabled) "Правило включено" else "Правило выключено",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRule = null }) { Text("Закрыть") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onRemoveRule(rule.id)
                        selectedRule = null
                        onShowMessage("Правило ${rule.value} удалено")
                    },
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    if (showImport) {
        ImportDialog(
            context = context,
            onDismiss = { showImport = false },
            onImport = { text ->
                onAddRules(text)
                showImport = false
            },
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Очистить блоклист?") },
            text = { Text("Все правила будут удалены. Текущее подключение перезагрузится.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearRules()
                        showClear = false
                    },
                ) {
                    Text("Очистить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) {
                    Text("Отмена")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRuleRow(
    rule: BlockRule,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                        RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        EsportsCard(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    rule.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                RuleRegion.of(rule.value, rule.note)?.let { badge ->
                    RegionBadge(badge.label, badgeColor(badge.kind))
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.scale(0.8f),
                )
            }
        }
    }
}

private fun badgeColor(kind: RuleRegion.Kind) = when (kind) {
    RuleRegion.Kind.MOSCOW -> AppColors.Blue
    RuleRegion.Kind.FAR_EAST -> AppColors.Success
    RuleRegion.Kind.NOVOSIBIRSK -> AppColors.Gold
    RuleRegion.Kind.SERVICE -> AppColors.Cyan
    RuleRegion.Kind.OTHER -> AppColors.TextMuted
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleSheet(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onAddPreset: (BlockPreset) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Новое правило", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    invalid = false
                },
                label = { Text("IP или CIDR") },
                placeholder = { Text("154.85.91.0/24") },
                singleLine = true,
                isError = invalid,
                modifier = Modifier.fillMaxWidth(),
            )
            if (invalid) {
                Text(
                    "Некорректный IP/CIDR",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Комментарий (необязательно)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val normalized = Cidr.normalize(value)
                    if (normalized == null) {
                        invalid = true
                    } else {
                        onAdd(normalized, note)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("Добавить")
            }

            Text(
                "Готовые пресеты",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.CyanLight,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onAddPreset(Presets.MOSCOW) },
                    modifier = Modifier.weight(1f),
                ) { Text("МСК") }
                OutlinedButton(
                    onClick = { onAddPreset(Presets.NOVOSIBIRSK) },
                    modifier = Modifier.weight(1f),
                ) { Text("НСК") }
                OutlinedButton(
                    onClick = { onAddPreset(Presets.NON_FAR_EAST) },
                    modifier = Modifier.weight(1f),
                ) { Text("Не ДВ") }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ImportDialog(
    context: Context,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Импорт блоклиста") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Один IP/CIDR на строку.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Список") },
                    placeholder = { Text("154.85.91.0/24\n66.253.15.83") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { text = readClipboard(context).orEmpty() }) {
                    Text("Вставить из буфера")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(text) }, enabled = text.isNotBlank()) {
                Text("Импортировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("MLBB Blocker", text))
}

private fun readClipboard(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context).toString()
}
