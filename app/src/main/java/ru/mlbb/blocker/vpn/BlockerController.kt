package ru.mlbb.blocker.vpn

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import ru.mlbb.blocker.config.SingBoxConfigBuilder

object BlockerController {

    const val ACTION_START = "ru.mlbb.blocker.action.START"
    const val ACTION_STOP = "ru.mlbb.blocker.action.STOP"
    const val ACTION_RELOAD = "ru.mlbb.blocker.action.RELOAD"
    const val ACTION_ALLOW_TEMP = "ru.mlbb.blocker.action.ALLOW_TEMP"
    const val EXTRA_PORT = "clash_port"
    const val EXTRA_SECRET = "clash_secret"
    const val EXTRA_IP = "allow_ip"
    const val EXTRA_ALLOW_PORT = "allow_port"
    const val EXTRA_TTL = "allow_ttl"

    fun isMlbbInstalled(context: Context): Boolean {
        return SingBoxConfigBuilder.MLBB_PACKAGES.any { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun start(context: Context, port: Int, secret: String) {
        val intent = Intent(context, BlockerVpnService::class.java)
            .setAction(ACTION_START)
            .putExtra(EXTRA_PORT, port)
            .putExtra(EXTRA_SECRET, secret)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        val intent = Intent(context, BlockerVpnService::class.java).setAction(ACTION_STOP)
        context.startService(intent)
    }

    fun reload(context: Context) {
        val intent = Intent(context, BlockerVpnService::class.java).setAction(ACTION_RELOAD)
        context.startService(intent)
    }

    fun allowTemporary(context: Context, ip: String, port: Int, ttlMs: Long = 5 * 60 * 1000L) {
        val intent = Intent(context, BlockerVpnService::class.java)
            .setAction(ACTION_ALLOW_TEMP)
            .putExtra(EXTRA_IP, ip)
            .putExtra(EXTRA_ALLOW_PORT, port)
            .putExtra(EXTRA_TTL, ttlMs)
        context.startService(intent)
    }
}
