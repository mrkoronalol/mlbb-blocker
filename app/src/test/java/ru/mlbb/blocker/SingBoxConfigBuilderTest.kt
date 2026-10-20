package ru.mlbb.blocker

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mlbb.blocker.config.SingBoxConfigBuilder

class SingBoxConfigBuilderTest {

    @Test
    fun blockingConfigRoutesBlockedSubnetsToSink() {
        val config = JSONObject(
            SingBoxConfigBuilder.build(
                rules = listOf("154.85.91.0/24", "66.253.15.83/32"),
                discoveryMode = false,
                clashPort = 39090,
                clashSecret = "test-secret",
            ),
        )

        val route = config.getJSONObject("route")
        assertEquals("direct", route.getString("final"))
        assertEquals(true, route.getBoolean("auto_detect_interface"))

        val rules = route.getJSONArray("rules")
        var sinkRuleFound = false
        for (index in 0 until rules.length()) {
            val rule = rules.getJSONObject(index)
            if (rule.optString("outbound") == "sink") {
                sinkRuleFound = true
                assertEquals(2, rule.getJSONArray("ip_cidr").length())
            }
        }
        assertTrue(sinkRuleFound)

        var sinkOutboundFound = false
        val outbounds = config.getJSONArray("outbounds")
        for (index in 0 until outbounds.length()) {
            val outbound = outbounds.getJSONObject(index)
            if (outbound.optString("tag") == "sink") {
                sinkOutboundFound = true
                assertEquals("socks", outbound.getString("type"))
                assertEquals("127.0.0.1", outbound.getString("server"))
            }
        }
        assertTrue(sinkOutboundFound)

        val tun = config.getJSONArray("inbounds").getJSONObject(0)
        assertEquals("tun", tun.getString("type"))
        val packages = tun.getJSONArray("include_package")
        assertEquals(SingBoxConfigBuilder.MLBB_PACKAGES.size, packages.length())
        assertEquals(SingBoxConfigBuilder.MLBB_PACKAGE, packages.getString(0))
        assertEquals(SingBoxConfigBuilder.MLBB_PACKAGE_XIAOMI, packages.getString(1))

        val clash = config.getJSONObject("experimental").getJSONObject("clash_api")
        assertEquals("127.0.0.1:39090", clash.getString("external_controller"))
        assertEquals("test-secret", clash.getString("secret"))
    }

    @Test
    fun allowRulesComeBeforeBlockRule() {
        val config = JSONObject(
            SingBoxConfigBuilder.build(
                rules = listOf("154.85.91.0/24"),
                allowRules = listOf("154.85.91.153/32"),
                discoveryMode = false,
                clashPort = 39090,
                clashSecret = "test-secret",
            ),
        )
        val rules = config.getJSONObject("route").getJSONArray("rules")
        var allowIndex = -1
        var blockIndex = -1
        for (index in 0 until rules.length()) {
            val rule = rules.getJSONObject(index)
            if (rule.optString("outbound") == "direct" && rule.has("ip_cidr")) {
                allowIndex = index
            }
            if (rule.optString("outbound") == "sink") {
                blockIndex = index
            }
        }
        assertTrue(allowIndex >= 0)
        assertTrue(blockIndex >= 0)
        assertTrue(allowIndex < blockIndex)
    }

    @Test
    fun discoveryModeOmitsBlockRule() {
        val config = JSONObject(
            SingBoxConfigBuilder.build(
                rules = listOf("154.85.91.0/24"),
                discoveryMode = true,
                clashPort = 39090,
                clashSecret = "test-secret",
            ),
        )
        val rules = config.getJSONObject("route").getJSONArray("rules")
        for (index in 0 until rules.length()) {
            assertFalse(rules.getJSONObject(index).optString("outbound") == "sink")
        }
    }

    @Test
    fun allowOnlyProfileBlocksOtherMatchPortsAfterAllowRule() {
        val config = JSONObject(
            SingBoxConfigBuilder.build(
                rules = listOf("146.185.199.0/24"),
                allowRules = listOf("154.85.91.0/24"),
                blockMatchPorts = true,
                discoveryMode = false,
                clashPort = 39090,
                clashSecret = "test-secret",
            ),
        )
        val rules = config.getJSONObject("route").getJSONArray("rules")
        var allowIndex = -1
        var portBlockIndex = -1
        var rangeBlockIndex = -1
        for (index in 0 until rules.length()) {
            val rule = rules.getJSONObject(index)
            if (rule.optString("outbound") == "direct" && rule.has("ip_cidr")) allowIndex = index
            if (rule.optString("outbound") == "sink" && rule.has("port")) portBlockIndex = index
            if (rule.optString("outbound") == "sink" && rule.has("port_range")) rangeBlockIndex = index
        }
        assertTrue(allowIndex >= 0)
        assertTrue(portBlockIndex > allowIndex)
        assertTrue(rangeBlockIndex > allowIndex)
        assertEquals(10003, rules.getJSONObject(portBlockIndex).getJSONArray("port").getInt(0))
        assertEquals("5000:5054", rules.getJSONObject(rangeBlockIndex).getJSONArray("port_range").getString(0))
    }

    @Test
    fun discoveryModeOmitsMatchPortBlock() {
        val config = JSONObject(
            SingBoxConfigBuilder.build(
                rules = emptyList(),
                allowRules = listOf("154.85.91.0/24"),
                blockMatchPorts = true,
                discoveryMode = true,
                clashPort = 39090,
                clashSecret = "test-secret",
            ),
        )
        val rules = config.getJSONObject("route").getJSONArray("rules")
        for (index in 0 until rules.length()) {
            assertFalse(rules.getJSONObject(index).optString("outbound") == "sink")
        }
    }
}
