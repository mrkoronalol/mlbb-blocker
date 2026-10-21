package ru.mlbb.blocker.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.mlbb.blocker.UpdateInstallReceiver
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

sealed interface InstallResult {
    data object Started : InstallResult
    data object NeedPermission : InstallResult
    data object SignatureMismatch : InstallResult
    data class Error(val message: String) : InstallResult
}

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progress: Int, val received: Long, val total: Long) :
        UpdateDownloadState

    data class Ready(val file: File) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

object UpdateInstaller {

    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int, Long, Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "mlbb-${info.version}.apk")
        val temp = File(dir, "download.tmp")
        temp.delete()
        val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 120_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "MLBB-Blocker")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 }
                ?: info.size
                ?: 0L
            val digest = MessageDigest.getInstance("SHA-256")
            var received = 0L
            connection.inputStream.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        received += read
                        onProgress(
                            if (total > 0) ((received * 100) / total).toInt().coerceIn(0, 99) else 0,
                            received,
                            total,
                        )
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            val expected = info.sha256?.lowercase(Locale.US)
            if (expected != null && expected != actual) {
                temp.delete()
                throw IllegalStateException("SHA256 не совпадает")
            }
            target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            onProgress(100, received, total)
            target
        } finally {
            connection.disconnect()
        }
    }

    fun install(context: Context, file: File): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            openInstallSettings(context)
            return InstallResult.NeedPermission
        }
        if (!hasSameSignature(context, file)) {
            return InstallResult.SignatureMismatch
        }
        return try {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL,
            ).apply {
                setAppPackageName(context.packageName)
            }
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("base.apk", 0, file.length()).use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                    session.fsync(output)
                }
                val resultIntent = Intent(context, UpdateInstallReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                session.commit(pendingIntent.intentSender)
            }
            InstallResult.Started
        } catch (t: Throwable) {
            InstallResult.Error(t.message ?: t.javaClass.simpleName)
        }
    }

    private fun hasSameSignature(context: Context, file: File): Boolean {
        return try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
                ?: return false
            val installed = context.packageManager.getPackageInfo(context.packageName, flags)
            signatures(archive).isNotEmpty() && signatures(archive) == signatures(installed)
        } catch (_: Throwable) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun signatures(info: android.content.pm.PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        } ?: return emptySet()
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun openInstallSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
