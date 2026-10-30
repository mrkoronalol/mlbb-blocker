package ru.mlbb.blocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.BuildConfig

private data class Release(val version: String, val bullets: List<String>)

private val RELEASES = listOf(
    Release(
        "0.18.0",
        listOf(
            "Монитор: компактные строки, действия в шторке по тапу",
            "«О приложении» разбито на аккордеоны, история версий свёрнута",
            "Меньше текста на экранах — описания спрятаны в раскрытия",
        ),
    ),
    Release(
        "0.17.0",
        listOf(
            "Компактная главная: профили МСК / ДВ / НСК в один ряд, описание — по (i)",
            "Блокировка включается кнопкой, видимой без прокрутки",
            "Блоклист: короткие строки и удаление свайпом влево",
            "Настройки разбиты на аккордеоны",
            "Мета: заглушка при недоступном API и крупные плитки героев",
        ),
    ),
    Release(
        "0.16.0",
        listOf(
            "Тёмный дизайн в стиле сайта MLBB Meta",
            "Вкладка «Мета» с тирами S–D и статистикой героев",
            "Данные меты кэшируются в Room и работают офлайн",
        ),
    ),
    Release(
        "0.15.0",
        listOf(
            "Полный пинг в карточке матча: среднее, джиттер, потери",
            "Кнопки «Заблокировать» и «Не показывать» в карточке",
            "Настройка «Только стабильные соединения»",
        ),
    ),
    Release(
        "0.14.0",
        listOf(
            "VPN поднимается сам после обновления APK",
            "Баннер «Защита выключена»",
            "Подписанные правила с GitHub и ежедневная проверка",
            "Уведомление о новом порте MLBB",
        ),
    ),
    Release(
        "0.13.x",
        listOf(
            "Исправлена GeoIP после минификации",
            "Добавлены матч-порты 10011–10013, 10063 и 5500–5599",
            "Переключатель основного сервера .151",
        ),
    ),
    Release(
        "0.12.0",
        listOf(
            "Подписанный release APK и автосборка",
            "Профили блокируют только матч-порты",
            "Монитор: поиск, сортировка, лучший пинг",
        ),
    ),
    Release(
        "0.10 – 0.11",
        listOf(
            "Карточка «Матч найден» поверх игры",
            "Монитор всех серверов MLBB",
            "Новая иконка MB и плитка Quick Settings",
        ),
    ),
    Release(
        "0.9.0",
        listOf(
            "GeoIP внутри APK",
            "Профили «только Москва» и «только Новосибирск»",
            "Экран «О приложении»",
        ),
    ),
)

@Composable
fun AboutScreen() {
    var expanded by rememberSaveable { mutableStateOf(setOf("features", "latest")) }

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
        HeroCard()

        ExpandableCard(
            title = "Возможности",
            subtitle = "VPN, профили, монитор, мета",
            expanded = "features" in expanded,
            onToggle = { toggle("features") },
        ) {
            FeatureRow("Локальный VPN-фильтр только для Mobile Legends")
            FeatureRow("Профили Москва, ДВ и Новосибирск с режимом «только этот регион»")
            FeatureRow("Блоклист IP/CIDR и региональные пресеты")
            FeatureRow("Монитор серверов: фильтры по регионам, пинг и джиттер")
            FeatureRow("Мета с сайта MLBB Meta, кэш для офлайна")
            FeatureRow("Карточка матча поверх игры и «Разрешить 5 мин»")
            FeatureRow("GeoIP офлайн и сбор серверов за 60 секунд")
        }

        ExpandableCard(
            title = "Как это работает",
            subtitle = "4 шага",
            expanded = "how" in expanded,
            onToggle = { toggle("how") },
        ) {
            StepRow("1", "VPN перехватывает только трафик MLBB.")
            StepRow("2", "Разрешённые IP идут напрямую, остальные — в локальный sink.")
            StepRow("3", "Профили «только Москва/Новосибирск» блокируют другие матч-серверы.")
            StepRow("4", "Логин, магазин и служебные подключения не блокируются.")
        }

        ExpandableCard(
            title = "Что нового в ${BuildConfig.VERSION_NAME}",
            subtitle = "Коротко о последнем обновлении",
            expanded = "latest" in expanded,
            onToggle = { toggle("latest") },
        ) {
            val latest = RELEASES.first()
            latest.bullets.forEach { BulletRow(it) }
        }

        ExpandableCard(
            title = "История версий",
            subtitle = "0.9 — 0.17",
            expanded = "history" in expanded,
            onToggle = { toggle("history") },
        ) {
            RELEASES.drop(1).forEach { release ->
                Text(
                    release.version,
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.CyanLight,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                release.bullets.forEach { BulletRow(it) }
            }
        }

        ExpandableCard(
            title = "Важно знать",
            subtitle = "Ограничения",
            expanded = "limits" in expanded,
            onToggle = { toggle("limits") },
        ) {
            NoticeRow("Приложение не меняет регион аккаунта и не обходит ограничения издателя.")
            NoticeRow("Если все серверы выбранного региона заняты, матч может не найтись.")
            NoticeRow("Перед включением полностью закройте MLBB.")
            NoticeRow("GeoIP содержит только IPv4 — для IPv6 город может не определиться.")
            NoticeRow("Блокировка по IP не гарантирует выбор конкретного сервера.")
        }

        ExpandableCard(
            title = "Приватность",
            subtitle = "Только локально",
            expanded = "privacy" in expanded,
            onToggle = { toggle("privacy") },
        ) {
            Text(
                "Нет аналитики, аккаунтов и облачной синхронизации. Правила, история и измерения хранятся на устройстве. Трафик не уходит на сторонние VPN-серверы.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ExpandableCard(
            title = "Технологии",
            subtitle = "Kotlin, Compose, sing-box",
            expanded = "tech" in expanded,
            onToggle = { toggle("tech") },
        ) {
            Text(
                "Kotlin, Jetpack Compose, sing-box/libbox, Room, Coil, MaxMind DB Reader. Геоданные: DB-IP Lite City, CC BY 4.0 (db-ip.com), sapics/ip-location-db.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "MLBB Blocker ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · для личного использования",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun HeroCard() {
    EsportsCard(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(
                            "MLBB Blocker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Локальный фильтр серверов Mobile Legends",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("v${BuildConfig.VERSION_NAME}", AppColors.CyanLight)
                    Pill("локально", AppColors.Success)
                    Pill("без аккаунтов", AppColors.TextMuted)
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = AppColors.Success,
            modifier = Modifier.padding(top = 2.dp).size(15.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BulletRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "•",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Cyan,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepRow(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AppColors.Blue.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.BlueLight,
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NoticeRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = AppColors.Warning,
            modifier = Modifier.padding(top = 2.dp).size(15.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
