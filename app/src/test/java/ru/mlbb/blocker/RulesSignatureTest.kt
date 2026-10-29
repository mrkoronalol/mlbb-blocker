package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.rules.RulesSignature
import java.security.KeyPairGenerator
import java.security.Signature

class RulesSignatureTest {

    private fun keyPair(algorithm: String) = KeyPairGenerator.getInstance(algorithm).apply {
        initialize(2048)
    }.generateKeyPair()

    @Test
    fun verifiesRsaSignature() {
        val keys = keyPair("RSA")
        val content = """{"schemaVersion":1}""".toByteArray()
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(keys.private)
            update(content)
            sign()
        }
        val base64 = java.util.Base64.getEncoder().encodeToString(signature)
        assertTrue(RulesSignature.verify(keys.public, content, base64))
    }

    @Test
    fun rejectsTamperedContent() {
        val keys = keyPair("RSA")
        val content = """{"schemaVersion":1}""".toByteArray()
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(keys.private)
            update(content)
            sign()
        }
        val base64 = java.util.Base64.getEncoder().encodeToString(signature)
        assertFalse(RulesSignature.verify(keys.public, """{"schemaVersion":2}""".toByteArray(), base64))
    }

    @Test
    fun rejectsSignatureFromAnotherKey() {
        val signer = keyPair("RSA")
        val stranger = keyPair("RSA")
        val content = "rules".toByteArray()
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(signer.private)
            update(content)
            sign()
        }
        val base64 = java.util.Base64.getEncoder().encodeToString(signature)
        assertFalse(RulesSignature.verify(stranger.public, content, base64))
    }

    @Test
    fun rejectsMalformedSignature() {
        val keys = keyPair("RSA")
        assertFalse(RulesSignature.verify(keys.public, "rules".toByteArray(), "not base64 !!"))
        assertFalse(RulesSignature.verify(keys.public, "rules".toByteArray(), ""))
    }

    @Test
    fun mapsKeyAlgorithms() {
        assertEquals("SHA256withRSA", RulesSignature.algorithmFor("RSA"))
        assertEquals("SHA256withECDSA", RulesSignature.algorithmFor("EC"))
        assertNull(RulesSignature.algorithmFor("unknown"))
    }

    @Test
    fun decodesBase64WithWhitespace() {
        val decoded = RulesSignature.decodeBase64("aGVs\nbG8g d29ybGQ=")
        assertEquals("hello world", String(decoded!!, Charsets.UTF_8))
    }
}
