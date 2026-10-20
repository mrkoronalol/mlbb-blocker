package ru.mlbb.blocker

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.data.UpdateChecker

class UpdateCheckerTest {

    @Test
    fun comparesVersionsNumerically() {
        assertTrue(UpdateChecker.isNewer("0.11.0", "0.10.0"))
        assertTrue(UpdateChecker.isNewer("1.0.0", "0.99.9"))
        assertTrue(UpdateChecker.isNewer("0.10.1", "0.10.0"))
        assertFalse(UpdateChecker.isNewer("0.10.0", "0.10.0"))
        assertFalse(UpdateChecker.isNewer("0.9.9", "0.10.0"))
    }

    @Test
    fun parsesReleaseAndPrefersArm64Apk() {
        val json = JSONObject(
            """
            {
              "tag_name": "v0.11.0",
              "name": "MLBB Blocker v0.11.0",
              "html_url": "https://github.com/mrkoronalol/mlbb-blocker/releases/tag/v0.11.0",
              "body": "notes",
              "assets": [
                {
                  "name": "MLBB-Blocker-0.11.0-universal.apk",
                  "browser_download_url": "https://example.com/universal.apk"
                },
                {
                  "name": "MLBB-Blocker-0.11.0-arm64.apk",
                  "browser_download_url": "https://example.com/arm64.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        val info = UpdateChecker.parseRelease(json, "0.10.0")
        assertNotNull(info)
        assertEquals("0.11.0", info!!.version)
        assertEquals("https://example.com/arm64.apk", info.downloadUrl)
        assertEquals("https://github.com/mrkoronalol/mlbb-blocker/releases/tag/v0.11.0", info.releaseUrl)
        assertNull(UpdateChecker.parseRelease(json, "0.11.0"))
    }
}
