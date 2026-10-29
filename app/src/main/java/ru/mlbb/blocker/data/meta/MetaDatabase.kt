package ru.mlbb.blocker.data.meta

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "meta_heroes")
data class MetaHeroEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameRu: String,
    val role: String,
    val lane: String,
    val lanes: String,
    val imageUrl: String?,
    val winRate: Double?,
    val pickRate: Double?,
    val banRate: Double?,
    val matches: Int?,
    val tier: String,
    val sortOrder: Int,
)

@Entity(tableName = "meta_state")
data class MetaStateEntity(
    @PrimaryKey val id: Int = 1,
    val region: String,
    val regional: Boolean,
    val patch: String?,
    val updatedAt: String?,
    val fetchedAt: Long,
)

@Dao
interface MetaDao {
    @Query("SELECT * FROM meta_heroes ORDER BY sortOrder")
    suspend fun heroes(): List<MetaHeroEntity>

    @Query("SELECT * FROM meta_state WHERE id = 1")
    suspend fun state(): MetaStateEntity?

    @Upsert
    suspend fun upsertHeroes(heroes: List<MetaHeroEntity>)

    @Upsert
    suspend fun upsertState(state: MetaStateEntity)

    @Query("DELETE FROM meta_heroes")
    suspend fun clearHeroes()
}

@Database(
    entities = [MetaHeroEntity::class, MetaStateEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MetaDatabase : RoomDatabase() {
    abstract fun metaDao(): MetaDao

    companion object {
        @Volatile
        private var instance: MetaDatabase? = null

        fun get(context: Context): MetaDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MetaDatabase::class.java,
                    "mlbb-meta.db",
                ).build().also { instance = it }
            }
        }
    }
}
