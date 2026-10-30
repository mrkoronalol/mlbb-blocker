package ru.mlbb.blocker.data.analytics

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ru.mlbb.blocker.BuildConfig
import ru.mlbb.blocker.data.meta.MetaRepository
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Локальный буфер аналитики: события и игровые серверы копятся в Room и
 * отправляются батчем. При отсутствии сети данные не теряются.
 */
object Analytics {

    private const val PREFS = "mlbb_analytics"
    private const val KEY_DEVICE_ID = "device_id"
    private const val MAX_EVENTS_BATCH = 200
    private const val MAX_IPS_BATCH = 200
    private const val MAX_EVENTS_KEPT = 2000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var dao: AnalyticsDao? = null

    @Volatile
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        if (dao != null) return
        val app = context.applicationContext
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val database = AnalyticsDatabase.get(app)
        dao = database.dao()
        scope.launch {
            runCatching {
                database.dao().trimEvents(MAX_EVENTS_KEPT)
                database.dao().pruneSent(
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7),
                )
            }
        }
    }

    fun track(name: String, properties: Map<String, Any?> = emptyMap()) {
        val database = dao ?: return
        val json = JSONObject()
        properties.forEach { (key, value) -> json.put(key, value ?: JSONObject.NULL) }
        scope.launch {
            runCatching {
                database.insertEvent(
                    AnalyticsEventEntity(
                        name = name,
                        properties = json.toString(),
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    fun recordServerIp(
        ip: String,
        port: Int,
        protocol: String,
        city: String? = null,
        country: String? = null,
        pingMs: Long? = null,
    ) {
        val database = dao ?: return
        val normalized = protocol.lowercase().ifBlank { "tcp" }
        scope.launch {
            runCatching {
                database.upsertIp(
                    TelemetryIpEntity(
                        key = "$ip|$port|$normalized",
                        ip = ip,
                        port = port,
                        protocol = normalized,
                        city = city,
                        country = country,
                        pingMs = pingMs,
                        seenAt = System.currentTimeMillis(),
                        pending = true,
                    ),
                )
            }
        }
    }

    /** Отправляет накопленные события и IP. Возвращает false, если сеть недоступна. */
    suspend fun flush(context: Context): Boolean = withContext(Dispatchers.IO) {
        val database = dao ?: return@withContext false
        val base = MetaRepository.apiBase(context).trimEnd('/')
        if (base.isBlank()) return@withContext false
        val api = TelemetryApi(base)
        var success = true

        val events = database.events(MAX_EVENTS_BATCH)
        if (events.isNotEmpty()) {
            val sent = api.sendEvents(deviceId(context), BuildConfig.VERSION_NAME, events)
            if (sent) {
                database.deleteEvents(events.map { it.id })
            } else {
                success = false
            }
        }

        val ips = database.pendingIps(MAX_IPS_BATCH)
        if (ips.isNotEmpty()) {
            val sent = api.sendIps(ips)
            if (sent) {
                database.markIpsSent(ips.map { it.key })
            } else {
                success = false
            }
        }

        success
    }

    fun deviceId(context: Context): String {
        initialize(context)
        val store = prefs ?: return "unknown"
        store.getString(KEY_DEVICE_ID, null)?.let { return it }
        val generated = UUID.randomUUID().toString()
        store.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }
}
