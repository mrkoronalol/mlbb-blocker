package ru.mlbb.blocker.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Палитра сайта mlbb-meta (docs/design-system.md): тёмный navy, синий акцент,
 * cyan для данных, фиолетовый для S-тира, золото для premium.
 */
object AppColors {
    val Background = Color(0xFF0B0F19)
    val Surface = Color(0xFF14182A)
    val SurfaceHigh = Color(0xFF1A2235)
    val SurfaceHigher = Color(0xFF1B2036)
    val Border = Color(0xFF2A3756)
    val BorderSoft = Color(0xFF223050)

    val Blue = Color(0xFF637FE8)
    val BlueLight = Color(0xFF7898F2)
    val Cyan = Color(0xFF68A9E8)
    val CyanLight = Color(0xFF8FD8FF)
    val Purple = Color(0xFF765FE8)
    val Gold = Color(0xFFD9A85C)
    val GoldLight = Color(0xFFF3C27D)

    val Text = Color(0xFFE8ECF5)
    val TextMuted = Color(0xFFA8B1C9)
    val Success = Color(0xFF55D6A3)
    val Danger = Color(0xFFE96A7A)
    val Warning = Color(0xFFF0B95A)
}

private val NavyColors = darkColorScheme(
    primary = AppColors.Blue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF25305A),
    onPrimaryContainer = Color(0xFFD9E0FF),
    secondary = AppColors.Cyan,
    onSecondary = Color(0xFF04121F),
    secondaryContainer = Color(0xFF1B2B44),
    onSecondaryContainer = AppColors.CyanLight,
    tertiary = AppColors.Purple,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF2C2554),
    onTertiaryContainer = Color(0xFFD6CCFF),
    background = AppColors.Background,
    onBackground = AppColors.Text,
    surface = AppColors.Surface,
    onSurface = AppColors.Text,
    surfaceVariant = AppColors.SurfaceHigh,
    onSurfaceVariant = AppColors.TextMuted,
    surfaceContainer = AppColors.Surface,
    surfaceContainerHigh = AppColors.SurfaceHigh,
    surfaceContainerHighest = AppColors.SurfaceHigher,
    outline = AppColors.Border,
    outlineVariant = AppColors.BorderSoft,
    error = AppColors.Danger,
    onError = Color(0xFF2A0509),
    errorContainer = Color(0xFF3A1219),
    onErrorContainer = Color(0xFFFFDADF),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

/**
 * Тема приложения повторяет тёмный стиль сайта. Динамические цвета отключены:
 * бренд важнее системной палитры.
 */
@Composable
fun MlbbBlockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NavyColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
