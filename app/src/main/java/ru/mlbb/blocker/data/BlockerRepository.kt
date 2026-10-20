package ru.mlbb.blocker.data

import ru.mlbb.blocker.BuildConfig

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface BlockerStatus {
    data object Stopped : BlockerStatus
    data object Starting : BlockerStatus
    data object Running : BlockerStatus
    data class Error(val message: String) : BlockerStatus
}

object BlockerRepository {

    private val _status = MutableStateFlow<BlockerStatus>(BlockerStatus.Stopped)
    val status: StateFlow<BlockerStatus> = _status.asStateFlow()

    private val _connections = MutableStateFlow<List<ConnectionStat>>(emptyList())
    val connections: StateFlow<List<ConnectionStat>> = _connections.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val stats = LinkedHashMap<String, ConnectionStat>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun setStatus(status: BlockerStatus) {
        _status.value = status
    }

    fun log(message: String) {
        val line = "${timeFormat.format(Date())}  $message"
        if (BuildConfig.DEBUG) android.util.Log.d("MLBBBlocker", message)
        _logs.update { current -> (current + line).takeLast(300) }
    }

    @Synchronized
    fun mergeConnections(items: List<ConnectionSnapshot>) {
        val now = System.currentTimeMillis()
        for (item in items) {
            val key = "${item.network}://${item.ip}:${item.port}"
            val existing = stats[key]
            stats[key] = if (existing == null) {
                ConnectionStat(
                    ip = item.ip,
                    port = item.port,
                    host = item.host,
                    network = item.network,
                    firstSeen = now,
                    lastSeen = now,
                    observations = 1,
                )
            } else {
                existing.copy(
                    host = item.host?.takeIf { it.isNotBlank() } ?: existing.host,
                    lastSeen = now,
                    observations = existing.observations + 1,
                )
            }
        }
        if (stats.size > 300) {
            val keep = stats.values.sortedByDescending { it.lastSeen }.take(300).map { it.key }.toSet()
            stats.keys.filterNot { it in keep }.forEach(stats::remove)
        }
        _connections.value = stats.values.sortedByDescending { it.lastSeen }
    }

    @Synchronized
    fun clearConnections() {
        stats.clear()
        _connections.value = emptyList()
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun resetSession() {
        _status.value = BlockerStatus.Stopped
        clearConnections()
    }
}
