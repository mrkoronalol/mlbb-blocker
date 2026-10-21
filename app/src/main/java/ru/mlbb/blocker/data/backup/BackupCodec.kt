package ru.mlbb.blocker.data.backup

import org.json.JSONArray
import org.json.JSONObject
import ru.mlbb.blocker.data.AllowRule
import ru.mlbb.blocker.data.BlockRule
import ru.mlbb.blocker.data.db.BlockedAttemptEntity
import ru.mlbb.blocker.data.db.EndpointEntity
import ru.mlbb.blocker.data.db.MatchSessionEntity
import ru.mlbb.blocker.data.db.PingSampleEntity

data class BackupSettings(
    val discoveryMode: Boolean = false,
    val fastReject: Boolean = true,
    val matchAlerts: Boolean = true,
    val hideStartupAlerts: Boolean = true,
    val autoStart: Boolean = true,
    val onboardingDone: Boolean = true,
    val logBlockedAttempts: Boolean = true,
)

data class BackupData(
    val schemaVersion: Int = BackupCodec.SCHEMA_VERSION,
    val appVersion: String,
    val createdAt: Long,
    val selectedProfile: String?,
    val settings: BackupSettings,
    val rules: List<BlockRule>,
    val allowRules: List<AllowRule>,
    val endpoints: List<EndpointEntity>,
    val blockedAttempts: List<BlockedAttemptEntity>,
    val matchSessions: List<MatchSessionEntity>,
    val pingSamples: List<PingSampleEntity>,
)

data class BackupSummary(
    val appVersion: String,
    val createdAt: Long,
    val rules: Int,
    val allowRules: Int,
    val endpoints: Int,
    val blockedAttempts: Int,
    val matchSessions: Int,
    val pingSamples: Int,
)

object BackupCodec {

    const val SCHEMA_VERSION = 1

    fun encode(data: BackupData): String {
        val root = JSONObject()
            .put("schema", data.schemaVersion)
            .put("appVersion", data.appVersion)
            .put("createdAt", data.createdAt)
        if (data.selectedProfile == null) {
            root.put("selectedProfile", JSONObject.NULL)
        } else {
            root.put("selectedProfile", data.selectedProfile)
        }

        root.put(
            "settings",
            JSONObject()
                .put("discoveryMode", data.settings.discoveryMode)
                .put("fastReject", data.settings.fastReject)
                .put("matchAlerts", data.settings.matchAlerts)
                .put("hideStartupAlerts", data.settings.hideStartupAlerts)
                .put("autoStart", data.settings.autoStart)
                .put("onboardingDone", data.settings.onboardingDone)
                .put("logBlockedAttempts", data.settings.logBlockedAttempts),
        )

        root.put("rules", JSONArray().also { array -> data.rules.forEach { array.put(it.toJson()) } })
        root.put(
            "allowRules",
            JSONArray().also { array -> data.allowRules.forEach { array.put(it.toJson()) } },
        )
        root.put(
            "endpoints",
            JSONArray().also { array -> data.endpoints.forEach { array.put(endpointJson(it)) } },
        )
        root.put(
            "blockedAttempts",
            JSONArray().also { array -> data.blockedAttempts.forEach { array.put(blockedJson(it)) } },
        )
        root.put(
            "matchSessions",
            JSONArray().also { array -> data.matchSessions.forEach { array.put(sessionJson(it)) } },
        )
        root.put(
            "pingSamples",
            JSONArray().also { array -> data.pingSamples.forEach { array.put(pingJson(it)) } },
        )
        return root.toString(2)
    }

    fun decode(text: String): BackupData {
        val root = JSONObject(text)
        val schema = root.optInt("schema", 0)
        require(schema in 1..SCHEMA_VERSION) { "Неподдерживаемая версия резервной копии: $schema" }
        val settings = root.optJSONObject("settings") ?: JSONObject()
        return BackupData(
            schemaVersion = schema,
            appVersion = root.optString("appVersion"),
            createdAt = root.optLong("createdAt", System.currentTimeMillis()),
            selectedProfile = root.optString("selectedProfile")
                .takeIf { it.isNotBlank() && !root.isNull("selectedProfile") },
            settings = BackupSettings(
                discoveryMode = settings.optBoolean("discoveryMode", false),
                fastReject = settings.optBoolean("fastReject", true),
                matchAlerts = settings.optBoolean("matchAlerts", true),
                hideStartupAlerts = settings.optBoolean("hideStartupAlerts", true),
                autoStart = settings.optBoolean("autoStart", true),
                onboardingDone = settings.optBoolean("onboardingDone", true),
                logBlockedAttempts = settings.optBoolean("logBlockedAttempts", true),
            ),
            rules = root.optJSONArray("rules").mapObjects(BlockRule::fromJson),
            allowRules = root.optJSONArray("allowRules").mapObjects(AllowRule::fromJson),
            endpoints = root.optJSONArray("endpoints").mapObjects(::endpointFromJson),
            blockedAttempts = root.optJSONArray("blockedAttempts").mapObjects(::blockedFromJson),
            matchSessions = root.optJSONArray("matchSessions").mapObjects(::sessionFromJson),
            pingSamples = root.optJSONArray("pingSamples").mapObjects(::pingFromJson),
        )
    }

