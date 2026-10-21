package ru.mlbb.blocker

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mlbb.blocker.data.BlockerPolicy
import ru.mlbb.blocker.data.GamePorts
import ru.mlbb.blocker.data.RouteAction
import ru.mlbb.blocker.data.RuleEngine
import ru.mlbb.blocker.data.StartupEndpoints

class RuleEngineTest {

    private val policy = BlockerPolicy(
        allowCidrs = listOf("154.85.91.0/24"),
        startupCidrs = StartupEndpoints.CIDRS,
        userBlockCidrs = listOf("92.223.91.0/24"),
        profileBlockCidrs = listOf("146.185.199.0/24"),
        blockOtherMatchPorts = true,
    )

    @Test
    fun allowsSelectedRegionAndStartupServer() {
        assertEquals(
            RouteAction.DIRECT,
            RuleEngine.decide("154.85.91.151", GamePorts.MATCH_PORT, policy).action,
        )
        assertEquals(
            RouteAction.DIRECT,
            RuleEngine.decide("154.85.91.151", 443, policy).action,
        )
    }

    @Test
    fun blocksUserRulesOnAllPorts() {
        val decision = RuleEngine.decide("92.223.91.7", 443, policy)
        assertEquals(RouteAction.SINK, decision.action)
        assertEquals("user blocklist", decision.reason)
    }

    @Test
    fun profileOnlyBlocksMatchPorts() {
        assertEquals(
            RouteAction.DIRECT,
            RuleEngine.decide("146.185.199.90", 443, policy).action,
        )
        assertEquals(
            RouteAction.SINK,
            RuleEngine.decide("146.185.199.90", GamePorts.MATCH_PORT, policy).action,
        )
        assertEquals(
            RouteAction.SINK,
            RuleEngine.decide("146.185.199.90", 5054, policy).action,
        )
    }

    @Test
    fun safeServicePortsAreAlwaysDirect() {
        for (port in GamePorts.SAFE_PORTS) {
            assertEquals(
                RouteAction.DIRECT,
                RuleEngine.decide("92.223.91.7", port, policy).action,
            )
        }
    }

    @Test
    fun catchAllBlocksUnknownMatchServers() {
        val decision = RuleEngine.decide("203.0.113.1", GamePorts.MATCH_PORT, policy)
        assertEquals(RouteAction.SINK, decision.action)
        assertEquals("profile allows only selected region", decision.reason)
    }

    @Test
    fun privateAddressesAreDirect() {
        assertEquals(RouteAction.DIRECT, RuleEngine.decide("192.168.1.10", 10003, policy).action)
    }
}
