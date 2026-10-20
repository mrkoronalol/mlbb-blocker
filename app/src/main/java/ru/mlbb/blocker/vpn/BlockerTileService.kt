package ru.mlbb.blocker.vpn

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.mlbb.blocker.MainActivity
import ru.mlbb.blocker.R
import ru.mlbb.blocker.config.BlockerConfig
import ru.mlbb.blocker.data.BlockerRepository
import ru.mlbb.blocker.data.BlockerStatus

class BlockerTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var statusJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        statusJob?.cancel()
        statusJob = scope.launch {
            BlockerRepository.status.collect {
                updateTile()
            }
        }
        updateTile()
    }

    override fun onStopListening() {
        statusJob?.cancel()
        statusJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        when (BlockerRepository.status.value) {
            BlockerStatus.Running, BlockerStatus.Starting -> {
                BlockerController.stop(this)
            }

            else -> startBlockerOrOpenApp()
        }
        updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startBlockerOrOpenApp() {
        if (!BlockerController.isMlbbInstalled(this)) {
            openApp()
            return
        }
        if (runCatching { VpnService.prepare(this) }.getOrNull() != null) {
            openApp()
            return
        }
        BlockerController.start(this, BlockerConfig.newPort(), BlockerConfig.newSecret())
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val status = BlockerRepository.status.value
        tile.state = if (status is BlockerStatus.Running || status is BlockerStatus.Starting) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (status) {
                BlockerStatus.Running -> getString(R.string.status_running)
                BlockerStatus.Starting -> getString(R.string.status_starting)
                is BlockerStatus.Error -> getString(R.string.status_error)
                else -> getString(R.string.status_stopped)
            }
        }
        tile.updateTile()
    }
}
