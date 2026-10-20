package ru.mlbb.blocker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ru.mlbb.blocker.R
import ru.mlbb.blocker.data.BlockerStatus
import java.util.concurrent.TimeUnit

@Composable
fun statusText(status: BlockerStatus): String {
    return when (status) {
        BlockerStatus.Stopped -> stringResource(R.string.status_stopped)
        BlockerStatus.Starting -> stringResource(R.string.status_starting)
        BlockerStatus.Running -> stringResource(R.string.status_running)
        is BlockerStatus.Error -> stringResource(R.string.status_error)
    }
}

fun statusColor(status: BlockerStatus): Color {
    return when (status) {
        BlockerStatus.Running -> Color(0xFF31D0AA)
        BlockerStatus.Starting -> Color(0xFFFFC857)
        BlockerStatus.Stopped -> Color(0xFF7C8B99)
        is BlockerStatus.Error -> Color(0xFFFF6B6B)
    }
}

fun formatAge(lastSeen: Long): String {
    val delta = (System.currentTimeMillis() - lastSeen).coerceAtLeast(0)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(delta)
    return when {
        seconds < 2 -> "только что"
        seconds < 60 -> "$seconds с назад"
        seconds < 3600 -> "${seconds / 60} мин назад"
        seconds < 86400 -> "${seconds / 3600} ч назад"
        else -> "${seconds / 86400} д назад"
    }
}
