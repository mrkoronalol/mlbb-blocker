package ru.mlbb.blocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_sessions")
data class MatchSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val profiles: String,
    val gamePackage: String,
    val matchIp: String? = null,
    val city: String? = null,
    val result: String? = null,
)
