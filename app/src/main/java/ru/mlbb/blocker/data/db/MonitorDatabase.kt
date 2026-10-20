package ru.mlbb.blocker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        EndpointEntity::class,
        BlockedAttemptEntity::class,
        PingSampleEntity::class,
        MatchSessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MonitorDatabase : RoomDatabase() {
    abstract fun monitorDao(): MonitorDao

    companion object {
        @Volatile
        private var instance: MonitorDatabase? = null

        fun get(context: Context): MonitorDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MonitorDatabase::class.java,
                    "mlbb-monitor.db",
                ).build().also { instance = it }
            }
        }
    }
}
