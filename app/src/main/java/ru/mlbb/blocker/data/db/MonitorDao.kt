package ru.mlbb.blocker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitorDao {

    @Query("SELECT * FROM endpoints ORDER BY lastSeen DESC LIMIT :limit")
    fun observeEndpoints(limit: Int = 1000): Flow<List<EndpointEntity>>

    @Query("SELECT * FROM endpoints WHERE network = :network AND ip = :ip AND port = :port LIMIT 1")
    suspend fun findEndpoint(network: String, ip: String, port: Int): EndpointEntity?

    @Upsert
    suspend fun upsertEndpoint(endpoint: EndpointEntity)

    @Upsert
    suspend fun upsertEndpoints(endpoints: List<EndpointEntity>)

    @Query(
        "UPDATE endpoints SET pingMs = :ms, pingJitterMs = :jitter, pingKind = :kind, pingAt = :at " +
            "WHERE network = :network AND ip = :ip AND port = :port",
    )
    suspend fun updatePing(
        network: String,
        ip: String,
        port: Int,
        ms: Long?,
        jitter: Long?,
        kind: String,
        at: Long,
    )

    @Query(
        "UPDATE endpoints SET blockedAttempts = blockedAttempts + 1 " +
            "WHERE network = :network AND ip = :ip AND port = :port",
    )
    suspend fun incrementBlocked(network: String, ip: String, port: Int): Int

    @Insert
    suspend fun insertBlockedAttempt(attempt: BlockedAttemptEntity): Long

    @Query("SELECT * FROM endpoints WHERE geoAt = 0 LIMIT :limit")
    suspend fun endpointsWithoutGeo(limit: Int = 500): List<EndpointEntity>

    @Query(
        "UPDATE endpoints SET city = :city, country = :country, geoAt = :at " +
            "WHERE network = :network AND ip = :ip AND port = :port",
    )
    suspend fun updateGeo(
        network: String,
        ip: String,
        port: Int,
        city: String?,
        country: String?,
        at: Long,
    )

    @Query("SELECT * FROM blocked_attempts ORDER BY timestamp DESC LIMIT :limit")
    fun observeBlockedAttempts(limit: Int = 300): Flow<List<BlockedAttemptEntity>>

    @Insert
    suspend fun insertPingSample(sample: PingSampleEntity): Long

    @Query("SELECT * FROM ping_samples WHERE endpointKey = :key ORDER BY timestamp DESC LIMIT :limit")
    fun observePingSamples(key: String, limit: Int = 50): Flow<List<PingSampleEntity>>

    @Insert
    suspend fun insertMatchSession(session: MatchSessionEntity): Long

    @Query(
        "UPDATE match_sessions SET endedAt = :endedAt, result = :result, matchIp = :matchIp, city = :city " +
            "WHERE id = :id",
    )
    suspend fun finishMatchSession(
        id: Long,
        endedAt: Long,
        result: String?,
        matchIp: String?,
        city: String?,
    )

    @Query("SELECT * FROM match_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeMatchSessions(limit: Int = 50): Flow<List<MatchSessionEntity>>

    @Query("DELETE FROM endpoints")
    suspend fun clearEndpoints()

    @Query("DELETE FROM blocked_attempts")
    suspend fun clearBlockedAttempts()

    @Query("DELETE FROM ping_samples")
    suspend fun clearPingSamples()

    @Query("DELETE FROM match_sessions")
    suspend fun clearMatchSessions()

    @Query("DELETE FROM endpoints WHERE lastSeen < :cutoff")
    suspend fun deleteEndpointsOlderThan(cutoff: Long)

    @Query("DELETE FROM ping_samples WHERE timestamp < :cutoff")
    suspend fun deletePingSamplesOlderThan(cutoff: Long)

    @Query("DELETE FROM blocked_attempts WHERE timestamp < :cutoff")
    suspend fun deleteBlockedAttemptsOlderThan(cutoff: Long)
}
