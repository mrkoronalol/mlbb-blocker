package ru.mlbb.blocker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_attempts",
    indices = [Index("timestamp"), Index("ip")],
)
data class BlockedAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val ip: String,
    val port: Int,
    val network: String,
    val matchedProfile: String?,
)