    fun summary(data: BackupData): BackupSummary {
        return BackupSummary(
            appVersion = data.appVersion,
            createdAt = data.createdAt,
            rules = data.rules.size,
            allowRules = data.allowRules.size,
            endpoints = data.endpoints.size,
            blockedAttempts = data.blockedAttempts.size,
            matchSessions = data.matchSessions.size,
            pingSamples = data.pingSamples.size,
        )
    }

    private fun endpointJson(endpoint: EndpointEntity): JSONObject {
        return JSONObject()
            .put("network", endpoint.network)
            .put("ip", endpoint.ip)
            .put("port", endpoint.port)
            .put("host", endpoint.host ?: JSONObject.NULL)
            .put("firstSeen", endpoint.firstSeen)
            .put("lastSeen", endpoint.lastSeen)
            .put("sessions", endpoint.sessions)
            .put("pingMs", endpoint.pingMs ?: JSONObject.NULL)
            .put("pingJitterMs", endpoint.pingJitterMs ?: JSONObject.NULL)
            .put("pingKind", endpoint.pingKind ?: JSONObject.NULL)
            .put("pingAt", endpoint.pingAt)
            .put("blockedAttempts", endpoint.blockedAttempts)
            .put("city", endpoint.city ?: JSONObject.NULL)
            .put("country", endpoint.country ?: JSONObject.NULL)
            .put("geoAt", endpoint.geoAt)
    }

    private fun endpointFromJson(json: JSONObject): EndpointEntity {
        return EndpointEntity(
            network = json.optString("network", "tcp").ifBlank { "tcp" },
            ip = json.optString("ip"),
            port = json.optInt("port"),
            host = json.nullableString("host"),
            firstSeen = json.optLong("firstSeen"),
            lastSeen = json.optLong("lastSeen"),
            sessions = json.optInt("sessions"),
            pingMs = json.nullableLong("pingMs"),
            pingJitterMs = json.nullableLong("pingJitterMs"),
            pingKind = json.nullableString("pingKind"),
            pingAt = json.optLong("pingAt"),
            blockedAttempts = json.optInt("blockedAttempts"),
            city = json.nullableString("city"),
            country = json.nullableString("country"),
            geoAt = json.optLong("geoAt"),
        )
    }

    private fun blockedJson(attempt: BlockedAttemptEntity): JSONObject {
        return JSONObject()
            .put("timestamp", attempt.timestamp)
            .put("ip", attempt.ip)
            .put("port", attempt.port)
            .put("network", attempt.network)
            .put("matchedProfile", attempt.matchedProfile ?: JSONObject.NULL)
    }

    private fun blockedFromJson(json: JSONObject): BlockedAttemptEntity {
        return BlockedAttemptEntity(
            timestamp = json.optLong("timestamp"),
            ip = json.optString("ip"),
            port = json.optInt("port"),
            network = json.optString("network", "tcp").ifBlank { "tcp" },
            matchedProfile = json.nullableString("matchedProfile"),
        )
    }

    private fun sessionJson(session: MatchSessionEntity): JSONObject {
        return JSONObject()
            .put("startedAt", session.startedAt)
            .put("endedAt", session.endedAt ?: JSONObject.NULL)
            .put("profiles", session.profiles)
            .put("gamePackage", session.gamePackage)
            .put("matchIp", session.matchIp ?: JSONObject.NULL)
            .put("city", session.city ?: JSONObject.NULL)
            .put("result", session.result ?: JSONObject.NULL)
    }

    private fun sessionFromJson(json: JSONObject): MatchSessionEntity {
        return MatchSessionEntity(
            startedAt = json.optLong("startedAt"),
            endedAt = json.nullableLong("endedAt"),
            profiles = json.optString("profiles"),
            gamePackage = json.optString("gamePackage"),
            matchIp = json.nullableString("matchIp"),
            city = json.nullableString("city"),
            result = json.nullableString("result"),
        )
    }

    private fun pingJson(sample: PingSampleEntity): JSONObject {
        return JSONObject()
            .put("endpointKey", sample.endpointKey)
            .put("timestamp", sample.timestamp)
            .put("ms", sample.ms ?: JSONObject.NULL)
            .put("jitterMs", sample.jitterMs ?: JSONObject.NULL)
            .put("kind", sample.kind)
            .put("lossPercent", sample.lossPercent ?: JSONObject.NULL)
    }

    private fun pingFromJson(json: JSONObject): PingSampleEntity {
        return PingSampleEntity(
            endpointKey = json.optString("endpointKey"),
            timestamp = json.optLong("timestamp"),
            ms = json.nullableLong("ms"),
            jitterMs = json.nullableLong("jitterMs"),
            kind = json.optString("kind"),
            lossPercent = json.nullableInt("lossPercent"),
        )
    }

    private fun JSONObject.nullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.nullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key)
    }

    private fun JSONObject.nullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }

    private fun <T> JSONArray?.mapObjects(mapper: (JSONObject) -> T?): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                mapper(item)?.let(::add)
            }
        }
    }
}
