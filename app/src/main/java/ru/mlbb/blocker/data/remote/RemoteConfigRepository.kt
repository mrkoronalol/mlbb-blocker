package ru.mlbb.blocker.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ru.mlbb.blocker.BuildConfig
import ru.mlbb.blocker.data.RegionProfile
import ru.mlbb.blocker.data.meta.MetaRepository
import java.net.HttpURLConnection
import java.net.URL

/**
 * Удалённая конфигурация приложения: maintenance, фичефлаги, уведомление,
 * минимальная версия и профили блокировки. Последний успешный ответ хранится
 * в SharedPreferences и используется при отсутствии сети.
 */
object RemoteConfigRepository {

    private const val PREFS = "mlbb_remote_config"
    private const val KEY_CONFIG = "config_json"
    private const val TIMEOUT_MS = 10_000

    @Volatile
    private var prefs: SharedPreferences? = null

    private val _state = MutableStateFlow(RemoteAppConfig.DEFAULT)
    val state: StateFlow<RemoteAppConfig> = _state.asStateFlow()

    private val _syncedAt = MutableStateFlow(0L)
    val syncedAt: StateFlow<Long> = _syncedAt.asStateFlow()

    @Volatile
    var profiles: List<RegionProfile> = emptyList()
        private set

    fun initialize(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val store = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = store
        val cached = store.getString(KEY_CONFIG, null) ?: return
        val parsed = RemoteAppConfig.parse(cached) ?: return
        apply(withFreshVersionCheck(parsed))
    }

    suspend fun refresh(context: Context): Boolean = withContext(Dispatchers.IO) {
        val base = MetaRepository.apiBase(context).trimEnd('/')
        if (base.isBlank()) return@withContext false
        val url = "$base/api/v1/config?version=${BuildConfig.VERSION_NAME}"
        val body = try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MLBB-Blocker/${BuildConfig.VERSION_NAME}")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext false
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                runCatching { connection.disconnect() }
            }
        } catch (_: Throwable) {
            return@withContext false
        }

        val parsed = RemoteAppConfig.parse(body) ?: return@withContext false
        val config = withFreshVersionCheck(parsed)
        prefs?.edit()?.putString(KEY_CONFIG, config.toJson())?.apply()
        apply(config)
        true
    }

    /** Локальная страховка: даже если сервер забыл update_required, версия важнее. */
    private fun withFreshVersionCheck(config: RemoteAppConfig): RemoteAppConfig {
        val outdated = VersionCompare.isLower(BuildConfig.VERSION_NAME, config.minAppVersion)
        if (outdated == config.updateRequired) return config
        return config.copy(updateRequired = outdated)
    }

    private fun apply(config: RemoteAppConfig) {
        _state.value = config
        profiles = config.regionProfiles()
        _syncedAt.value = config.fetchedAt
    }

    fun cachedProfiles(): List<RegionProfile> = profiles

    fun apiBase(context: Context): String = MetaRepository.apiBase(context)
}
