package ru.mlbb.blocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mlbb.blocker.BuildConfig

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HeroCard()

        AboutCard("Что уже работает") {
            FeatureRow("Локальный VPN-фильтр только для Mobile Legends — без удалённого сервера")
            FeatureRow("Три профиля: Москва, ДВ и Новосибирск")
            FeatureRow("Режимы «только Москва» и «только Новосибирск» с блокировкой остальных матч-серверов")
            FeatureRow("Блоклист IP/CIDR и ручное управление правилами")
            FeatureRow("Монитор всех серверов MLBB с фильтрами по регионам")
            FeatureRow("TCP-пинг, джиттер, история матчей и заблокированных попыток")
            FeatureRow("Карточка «Матч найден» поверх игры и кнопка «Разрешить 5 мин»")
            FeatureRow("Офлайн GeoIP: встроенная база DB-IP Lite City (IPv4)")
            FeatureRow("Сбор серверов за 60 секунд и экспорт списка IP")
        }

        AboutCard("Что нового в 0.13.0") {
            BulletRow("Экспорт и импорт резервной копии через системный выбор файла")
            BulletRow("Перед обновлением автоматически создаётся локальная копия")
            BulletRow("Загрузка APK в приложении с проверкой SHA256 и подписи")
            BulletRow("Установка обновления через системный PackageInstaller")
        }

        AboutCard("Что нового в 0.12.0") {
            BulletRow("Подписанный release APK и автосборка GitHub Actions")
            BulletRow("Умное скрытие стартовых подключений по времени жизни")
            BulletRow("Профили блокируют только матч-порты, сервисные порты открыты")
            BulletRow("Основной сервер 154.85.91.151:10003 всегда разрешён")
            BulletRow("Монитор: поиск, сортировка, фильтры по странам и лучший пинг")
            BulletRow("Пресеты блоклиста для Москвы, Новосибирска и ДВ")
            BulletRow("Светлая тема / Material You и автозапуск после перезагрузки")
        }

        AboutCard("Что нового в 0.11.4") {
            BulletRow("Первое появление карточки матча после включения VPN скрывается")
            BulletRow("Стартовое подключение игры больше не показывает окно")
            BulletRow("Порт 30190 по-прежнему идёт напрямую")
        }

        AboutCard("Что нового в 0.11.3") {
            BulletRow("Убрано стартовое 60-секундное окно")
            BulletRow("Порт 30190 с диагностикой серверов по-прежнему идёт напрямую")
            BulletRow("Профиль применяется сразу после включения блокировки")
        }

        AboutCard("Что нового в 0.11.1") {
            BulletRow("Новый логотип MB в мятном круге")
            BulletRow("Adaptive icon, круглая иконка и monochrome для Android 13")
            BulletRow("Обновлена иконка уведомления и плитки Quick Settings")
        }

        AboutCard("Что нового в 0.11.0") {
            BulletRow("Компактная карточка матча в стиле уведомления")
            BulletRow("Вкладка «Монитор» с фильтрами по всем регионам")
            BulletRow("Проверка обновлений и уведомление о новой версии через GitHub")
            BulletRow("Публичный репозиторий и APK на GitHub")
        }

        AboutCard("Что нового в 0.10.0") {
            BulletRow("Карточка «Матч найден» поверх игры: город, IP, профиль и пинг")
            BulletRow("Если показ поверх игры запрещён — heads-up уведомление")
            BulletRow("Кулдаун 30 секунд на сервер, автоскрытие через 10 секунд")
            BulletRow("Кнопка «Разрешить 5 мин» прямо из карточки")
        }

        AboutCard("Что нового в 0.9.0") {
            BulletRow("GeoIP-база теперь лежит внутри APK и готова сразу после установки")
            BulletRow("Новый экран «О приложении»")
            BulletRow("Профили «Москва» и «Новосибирск» переделаны в режим «только этот регион»")
            BulletRow("На главной появились крупные карточки профилей")
        }

        AboutCard("Как это работает") {
            StepRow("1", "VPN-сервис перехватывает только трафик com.mobile.legends и com.mobilelegends.mi.")
            StepRow("2", "Разрешённые IP идут напрямую. Нежелательные адреса уходят в локальный sink и не покидают телефон.")
            StepRow("3", "В профилях «только Москва» и «только Новосибирск» разрешён выбранный регион, а остальные матч-порты блокируются.")
            StepRow("4", "Логин, магазин и служебные подключения не блокируются, чтобы игра могла запуститься.")
        }

        AboutCard("Важно знать") {
            NoticeRow("Приложение не меняет регион аккаунта и не обходит ограничения издателя.")
            NoticeRow("Игра может не найти матч, если все доступные серверы выбранного региона заняты.")
            NoticeRow("Перед включением полностью закройте MLBB, чтобы игра заново проверила серверы.")
            NoticeRow("GeoIP-база содержит только IPv4. Для IPv6-адресов город может не определиться.")
            NoticeRow("Блокировка по IP не гарантирует выбор конкретного сервера.")
        }

        AboutCard("Приватность") {
            Text(
                "Нет аналитики, аккаунтов и облачной синхронизации. Все правила, история и измерения хранятся только на этом устройстве. Трафик не отправляется на сторонние VPN-серверы.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AboutCard("Технологии и данные") {
            Text(
                "Kotlin, Jetpack Compose, sing-box/libbox, Room, MaxMind DB Reader. Геоданные: DB-IP Lite City, лицензия CC BY 4.0 (db-ip.com), сборка sapics/ip-location-db.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "MLBB Blocker ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · для личного использования",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun HeroCard() {
    Card(
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
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.size(14.dp))
                    Column {
                        Text(
                            "MLBB Blocker",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Локальный фильтр серверов Mobile Legends",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("3", "профиля", Modifier.weight(1f))
                    MiniStat("0", "удалённых серверов", Modifier.weight(1f))
                    MiniStat("100%", "локально", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            content()
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
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp).size(16.dp),
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
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
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
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
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 2.dp).size(16.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
