package ru.mlbb.blocker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.mlbb.blocker.ui.BlockerApp
import ru.mlbb.blocker.ui.MlbbBlockerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var startAfterNotificationRequest = false
    private var pendingGamePackage: String? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.startVpn()
            if (pendingGamePackage != null) {
                launchGameSoon()
            }
        } else {
            pendingGamePackage = null
            viewModel.showMessage(getString(R.string.error_vpn_permission))
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        if (startAfterNotificationRequest) {
            startAfterNotificationRequest = false
            requestVpnPermission()
        }
    }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let(viewModel::exportBackup)
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::prepareImport)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !nightMode
            isAppearanceLightNavigationBars = !nightMode
        }
        setContent {
            MlbbBlockerTheme {
                val ui = viewModel.ui.collectAsStateWithLifecycle()
                BlockerApp(
                    ui = ui.value,
                    onToggle = ::toggleBlocker,
                    onDiscoveryChange = viewModel::toggleDiscovery,
                    onSelectProfile = viewModel::selectProfile,
                    onAddRule = viewModel::addRule,
                    onAddRules = viewModel::addRules,
                    onAddPreset = viewModel::addPreset,
                    onRemoveRule = viewModel::removeRule,
                    onSetRuleEnabled = viewModel::setRuleEnabled,
                    onClearRules = viewModel::clearRules,
                    onAddConnection = viewModel::addConnection,
                    onPing = viewModel::ping,
                    onPingAll = viewModel::pingAll,
                    onClearConnections = viewModel::clearConnections,
                    onClearLogs = viewModel::clearLogs,
                    onFastRejectChange = viewModel::setFastReject,
                    onMatchAlertsChange = viewModel::setMatchAlerts,
                    onHideStartupAlertsChange = viewModel::setHideStartupAlerts,
                    onAutoStartChange = viewModel::setAutoStart,
                    onFinishOnboarding = viewModel::finishOnboarding,
                    onShowMatchDemo = viewModel::showMatchDemo,
                    onRefreshDiagnostics = viewModel::refreshMlbbInstalled,
                    onDownloadGeoIp = viewModel::downloadGeoIp,
                    onExportBackup = {
                        exportBackupLauncher.launch(
                            "mlbb-blocker-backup-${System.currentTimeMillis()}.json",
                        )
                    },
                    onImportBackup = {
                        importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                    onApplyImport = viewModel::applyImport,
                    onCancelImport = viewModel::cancelImport,
                    onDownloadUpdate = viewModel::downloadUpdate,
                    onInstallUpdate = viewModel::installUpdate,
                    onStartGame = ::startMatch,
                    onMatchResult = viewModel::finishMatch,
                    onStartDiscovery = { viewModel.startDiscovery(60) },
                    onStopDiscovery = viewModel::stopDiscovery,
                    onAllow = viewModel::allowTemporarily,
                    onRemoveAllow = viewModel::removeAllowRule,
                    onMessageShown = viewModel::consumeMessage,
                    onShowMessage = viewModel::showMessage,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshMlbbInstalled()
    }

    private fun toggleBlocker() {
        val state = viewModel.ui.value
        if (state.isRunning || state.isBusy) {
            viewModel.stopVpn()
            return
        }
        if (!state.mlbbInstalled) {
            viewModel.showMessage(getString(R.string.error_mlbb_missing))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            startAfterNotificationRequest = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        requestVpnPermission()
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            viewModel.startVpn()
        } else {
            vpnPermissionLauncher.launch(intent)
        }
    }

    private fun startMatch(gamePackage: String) {
        viewModel.startMatch(gamePackage)
        pendingGamePackage = gamePackage
        if (viewModel.ui.value.isRunning) {
            launchGameSoon()
        } else {
            requestVpnPermissionForGame()
        }
    }

    private fun requestVpnPermissionForGame() {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            viewModel.startVpn()
            launchGameSoon()
        } else {
            vpnPermissionLauncher.launch(intent)
        }
    }

    private fun launchGameSoon() {
        window.decorView.postDelayed({
            val packageName = pendingGamePackage ?: return@postDelayed
            pendingGamePackage = null
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent == null) {
                viewModel.showMessage("Не удалось открыть MLBB")
                return@postDelayed
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }, 1500)
    }
}
