package ru.mlbb.blocker.data.rules

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.ByteArrayInputStream
import java.security.PublicKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Проверка detached-подписи rules.json ключом, которым подписан сам APK.
 * Публичный ключ не хранится в коде, а читается из установленного пакета,
 * поэтому подменить правила может только тот, кто может выпустить обновление.
 */
object RulesSignature {

    fun algorithmFor(keyAlgorithm: String?): String? = when (keyAlgorithm?.uppercase()) {
        "RSA" -> "SHA256withRSA"
        "EC", "ECDSA" -> "SHA256withECDSA"
        "DSA" -> "SHA256withDSA"
        else -> null
    }

    fun verify(publicKey: PublicKey, content: ByteArray, signatureBase64: String): Boolean {
        val algorithm = algorithmFor(publicKey.algorithm) ?: return false
        val signatureBytes = decodeBase64(signatureBase64) ?: return false
        return try {
            val verifier = Signature.getInstance(algorithm)
            verifier.initVerify(publicKey)
            verifier.update(content)
            verifier.verify(signatureBytes)
        } catch (_: Throwable) {
            false
        }
    }

    /** Минимальный декодер Base64: java.util.Base64 требует API 26, а minSdk = 24. */
    fun decodeBase64(input: String): ByteArray? {
        val clean = input.filterNot { it.isWhitespace() }
        if (clean.isEmpty()) return null
        val output = java.io.ByteArrayOutputStream(clean.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (char in clean) {
            if (char == '=') break
            val value = when (char) {
                in 'A'..'Z' -> char - 'A'
                in 'a'..'z' -> char - 'a' + 26
                in '0'..'9' -> char - '0' + 52
                '+' -> 62
                '-' -> 62
                '/' -> 63
                '_' -> 63
                else -> return null
            }
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output.write((buffer shr bits) and 0xFF)
            }
        }
        return output.toByteArray()
    }

    /** Публичный ключ подписи установленного APK. */
    fun appPublicKey(context: Context): PublicKey? {
        return try {
            val manager = context.packageManager
            val packageName = context.packageName
            val certificates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = manager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners
                    ?: info.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                manager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            } ?: return null
            val first = certificates.firstOrNull() ?: return null
            val factory = CertificateFactory.getInstance("X.509")
            val certificate = factory.generateCertificate(
                ByteArrayInputStream(first.toByteArray()),
            ) as? X509Certificate ?: return null
            certificate.publicKey
        } catch (_: Throwable) {
            null
        }
    }
}
