package ru.mlbb.blocker.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B59),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9FF2DC),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF006688),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF5FAF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDCE5E1),
    onSurfaceVariant = Color(0xFF404946),
    outline = Color(0xFF707975),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

@Composable
fun MlbbBlockerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColors else LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content,
    )
}
