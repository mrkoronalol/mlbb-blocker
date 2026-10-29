package ru.mlbb.blocker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BlocklistStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var discoveryMode: Boolean
        get() = prefs.getBoolean(KEY_DISCOVERY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DISCOVERY, value).apply()
        }

    var fastReject: Boolean
        get() = prefs.getBoolean(KEY_FAST_REJECT, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FAST_REJECT, value).apply()
        }

    var logBlockedAttempts: Boolean
        get() = prefs.getBoolean(KEY_LOG_BLOCKED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_LOG_BLOCKED, value).apply()
        }

    var matchAlerts: Boolean
        get() = prefs.getBoolean(KEY_MATCH_ALERTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MATCH_ALERTS, value).apply()
        }

    var hideStartupAlerts: Boolean
        get() = prefs.getBoolean(KEY_HIDE_STARTUP_ALERTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HIDE_STARTUP_ALERTS, value).apply()
        }

    var autoStart: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_START, value).apply()
        }

    var allowStartupServer: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_STARTUP_SERVER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ALLOW_STARTUP_SERVER, value).apply()
        }

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()
        }

    var unknownPortAlerts: Boolean
        get() = prefs.getBoolean(KEY_UNKNOWN_PORT_ALERTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_UNKNOWN_PORT_ALERTS, value).apply()
        }

    val extraMatchPorts: Set<Int>
        get() = prefs.getStringSet(KEY_EXTRA_MATCH_PORTS, null)
            ?.mapNotNull(String::toIntOrNull)
            ?.toSet()
            ?: emptySet()

    val extraSafePorts: Set<Int>
        get() = prefs.getStringSet(KEY_EXTRA_SAFE_PORTS, null)
            ?.mapNotNull(String::toIntOrNull)
            ?.toSet()
            ?: emptySet()

    val ignoredPorts: Set<Int>
        get() = prefs.getStringSet(KEY_IGNORED_PORTS, null)
            ?.mapNotNull(String::toIntOrNull)
            ?.toSet()
            ?: emptySet()

    fun addExtraMatchPort(port: Int) {
        updatePortSet(KEY_EXTRA_MATCH_PORTS, port, add = true)
        updatePortSet(KEY_EXTRA_SAFE_PORTS, port, add = false)
        updatePortSet(KEY_IGNORED_PORTS, port, add = false)
    }

    fun addExtraSafePort(port: Int) {
        updatePortSet(KEY_EXTRA_SAFE_PORTS, port, add = true)
        updatePortSet(KEY_EXTRA_MATCH_PORTS, port, add = false)
        updatePortSet(KEY_IGNORED_PORTS, port, add = false)
    }

    fun ignorePort(port: Int) {
        updatePortSet(KEY_IGNORED_PORTS, port, add = true)
        updatePortSet(KEY_EXTRA_MATCH_PORTS, port, add = false)
        updatePortSet(KEY_EXTRA_SAFE_PORTS, port, add = false)
    }

    /** Порт уже показывали пользователю недавно (по умолчанию — в течение суток). */
    fun shouldAnnounceUnknownPort(port: Int, windowMs: Long = 24 * 60 * 60 * 1000L): Boolean {
        val now = System.currentTimeMillis()
        val raw = prefs.getString(KEY_UNKNOWN_PORT_SEEN, null)
        val seen = runCatching { JSONObject(raw ?: "{}") }.getOrDefault(JSONObject())
        val last = seen.optLong(port.toString(), 0L)
        if (now - last < windowMs) return false
        seen.put(port.toString(), now)
        val keys = seen.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (now - seen.optLong(key, 0L) > 7 * 24 * 60 * 60 * 1000L) keys.remove()
        }
        prefs.edit().putString(KEY_UNKNOWN_PORT_SEEN, seen.toString()).apply()
        return true
    }

    private fun updatePortSet(key: String, port: Int, add: Boolean) {
        val current = prefs.getStringSet(key, null)?.toMutableSet() ?: mutableSetOf()
        val value = port.toString()
        if (add) current.add(value) else current.remove(value)
        prefs.edit().putStringSet(key, current).apply()
    }

    var lastRunning: Boolean
        get() = prefs.getBoolean(KEY_LAST_RUNNING, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LAST_RUNNING, value).apply()
        }

    var discoveryUntil: Long
        get() = prefs.getLong(KEY_DISCOVERY_UNTIL, 0L)
        set(value) {
            prefs.edit().putLong(KEY_DISCOVERY_UNTIL, value).apply()
        }

    var enabledRegions: Set<String>
        get() {
            prefs.getStringSet(KEY_REGIONS, null)?.let { return it.toSet() }
            if (prefs.getBoolean(KEY_STABILIZATION_LEGACY, false)) {
                val migrated = setOf(RegionProfiles.ID_FAR_EAST)
                prefs.edit()
                    .putStringSet(KEY_REGIONS, migrated)
                    .remove(KEY_STABILIZATION_LEGACY)
                    .apply()
                return migrated
            }
            return emptySet()
        }
        set(value) {
            prefs.edit().putStringSet(KEY_REGIONS, value).apply()
        }

    var selectedProfile: String?
        get() {
            prefs.getString(KEY_SELECTED_PROFILE, null)?.let { saved ->
                return saved.takeIf { RegionProfiles.byId(it) != null }
            }
            val legacy = enabledRegions
            val migrated = listOf(
                RegionProfiles.ID_FAR_EAST,
                RegionProfiles.ID_MOSCOW,
                RegionProfiles.ID_NOVOSIBIRSK,
            ).firstOrNull { it in legacy } ?: return null
            prefs.edit().putString(KEY_SELECTED_PROFILE, migrated).apply()
            return migrated
        }
        set(value) {
            val valid = value?.takeIf { RegionProfiles.byId(it) != null }
            prefs.edit()
                .putString(KEY_SELECTED_PROFILE, valid)
                .putStringSet(KEY_REGIONS, valid?.let { setOf(it) } ?: emptySet())
                .apply()
        }

    fun selectProfile(profileId: String) {
        if (RegionProfiles.byId(profileId) == null) return
        selectedProfile = profileId
    }

    fun setRegionEnabled(profileId: String, enabled: Boolean) {
        if (enabled) {
            selectProfile(profileId)
        } else if (selectedProfile == profileId) {
            selectedProfile = null
        }
    }

    fun loadAllowRules(): List<AllowRule> {
        val raw = prefs.getString(KEY_ALLOW_RULES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    AllowRule.fromJson(item)?.let(::add)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAllowRules(rules: List<AllowRule>) {
        val array = JSONArray()
        rules.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ALLOW_RULES, array.toString()).apply()
    }

    fun activeAllowValues(now: Long = System.currentTimeMillis()): List<String> {
        val all = loadAllowRules()
        val active = all.filter { it.isActive(now) }
        if (active.size != all.size) saveAllowRules(active)
        return active.map { it.value }.distinct()
    }

    fun addAllowRule(value: String, note: String = "", ttlMs: Long = 0): Boolean {
        val normalized = Cidr.normalize(value) ?: return false
        val current = loadAllowRules().filterNot { it.value == normalized }.toMutableList()
        current.add(
            AllowRule(
                id = java.util.UUID.randomUUID().toString(),
                value = normalized,
                note = note.trim(),
                expiresAt = if (ttlMs > 0) System.currentTimeMillis() + ttlMs else 0,
            ),
        )
        saveAllowRules(current)
        return true
    }

    fun removeAllowRule(id: String) {
        saveAllowRules(loadAllowRules().filterNot { it.id == id })
    }

    fun clearAllowRules() {
        saveAllowRules(emptyList())
    }

    fun loadRules(): List<BlockRule> {
        val raw = prefs.getString(KEY_RULES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    BlockRule.fromJson(item)?.let(::add)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRules(rules: List<BlockRule>) {
        val array = JSONArray()
        rules.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_RULES, array.toString()).apply()
    }

    fun activeValues(): List<String> {
        return loadRules().filter { it.enabled }.map { it.value }.distinct()
    }

    fun addRules(values: List<String>, note: String = ""): Int {
        return addRulesDetailed(values.map { it to note })
    }

    fun addRulesDetailed(values: List<Pair<String, String>>): Int {
        val current = loadRules().toMutableList()
        val existing = current.map { it.value }.toMutableSet()
        var added = 0
        for ((value, note) in values) {
            val normalized = Cidr.normalize(value) ?: continue
            if (!existing.add(normalized)) continue
            current.add(
                BlockRule(
                    id = java.util.UUID.randomUUID().toString(),
                    value = normalized,
                    note = note.trim(),
                ),
            )
            added++
        }
        if (added > 0) saveRules(current)
        return added
    }

    fun removeRule(id: String) {
        val current = loadRules()
        saveRules(current.filterNot { it.id == id })
    }

    fun setEnabled(id: String, enabled: Boolean) {
        saveRules(loadRules().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun clearRules() {
        saveRules(emptyList())
    }

    fun exportText(): String = loadRules().joinToString("\n") { rule ->
        if (rule.note.isBlank()) rule.value else "${rule.value} # ${rule.note}"
    }

    companion object {
        private const val PREFS = "mlbb_blocker"
        private const val KEY_RULES = "rules"
        private const val KEY_DISCOVERY = "discovery_mode"
        private const val KEY_FAST_REJECT = "fast_reject"
        private const val KEY_DISCOVERY_UNTIL = "discovery_until"
        private const val KEY_LOG_BLOCKED = "log_blocked_attempts"
        private const val KEY_ALLOW_RULES = "allow_rules"
        private const val KEY_REGIONS = "enabled_regions"
        private const val KEY_SELECTED_PROFILE = "selected_profile"
        private const val KEY_MATCH_ALERTS = "match_alerts"
        private const val KEY_HIDE_STARTUP_ALERTS = "hide_startup_alerts"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_LAST_RUNNING = "last_running"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ALLOW_STARTUP_SERVER = "allow_startup_server"
        private const val KEY_UNKNOWN_PORT_ALERTS = "unknown_port_alerts"
        private const val KEY_EXTRA_MATCH_PORTS = "extra_match_ports"
        private const val KEY_EXTRA_SAFE_PORTS = "extra_safe_ports"
        private const val KEY_IGNORED_PORTS = "ignored_ports"
        private const val KEY_UNKNOWN_PORT_SEEN = "unknown_port_seen"
        private const val KEY_STABILIZATION_LEGACY = "ping_stabilization"
    }
}
