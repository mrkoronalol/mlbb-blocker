package ru.mlbb.blocker.data

import android.content.Context
import android.net.VpnService
import ru.mlbb.blocker.data.geo.GeoIpRepository
import ru.mlbb.blocker.data.geo.GeoIpStatus
import ru.mlbb.blocker.data.rules.RuleConfigRepository
import ru.mlbb.blocker.vpn.BlockerController
import java.io.File

enum class HealthIssue {
    /** Блокировка должна работать, но VPN-сервис не запущен. */
    SERVICE_STOPPED,

    /** Встроенная или скачанная база GeoIP не открывается. */
    GEOIP_FAILED,

    /** Правила с сервера не прошли проверку, используется запасной набор. */
    RULES_FALLBACK,
}

data class HealthSnapshot(
    val expectedRunning: Boolean = false,
    val running: Boolean = false,
    val vpnPermissionGranted: Boolean = true,
    val mlbbInstalled: Boolean = true,
    val geoIpReady: Boolean = true,
    val rulesFallback: Boolean = false,
)

object BlockerHealth {

    fun issues(snapshot: HealthSnapshot): List<HealthIssue> = buildList {
        if (snapshot.expectedRunning && !snapshot.running &&
            snapshot.vpnPermissionGranted && snapshot.mlbbInstalled
        ) {
            add(HealthIssue.SERVICE_STOPPED)
        }
        if (!snapshot.geoIpReady) add(HealthIssue.GEOIP_FAILED)
        if (snapshot.rulesFallback) add(HealthIssue.RULES_FALLBACK)
    }

    fun inspect(context: Context, expectedRunning: Boolean, running: Boolean): HealthSnapshot {
        val store = BlocklistStore(context)
        val geoStatus = GeoIpRepository.status.value
        return HealthSnapshot(
            expectedRunning = expectedRunning,
            running = running,
            vpnPermissionGranted = runCatching { VpnService.prepare(context) == null }
                .getOrDefault(false),
            mlbbInstalled = BlockerController.isMlbbInstalled(context),
            geoIpReady = geoStatus !is GeoIpStatus.Error && geoFileLooksValid(context),
            rulesFallback = RuleConfigRepository.isFallback,
        )
    }

    /** Клик по баннеру: включаем VPN, обновляем GeoIP или правила по проблеме. */
    fun recoveryAction(issue: HealthIssue): RecoveryAction = when (issue) {
        HealthIssue.SERVICE_STOPPED -> RecoveryAction.START_VPN
        HealthIssue.GEOIP_FAILED -> RecoveryAction.RELOAD_GEOIP
        HealthIssue.RULES_FALLBACK -> RecoveryAction.REFRESH_RULES
    }

    enum class RecoveryAction { START_VPN, RELOAD_GEOIP, REFRESH_RULES }

    private fun geoFileLooksValid(context: Context): Boolean {
        val dir = File(context.filesDir, "geoip")
        val files = dir.listFiles { file -> file.isFile && file.name.endsWith(".mmdb") } ?: return false
        return files.any { it.length() > MIN_GEOIP_BYTES }
    }

    private const val MIN_GEOIP_BYTES = 1L * 1024 * 1024

    fun isExpectedRunning(context: Context): Boolean = BlocklistStore(context).lastRunning
}
