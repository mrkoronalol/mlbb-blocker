package ru.mlbb.blocker.data.rules

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.data.PortOverrides
import java.io.File

enum class RulesSource {
    BUILT_IN,
    ASSET,
    CACHED,
    REMOTE,
}

data class RulesStatus(
    val source: RulesSource = RulesSource.BUILT_IN,
    val updatedAt: String = "built-in",
    val lastCheck: Long = 0L,
    val lastError: String? = null,
    val checking: Boolean = false,
)

sealed interface RulesRefreshResult {
    data class Updated(val updatedAt: String) : RulesRefreshResult
    data object UpToDate : RulesRefreshResult
    data class Rejected(val reason: String) : RulesRefreshResult
    data class NetworkError(val reason: String) : RulesRefreshResult
}

/**
 * Источник правил с приоритетом:
 * проверенные правила с сервера → последний рабочий кэш → assets → встроенные значения.
 */
object RuleConfigRepository {

    private const val ASSET_PATH = "rules/rules.json"
    private const val RULES_DIR = "rules"
    private const val RULES_FILE = "rules.json"
    private const val SIGNATURE_FILE = "rules.json.sig"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var initialized = false

    @Volatile
    var current: RulesConfig = BuiltInRules.config
        private set

    private val _status = MutableStateFlow(RulesStatus())
    val status: StateFlow<RulesStatus> = _status.asStateFlow()

    /** true, если не удалось прочитать даже встроенный набор. */
    val isFallback: Boolean
        get() = _status.value.source == RulesSource.BUILT_IN && _status.value.lastError != null

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        PortOverrides.load(BlocklistStore(context))
        scope.launch { loadInitial() }
    }

    /** Перечитать пользовательские порты после решения по неизвестному порту. */
    fun reloadPortOverrides(context: Context) {
        PortOverrides.load(BlocklistStore(context))
    }

    private suspend fun loadInitial() {
        val context = appContext ?: return
        val asset = loadAsset(context)
        if (asset != null) {
            apply(asset, RulesSource.ASSET)
        } else {
            apply(BuiltInRules.config, RulesSource.BUILT_IN)
            _status.update { it.copy(lastError = "assets/rules.json не прочитан") }
        }
        loadCached(context)?.let { cached ->
            apply(cached, RulesSource.CACHED)
        }
    }

    private fun loadAsset(context: Context): RulesConfig? {
        return try {
            val raw = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            (RulesParser.parse(raw) as? RulesParseResult.Success)?.config
        } catch (_: Throwable) {
            null
        }
    }

    private fun loadCached(context: Context): RulesConfig? {
        val dir = File(context.filesDir, RULES_DIR)
        val file = File(dir, RULES_FILE)
        val signature = File(dir, SIGNATURE_FILE)
        if (!file.isFile || !signature.isFile) return null
        val content = runCatching { file.readBytes() }.getOrNull() ?: return null
        val signatureText = runCatching { signature.readText() }.getOrNull() ?: return null
        val key = RulesSignature.appPublicKey(context) ?: return null
        if (!RulesSignature.verify(key, content, signatureText)) {
            runCatching { file.delete() }
            runCatching { signature.delete() }
            return null
        }
        return (RulesParser.parse(String(content, Charsets.UTF_8)) as? RulesParseResult.Success)?.config
    }

    suspend fun refresh(context: Context): RulesRefreshResult = withContext(Dispatchers.IO) {
        _status.update { it.copy(checking = true) }
        try {
            val rulesBytes = runCatching { RulesRemoteClient.fetchRules() }.getOrElse { t ->
                return@withContext finish(
                    RulesRefreshResult.NetworkError(t.message ?: "сеть недоступна"),
                    error = null,
                )
            }
            val signature = runCatching { RulesRemoteClient.fetchSignature() }.getOrElse { t ->
                return@withContext finish(
                    RulesRefreshResult.NetworkError(t.message ?: "подпись недоступна"),
                    error = null,
                )
            }
            val key = RulesSignature.appPublicKey(context)
                ?: return@withContext reject("не удалось прочитать ключ подписи приложения")
            if (!RulesSignature.verify(key, rulesBytes, signature)) {
                return@withContext reject("подпись rules.json не совпадает с ключом приложения")
            }
            val parsed = RulesParser.parse(String(rulesBytes, Charsets.UTF_8))
            if (parsed is RulesParseResult.Failure) {
                return@withContext reject(parsed.errors.take(3).joinToString("; "))
            }
            val config = (parsed as RulesParseResult.Success).config
            if (config.updatedAt == current.updatedAt &&
                _status.value.source != RulesSource.ASSET
            ) {
                return@withContext finish(RulesRefreshResult.UpToDate, error = null)
            }
            store(config, rulesBytes, signature)
            apply(config, RulesSource.REMOTE)
            finish(RulesRefreshResult.Updated(config.updatedAt), error = null)
        } catch (t: Throwable) {
            finish(RulesRefreshResult.NetworkError(t.message ?: t.javaClass.simpleName), error = null)
        } finally {
            _status.update { it.copy(checking = false) }
        }
    }

    private fun reject(reason: String): RulesRefreshResult {
        _status.update { it.copy(lastError = reason, lastCheck = System.currentTimeMillis()) }
        return RulesRefreshResult.Rejected(reason)
    }

    private fun finish(result: RulesRefreshResult, error: String?): RulesRefreshResult {
        _status.update { it.copy(lastCheck = System.currentTimeMillis(), lastError = error) }
        return result
    }

    private fun store(config: RulesConfig, content: ByteArray, signature: String) {
        val context = appContext ?: return
        val dir = File(context.filesDir, RULES_DIR)
        runCatching {
            dir.mkdirs()
            val temp = File(dir, "$RULES_FILE.tmp")
            temp.writeBytes(content)
            val signatureTemp = File(dir, "$SIGNATURE_FILE.tmp")
            signatureTemp.writeText(signature)
            if (!temp.renameTo(File(dir, RULES_FILE))) {
                File(dir, RULES_FILE).writeBytes(content)
                temp.delete()
            }
            if (!signatureTemp.renameTo(File(dir, SIGNATURE_FILE))) {
                File(dir, SIGNATURE_FILE).writeText(signature)
                signatureTemp.delete()
            }
        }
    }

    private fun apply(config: RulesConfig, source: RulesSource) {
        current = config
        _status.update {
            it.copy(
                source = source,
                updatedAt = config.updatedAt,
                lastError = if (source == RulesSource.BUILT_IN) it.lastError else null,
            )
        }
    }
}
