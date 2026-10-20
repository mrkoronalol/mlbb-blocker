package ru.mlbb.blocker.data.db

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import ru.mlbb.blocker.data.Cidr
import ru.mlbb.blocker.data.ConnectionStat
import ru.mlbb.blocker.data.geo.GeoInfo
import ru.mlbb.blocker.data.PingResult
import java.util.concurrent.TimeUnit

object MonitorRepository {

    private const val LEGACY_PREFS = "mlbb_monitor"
    private const val LEGACY_ENDPOINTS = "endpoints"
    private const val LEGACY_MIGRATED = "room_migrated"

    private lateinit var dao: MonitorDao
    private lateinit var scope: CoroutineScope
    private val queue = Channel<Record>(Channel.UNLIMITED)
    private var initialized = false

    private data class Record(
        val ip: String,
        val port: Int,
        val network: String,
        val host: String?,
        val timestamp: Long,
    )

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        dao = MonitorDatabase.get(context).monitorDao()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            migrateLegacy(context)
            applyRetention()
            writerLoop()
        }
        initialized = true
    }

    fun observeEndpoints(): Flow<List<ConnectionStat>> {
        return dao.observeEndpoints().map { list -> list.map(EndpointEntity::toConnectionStat) }
    }

    fun observeBlockedAttempts(): Flow<List<BlockedAttemptEntity>> {
        return dao.observeBlockedAttempts()
    }

    fun observeMatchSessions(): Flow<List<MatchSessionEntity>> {
        return dao.observeMatchSessions()
    }

    fun record(ip: String, port: Int, network: String, host: String?, timestamp: Long = System.currentTimeMillis()) {
        if (!initialized) return
        if (ip.isBlank() || port <= 0) return
        if (Cidr.isPrivate(ip) || port == 53) return
        val normalizedNetwork = network.lowercase().ifBlank { "tcp" }
        queue.trySend(Record(ip, port, normalizedNetwork, host?.takeIf { it.isNotBlank() }, timestamp))
    }

    fun updatePing(connection: ConnectionStat, result: PingResult, lossPercent: Int? = null) {
        if (!initialized) return
        scope.launch {
            val now = System.currentTimeMillis()
            dao.updatePing(
                network = connection.network,
                ip = connection.ip,
                port = connection.port,
                ms = result.ms,
                jitter = result.jitterMs,
                kind = result.kind,
                at = now,
            )
            dao.insertPingSample(
                PingSampleEntity(
                    endpointKey = connection.key,
                    timestamp = now,
                    ms = result.ms,
                    jitterMs = result.jitterMs,
                    kind = result.kind,
                    lossPercent = lossPercent,
                ),
            )
        }
    }

    fun recordBlockedAttempt(ip: String, port: Int, network: String, profile: String?) {
        if (!initialized) return
        val normalizedNetwork = network.lowercase().ifBlank { "tcp" }
        scope.launch {
            val now = System.currentTimeMillis()
            val updated = dao.incrementBlocked(normalizedNetwork, ip, port)
            if (updated == 0) {
                dao.upsertEndpoint(
                    EndpointEntity(
                        network = normalizedNetwork,
                        ip = ip,
                        port = port,
                        host = null,
                        firstSeen = now,
                        lastSeen = now,
                        sessions = 0,
                        pingMs = null,
                        pingJitterMs = null,
                        pingKind = null,
                        pingAt = 0,
                        blockedAttempts = 1,
                    ),
                )
            }
            dao.insertBlockedAttempt(
                BlockedAttemptEntity(
                    timestamp = now,
                    ip = ip,
                    port = port,
                    network = normalizedNetwork,
                    matchedProfile = profile,
                ),
            )
        }
    }

    suspend fun startMatchSession(gamePackage: String, profiles: Set<String>): Long {
        if (!initialized) return -1
        return try {
            dao.insertMatchSession(
                MatchSessionEntity(
                    startedAt = System.currentTimeMillis(),
                    profiles = profiles.joinToString(","),
                    gamePackage = gamePackage,
                ),
            )
        } catch (_: Exception) {
            -1
        }
    }

    fun finishMatchSession(id: Long, result: String, matchIp: String? = null, city: String? = null) {
        if (!initialized || id <= 0) return
        scope.launch {
            dao.finishMatchSession(
                id = id,
                endedAt = System.currentTimeMillis(),
                result = result,
                matchIp = matchIp,
                city = city,
            )
        }
    }

    suspend fun enrichGeoInfo(lookup: (String) -> GeoInfo?) {
        if (!initialized) return
        val pending = dao.endpointsWithoutGeo(500)
        for (endpoint in pending) {
            val geo = lookup(endpoint.ip) ?: continue
            dao.updateGeo(
                network = endpoint.network,
                ip = endpoint.ip,
                port = endpoint.port,
                city = geo.city,
                country = geo.country,
                at = System.currentTimeMillis(),
            )
        }
    }

    fun clearMonitoring() {
        if (!initialized) return
        scope.launch {
            dao.clearEndpoints()
            dao.clearBlockedAttempts()
            dao.clearPingSamples()
        }
    }

    fun clearMatchSessions() {
        if (!initialized) return
        scope.launch { dao.clearMatchSessions() }
    }

    private suspend fun writerLoop() {
        val batch = mutableListOf<Record>()
        while (true) {
            val first = queue.receive()
            batch.add(first)
            delay(400)
            while (batch.size < 100) {
                val next = queue.tryReceive().getOrNull() ?: break
                batch.add(next)
            }
            try {
                applyBatch(batch)
            } catch (_: Exception) {
                // keep monitoring alive even if one batch fails
            }
            batch.clear()
        }
    }

    private suspend fun applyBatch(batch: List<Record>) {
        val grouped = batch.groupBy { Triple(it.network, it.ip, it.port) }
        for ((key, records) in grouped) {
            val network = key.first
            val ip = key.second
            val port = key.third
            val existing = dao.findEndpoint(network, ip, port)
            val firstSeen = minOf(records.minOf { it.timestamp }, existing?.firstSeen ?: Long.MAX_VALUE)
            val lastSeen = maxOf(records.maxOf { it.timestamp }, existing?.lastSeen ?: 0L)
            val host = records.lastOrNull { !it.host.isNullOrBlank() }?.host ?: existing?.host
            dao.upsertEndpoint(
                EndpointEntity(
                    network = network,
                    ip = ip,
                    port = port,
                    host = host,
                    firstSeen = firstSeen,
                    lastSeen = lastSeen,
                    sessions = (existing?.sessions ?: 0) + records.size,
                    pingMs = existing?.pingMs,
                    pingJitterMs = existing?.pingJitterMs,
                    pingKind = existing?.pingKind,
                    pingAt = existing?.pingAt ?: 0,
                    blockedAttempts = existing?.blockedAttempts ?: 0,
                    city = existing?.city,
                    country = existing?.country,
                    geoAt = existing?.geoAt ?: 0,
                ),
            )
        }
    }

    private suspend fun applyRetention() {
        val now = System.currentTimeMillis()
        dao.deleteEndpointsOlderThan(now - TimeUnit.DAYS.toMillis(30))
        dao.deletePingSamplesOlderThan(now - TimeUnit.DAYS.toMillis(7))
        dao.deleteBlockedAttemptsOlderThan(now - TimeUnit.DAYS.toMillis(30))
    }

    private suspend fun migrateLegacy(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(LEGACY_MIGRATED, false)) return
        val raw = prefs.getString(LEGACY_ENDPOINTS, null)
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                val migrated = mutableListOf<EndpointEntity>()
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val ip = item.optString("ip")
                    val port = item.optInt("port", 0)
                    if (ip.isBlank() || port <= 0) continue
                    migrated.add(
                        EndpointEntity(
                            network = item.optString("network", "tcp"),
                            ip = ip,
                            port = port,
                            host = item.optString("host").takeIf { it.isNotBlank() },
                            firstSeen = item.optLong("firstSeen", System.currentTimeMillis()),
                            lastSeen = item.optLong("lastSeen", System.currentTimeMillis()),
                            sessions = item.optInt("observations", 1),
                            pingMs = item.optLong("pingMs", -1).takeIf { it >= 0 },
                            pingJitterMs = item.optLong("pingJitterMs", -1).takeIf { it >= 0 },
                            pingKind = item.optString("pingKind").takeIf { it.isNotBlank() },
                            pingAt = item.optLong("pingAt", 0),
                        ),
                    )
                }
                if (migrated.isNotEmpty()) {
                    dao.upsertEndpoints(migrated)
                }
            } catch (_: Exception) {
                // leave legacy data in place on failure
            }
        }
        prefs.edit()
            .putBoolean(LEGACY_MIGRATED, true)
            .remove(LEGACY_ENDPOINTS)
            .apply()
    }
}
