package ru.mlbb.blocker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.data.BlockPreset
import ru.mlbb.blocker.data.BlockRule
import ru.mlbb.blocker.data.Cidr
import ru.mlbb.blocker.data.Presets

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
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Блоклист",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${rules.size} правил · отбрасываются только для MLBB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить правило")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                PresetCard(onAddPreset = onAddPreset)
            }

            if (rules.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("Блоклист пуст", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Добавьте IP вручную, импортируйте список или используйте региональный пресет.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        onRemove = { onRemoveRule(rule.id) },
                        onEnabledChange = { onSetRuleEnabled(rule.id, it) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { showImport = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Импорт")
                    }
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(context, rules.joinToString("\n") { rule ->
                                if (rule.note.isBlank()) rule.value else "${rule.value} # ${rule.note}"
                            })
                            onShowMessage("Список скопирован в буфер обмена")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Экспорт")
                    }
                    OutlinedButton(
                        onClick = { showClear = true },
                        modifier = Modifier.weight(1f),
                        enabled = rules.isNotEmpty(),
                    ) {
                        Text("Очистить")
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddRuleDialog(
            onDismiss = { showAdd = false },
            onAdd = { value, note ->
                onAddRule(value, note)
                showAdd = false
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

@Composable
private fun PresetCard(onAddPreset: (BlockPreset) -> Unit) {
    EsportsCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Региональные пресеты",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Готовые наборы IP для Москвы, Новосибирска и ДВ. Их можно добавить в блоклист или настроить правила вручную. Остальные регионы видны в «Мониторе».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAddPreset(Presets.MOSCOW) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Москва")
                }
                Button(
                    onClick = { onAddPreset(Presets.NOVOSIBIRSK) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Новосибирск")
                }
            }
            OutlinedButton(
                onClick = { onAddPreset(Presets.NON_FAR_EAST) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Заблокировать всё, кроме ДВ")
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: BlockRule,
    onRemove: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    EsportsCard(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rule.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                )
                if (rule.note.isNotBlank()) {
                    Text(
                        rule.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = onEnabledChange,
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AddRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить правило") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalized = Cidr.normalize(value)
                    if (normalized == null) {
                        invalid = true
                    } else {
                        onAdd(normalized, note)
                        onDismiss()
                    }
                },
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
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
                    "Один IP/CIDR на строку. Строки с # или // игнорируются.",
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
                TextButton(
                    onClick = {
                        text = readClipboard(context).orEmpty()
                    },
                ) {
                    Text("Вставить из буфера")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(text) },
                enabled = text.isNotBlank(),
            ) {
                Text("Импортировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
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
