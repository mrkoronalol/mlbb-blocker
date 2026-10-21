package ru.mlbb.blocker.data

enum class RouteAction {
    DIRECT,
    SINK,
}

data class RouteDecision(
    val action: RouteAction,
    val reason: String,
    val matchedRule: String? = null,
)

data class BlockerPolicy(
    val allowCidrs: List<String> = emptyList(),
    val startupCidrs: List<String> = StartupEndpoints.CIDRS,
    val safePorts: Set<Int> = GamePorts.SAFE_PORTS,
    val userBlockCidrs: List<String> = emptyList(),
    val profileBlockCidrs: List<String> = emptyList(),
    val matchPorts: Set<Int> = GamePorts.MATCH_PORTS,
    val blockOtherMatchPorts: Boolean = false,
)

object RuleEngine {

    fun decide(ip: String, port: Int, policy: BlockerPolicy): RouteDecision {
        if (Cidr.isPrivate(ip)) {
            return RouteDecision(RouteAction.DIRECT, "private address")
        }
        policy.allowCidrs.firstOrNull { Cidr.contains(it, ip) }?.let {
            return RouteDecision(RouteAction.DIRECT, "allow rule", it)
        }
        policy.startupCidrs.firstOrNull { Cidr.contains(it, ip) }?.let {
            return RouteDecision(RouteAction.DIRECT, "startup server", it)
        }
        if (port in policy.safePorts) {
            return RouteDecision(RouteAction.DIRECT, "safe service port", port.toString())
        }
        policy.userBlockCidrs.firstOrNull { Cidr.contains(it, ip) }?.let {
            return RouteDecision(RouteAction.SINK, "user blocklist", it)
        }
        if (port in policy.matchPorts) {
            policy.profileBlockCidrs.firstOrNull { Cidr.contains(it, ip) }?.let {
                return RouteDecision(RouteAction.SINK, "profile blocklist", it)
            }
            if (policy.blockOtherMatchPorts) {
                return RouteDecision(RouteAction.SINK, "profile allows only selected region")
            }
        }
        return RouteDecision(RouteAction.DIRECT, "default")
    }
}
