package ru.mlbb.blocker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF31D0AA),
    onPrimary = Color(0xFF002019),
    primaryContainer = Color(0xFF06483B),
    onPrimaryContainer = Color(0xFFA7F2DF),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF00212F),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF121A22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1B2631),
    onSurfaceVariant = Color(0xFFA9B7C4),
    outline = Color(0xFF35424F),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3B0000),
    errorContainer = Color(0xFF4A1010),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun MlbbBlockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content,
    )
}
