package ru.mlbb.blocker.data.rules

import org.json.JSONArray
import org.json.JSONObject
import ru.mlbb.blocker.data.Cidr
import ru.mlbb.blocker.data.KnownServer
import ru.mlbb.blocker.data.KnownServerKind
import ru.mlbb.blocker.data.RegionProfile

sealed interface RulesParseResult {
    data class Success(val config: RulesConfig) : RulesParseResult
    data class Failure(val errors: List<String>) : RulesParseResult
}

/**
 * Разбор и проверка rules.json. Ошибки не бросаются наружу: при любой проблеме
 * правило не применяется и используется предыдущий рабочий набор.
 */
object RulesParser {

    private val PROFILE_ID = Regex("^[a-z0-9_]{1,32}$")

    fun parse(raw: String): RulesParseResult {
        val json = try {
            JSONObject(raw)
        } catch (t: Throwable) {
            return RulesParseResult.Failure(listOf("JSON: ${t.message ?: t.javaClass.simpleName}"))
        }

        val errors = mutableListOf<String>()
        val schemaVersion = json.optInt("schemaVersion", 0)
        if (schemaVersion !in 1..RulesConfig.SCHEMA_VERSION) {
            errors += "schemaVersion=$schemaVersion не поддерживается"
        }

        val matchPorts = intList(json.optJSONArray("matchPorts"), "matchPorts", errors)
        if (matchPorts.isEmpty()) errors += "matchPorts пуст"

        val matchPortRanges = stringList(json.optJSONArray("matchPortRanges"), "matchPortRanges", errors)
            .filter { range ->
                val expanded = RulesConfig.expandRange(range)
                if (expanded.isEmpty()) {
                    errors += "диапазон портов '$range' некорректен"
                    false
                } else {
                    true
                }
            }

        val safePorts = intList(json.optJSONArray("safePorts"), "safePorts", errors)

        val startup = mutableListOf<StartupEndpoint>()
        json.optJSONArray("startup")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                if (item == null) {
                    errors += "startup[$index] не объект"
                    continue
                }
                val ip = item.optString("ip").trim()
                val port = item.optInt("port", 0)
                if (Cidr.normalize(ip) == null) {
                    errors += "startup[$index].ip='$ip' некорректен"
                    continue
                }
                if (port !in 1..65535) {
                    errors += "startup[$index].port=$port некорректен"
                    continue
                }
                startup += StartupEndpoint(ip, port, item.optString("label"))
            }
        }

        val profiles = mutableListOf<RegionProfile>()
        val profileArray = json.optJSONArray("profiles")
        if (profileArray == null || profileArray.length() == 0) {
            errors += "profiles пуст"
        } else {
            val seen = mutableSetOf<String>()
            for (index in 0 until profileArray.length()) {
                val item = profileArray.optJSONObject(index)
                if (item == null) {
                    errors += "profiles[$index] не объект"
                    continue
                }
                val id = item.optString("id").trim()
                if (!PROFILE_ID.matches(id)) {
                    errors += "profiles[$index].id='$id' некорректен"
                    continue
                }
                if (!seen.add(id)) {
                    errors += "profiles: повторяется id '$id'"
                    continue
                }
                val rules = parseRules(item.optJSONArray("rules"), "profiles[$id].rules", errors)
                val allowed = stringList(
                    item.optJSONArray("allowedCidrs"),
                    "profiles[$id].allowedCidrs",
                    errors,
                ).filter { cidr ->
                    if (Cidr.normalize(cidr) == null) {
                        errors += "profiles[$id]: CIDR '$cidr' некорректен"
                        false
                    } else {
                        true
                    }
                }
                profiles += RegionProfile(
                    id = id,
                    title = item.optString("title").ifBlank { id },
                    badge = item.optString("badge").ifBlank { id.take(3).uppercase() },
                    description = item.optString("description"),
                    rules = rules,
                    allowedCidrs = allowed,
                    blockOtherMatchServers = item.optBoolean("blockOtherMatchServers", false),
                )
            }
        }

        val knownExact = mutableMapOf<String, KnownServer>()
        val knownSubnets = mutableMapOf<String, KnownServer>()
        json.optJSONObject("knownServers")?.let { known ->
            known.optJSONArray("exact")?.let { array ->
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val ip = item.optString("ip").trim()
                    if (!Cidr.isIpv4(ip) && !ip.contains(':')) {
                        errors += "knownServers.exact[$index].ip='$ip' некорректен"
                        continue
                    }
                    knownExact[ip] = knownServer(item, errors, "knownServers.exact[$index]") ?: continue
                }
            }
            known.optJSONArray("subnets")?.let { array ->
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val prefix = item.optString("prefix").trim()
                    val parts = prefix.split('.')
                    val valid = parts.size == 3 && parts.all { part ->
                        part.isNotEmpty() && part.toIntOrNull() in 0..255
                    }
                    if (!valid) {
                        errors += "knownServers.subnets[$index].prefix='$prefix' некорректен"
                        continue
                    }
                    knownSubnets[prefix] = knownServer(item, errors, "knownServers.subnets[$index]") ?: continue
                }
            }
        }

        val conflict = safePorts.firstOrNull { it in matchPorts }
        if (conflict != null) {
            errors += "порт $conflict одновременно матч-порт и сервисный"
        }

        if (errors.isNotEmpty()) return RulesParseResult.Failure(errors.distinct())

        return RulesParseResult.Success(
            RulesConfig(
                schemaVersion = schemaVersion,
                updatedAt = json.optString("updatedAt").ifBlank { "unknown" },
                matchPorts = matchPorts.distinct(),
                matchPortRanges = matchPortRanges.distinct(),
                safePorts = safePorts.distinct(),
                startup = startup,
                profiles = profiles,
                knownExact = knownExact,
                knownSubnets = knownSubnets,
            ),
        )
    }

    private fun knownServer(item: JSONObject, errors: MutableList<String>, path: String): KnownServer? {
        val city = item.optString("city").trim()
        if (city.isBlank()) {
            errors += "$path.city пуст"
            return null
        }
        val kind = runCatching {
            KnownServerKind.valueOf(item.optString("kind").uppercase())
        }.getOrNull() ?: run {
            errors += "$path.kind='${item.optString("kind")}' неизвестен"
            return null
        }
        val latency = item.optString("latency").takeIf { it.isNotBlank() }
        return KnownServer(city, latency, kind)
    }

    private fun parseRules(array: JSONArray?, path: String, errors: MutableList<String>): List<Pair<String, String>> {
        if (array == null) return emptyList()
        val result = mutableListOf<Pair<String, String>>()
        for (index in 0 until array.length()) {
            val value: String
            val note: String
            when (val item = array.opt(index)) {
                is JSONArray -> {
                    value = item.optString(0)
                    note = item.optString(1)
                }

                is JSONObject -> {
                    value = item.optString("cidr")
                    note = item.optString("note")
                }

                is String -> {
                    value = item
                    note = ""
                }

                else -> {
                    errors += "$path[$index] некорректен"
                    continue
                }
            }
            val normalized = Cidr.normalize(value)
            if (normalized == null) {
                errors += "$path[$index] CIDR '$value' некорректен"
                continue
            }
            result += normalized to note.trim()
        }
        return result
    }

    private fun intList(array: JSONArray?, name: String, errors: MutableList<String>): List<Int> {
        if (array == null) return emptyList()
        val result = mutableListOf<Int>()
        for (index in 0 until array.length()) {
            val port = array.optInt(index, -1)
            if (port !in 1..65535) {
                errors += "$name[$index]=$port некорректен"
                continue
            }
            result += port
        }
        return result
    }

    private fun stringList(array: JSONArray?, name: String, errors: MutableList<String>): List<String> {
        if (array == null) return emptyList()
        val result = mutableListOf<String>()
        for (index in 0 until array.length()) {
            val value = array.optString(index).trim()
            if (value.isEmpty()) {
                errors += "$name[$index] пуст"
                continue
            }
            result += value
        }
        return result
    }
}
