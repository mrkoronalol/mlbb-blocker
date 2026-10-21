package ru.mlbb.blocker.config

import org.json.JSONArray
import org.json.JSONObject
import ru.mlbb.blocker.BuildConfig
import ru.mlbb.blocker.data.GamePorts

object SingBoxConfigBuilder {

    const val MLBB_PACKAGE = "com.mobile.legends"
    const val MLBB_PACKAGE_XIAOMI = "com.mobilelegends.mi"
    val MLBB_PACKAGES = listOf(MLBB_PACKAGE, MLBB_PACKAGE_XIAOMI)
    const val TEST_PACKAGE = "com.android.shell"

    fun build(
        rules: List<String>,
        profileRules: List<String> = emptyList(),
        allowRules: List<String> = emptyList(),
        blockMatchPorts: Boolean = false,
        discoveryMode: Boolean,
        clashPort: Int,
        clashSecret: String,
        mtu: Int = 1400,
        rejectMethod: String = "drop",
        logOutput: String? = null,
        logLevel: String = "info",
    ): String {
        val includedPackages = MLBB_PACKAGES + if (BuildConfig.DEBUG) listOf(TEST_PACKAGE) else emptyList()
        val root = JSONObject()

        val log = JSONObject()
            .put("level", logLevel)
            .put("timestamp", true)
        if (logOutput != null) {
            log.put("output", logOutput)
        }
        root.put("log", log)

        root.put(
            "dns",
            JSONObject()
                .put(
                    "servers",
                    JSONArray().put(
                        JSONObject()
                            .put("type", "local")
                            .put("tag", "local"),
                    ),
                )
                .put("final", "local"),
        )

        val tun = JSONObject()
            .put("type", "tun")
            .put("tag", "tun-in")
            .put("address", JSONArray().put("172.19.0.1/30").put("fdfe:dcba:9876::1/126"))
            .put("mtu", mtu)
            .put("auto_route", true)
            .put("strict_route", false)
            .put("stack", "gvisor")
            .put("include_package", JSONArray().also { array -> includedPackages.forEach(array::put) })

        root.put("inbounds", JSONArray().put(tun))
        root.put(
            "outbounds",
            JSONArray()
                .put(
                    JSONObject()
                        .put("type", "direct")
                        .put("tag", "direct"),
                )
                .put(
                    JSONObject()
                        .put("type", "socks")
                        .put("tag", "sink")
                        .put("server", "127.0.0.1")
                        .put("server_port", 9)
                        .put("version", "5"),
                ),
        )

        val routeRules = JSONArray()
        routeRules.put(
            JSONObject()
                .put("ip_is_private", true)
                .put("action", "route")
                .put("outbound", "direct"),
        )
        if (allowRules.isNotEmpty()) {
            routeRules.put(
                JSONObject()
                    .put("ip_cidr", JSONArray(allowRules))
                    .put("action", "route")
                    .put("outbound", "direct"),
            )
        }
        routeRules.put(
            JSONObject()
                .put("port", JSONArray(GamePorts.SAFE_PORT_LIST))
                .put("action", "route")
                .put("outbound", "direct"),
        )
        if (!discoveryMode && rules.isNotEmpty()) {
            routeRules.put(
                JSONObject()
                    .put("ip_cidr", JSONArray(rules))
                    .put("action", "route")
                    .put("outbound", "sink"),
            )
        }
        if (!discoveryMode && profileRules.isNotEmpty()) {
            routeRules.put(
                JSONObject()
                    .put("ip_cidr", JSONArray(profileRules))
                    .put("port", JSONArray().put(GamePorts.MATCH_PORT))
                    .put("action", "route")
                    .put("outbound", "sink"),
            )
            routeRules.put(
                JSONObject()
                    .put("ip_cidr", JSONArray(profileRules))
                    .put("port_range", JSONArray().put(GamePorts.MATCH_PORT_RANGE))
                    .put("action", "route")
                    .put("outbound", "sink"),
            )
        }
        if (!discoveryMode && blockMatchPorts) {
            routeRules.put(
                JSONObject()
                    .put("port", JSONArray().put(GamePorts.MATCH_PORT))
                    .put("action", "route")
                    .put("outbound", "sink"),
            )
            routeRules.put(
                JSONObject()
                    .put("port_range", JSONArray().put(GamePorts.MATCH_PORT_RANGE))
                    .put("action", "route")
                    .put("outbound", "sink"),
            )
        }

        root.put(
            "route",
            JSONObject()
                .put("auto_detect_interface", true)
                .put("default_domain_resolver", "local")
                .put("rules", routeRules)
                .put("final", "direct"),
        )

        root.put(
            "experimental",
            JSONObject().put(
                "clash_api",
                JSONObject()
                    .put("external_controller", "127.0.0.1:$clashPort")
                    .put("secret", clashSecret),
            ),
        )

        return root.toString(2)
    }
}
