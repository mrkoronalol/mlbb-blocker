package ru.mlbb.blocker.data.analytics

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val properties: String,
    val createdAt: Long,
)

@Entity(tableName = "telemetry_ips")
data class TelemetryIpEntity(
    @PrimaryKey val key: String,
    val ip: String,
    val port: Int,
    val protocol: String,
    val city: String?,
    val country: String?,
    val pingMs: Long?,
    val seenAt: Long,
    val pending: Boolean = true,
)

@Dao
interface AnalyticsDao {
    @Insert
    suspend fun insertEvent(event: AnalyticsEventEntity): Long

    @Query("SELECT * FROM analytics_events ORDER BY id ASC LIMIT :limit")
    suspend fun events(limit: Int): List<AnalyticsEventEntity>

    @Query("DELETE FROM analytics_events WHERE id IN (:ids)")
    suspend fun deleteEvents(ids: List<Long>)

    @Query(
        "DELETE FROM analytics_events WHERE id NOT IN " +
            "(SELECT id FROM analytics_events ORDER BY id DESC LIMIT :keep)",
    )
    suspend fun trimEvents(keep: Int)

    @Upsert
    suspend fun upsertIp(entity: TelemetryIpEntity)

    @Query("SELECT * FROM telemetry_ips WHERE pending = 1 ORDER BY seenAt DESC LIMIT :limit")
    suspend fun pendingIps(limit: Int): List<TelemetryIpEntity>

    @Query("UPDATE telemetry_ips SET pending = 0 WHERE key IN (:keys)")
    suspend fun markIpsSent(keys: List<String>)

    @Query("DELETE FROM telemetry_ips WHERE pending = 0 AND seenAt < :cutoff")
    suspend fun pruneSent(cutoff: Long)
}

@Database(
    entities = [AnalyticsEventEntity::class, TelemetryIpEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AnalyticsDatabase : RoomDatabase() {
    abstract fun dao(): AnalyticsDao

    companion object {
        @Volatile
        private var instance: AnalyticsDatabase? = null

        fun get(context: Context): AnalyticsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnalyticsDatabase::class.java,
                    "mlbb-analytics.db",
                ).build().also { instance = it }
            }
        }
    }
}
