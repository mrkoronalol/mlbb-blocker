package ru.mlbb.blocker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ping_samples",
    indices = [Index("endpointKey"), Index("timestamp")],
)
data class PingSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val endpointKey: String,
    val timestamp: Long,
    val ms: Long?,
    val jitterMs: Long?,
    val kind: String,
    val lossPercent: Int? = null,
)
