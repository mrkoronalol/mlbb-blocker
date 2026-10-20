package ru.mlbb.blocker.data.geo

import android.content.Context
import android.content.SharedPreferences
import com.maxmind.db.Reader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

sealed interface GeoIpStatus {
    data object NotDownloaded : GeoIpStatus
    data object Preparing : GeoIpStatus
    data class Downloading(val progress: Int, val received: Long) : GeoIpStatus
    data class Ready(val month: String) : GeoIpStatus
    data class Error(val message: String) : GeoIpStatus
}

data class GeoInfo(
    val city: String?,
    val country: String?,
)

object GeoIpRepository {

    private const val PREFS = "mlbb_geoip"
    private const val KEY_MONTH = "month"
    private const val FILE_NAME = "dbip-city-lite.mmdb"
    private const val BUNDLED_ASSET = "geoip/dbip-city-ipv4.mmdb"
    private const val BUNDLED_LABEL = "встроенная база"
    private const val CITY_MAP_URL = "https://download.db-ip.com/free/dbip-city-lite-%s.mmdb.gz"

    private val _status = MutableStateFlow<GeoIpStatus>(GeoIpStatus.NotDownloaded)
    val status: StateFlow<GeoIpStatus> = _status.asStateFlow()

    private var appContext: Context? = null
    private var directory: File? = null
    private var databaseFile: File? = null
    private var prefs: SharedPreferences? = null
    private var reader: Reader? = null
    private val readerLock = Any()

    @Synchronized
    fun initialize(context: Context) {
        if (directory != null) return
        val app = context.applicationContext
        appContext = app
        val dir = File(app.filesDir, "geoip")
        dir.mkdirs()
        directory = dir
        databaseFile = File(dir, FILE_NAME)
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val month = prefs?.getString(KEY_MONTH, null)
        if (databaseFile?.exists() == true && (databaseFile?.length() ?: 0) > 1024) {
            _status.value = GeoIpStatus.Ready(month ?: "локальная база")
        } else {
            _status.value = GeoIpStatus.Preparing
            Thread({ installBundled() }, "geoip-bundled").apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun installBundled() {
        val context = appContext ?: return
        val dir = directory ?: return
        val target = databaseFile ?: return
        val temp = File(dir, "bundled.mmdb.tmp")
        try {
            if (target.exists() && target.length() > 1024) {
                val month = prefs?.getString(KEY_MONTH, null) ?: BUNDLED_LABEL
                _status.value = GeoIpStatus.Ready(month)
                return
            }
            context.assets.open(BUNDLED_ASSET).use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            }
            Reader(temp).use { }
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            databaseFile = target
            synchronized(readerLock) {
                runCatching { reader?.close() }
                reader = null
            }
            prefs?.edit()?.putString(KEY_MONTH, BUNDLED_LABEL)?.apply()
            _status.value = GeoIpStatus.Ready(BUNDLED_LABEL)
        } catch (t: Throwable) {
            temp.delete()
            _status.value = GeoIpStatus.Error("встроенная база: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    fun lookup(ip: String): GeoInfo? {
        val inet = try {
            InetAddress.getByName(ip)
        } catch (_: Exception) {
            return null
        }
        synchronized(readerLock) {
            val current = reader ?: openReader() ?: return null
            return try {
                @Suppress("UNCHECKED_CAST")
                val record = current.get(inet, Map::class.java) as? Map<*, *> ?: return null
                val city = cityName(record)
                val country = countryCode(record)
                GeoInfo(city, country)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun download(onProgress: (Int, Long) -> Unit = { _, _ -> }): Boolean = withContext(Dispatchers.IO) {
        if (_status.value is GeoIpStatus.Downloading || _status.value is GeoIpStatus.Preparing) {
            return@withContext false
        }
        val dir = directory ?: return@withContext false
        _status.value = GeoIpStatus.Downloading(0, 0)
        val months = recentMonths()
        var lastError = "не удалось скачать базу"
        for (month in months) {
            val url = String.format(CITY_MAP_URL, month)
            val temp = File(dir, "download.mmdb.tmp")
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 120_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "MLBB-Blocker/GeoIP")
                }
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    lastError = "HTTP $code для $month"
                    connection.disconnect()
                    continue
                }
                val total = connection.contentLengthLong
                var received = 0L
                connection.inputStream.use { input ->
                    GZIPInputStream(input).use { gzip ->
                        temp.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (gzip.read(buffer).also { read = it } > 0) {
                                output.write(buffer, 0, read)
                                received += read
                                val progress = if (total > 0) ((received * 100) / total).toInt() else 0
                                _status.value = GeoIpStatus.Downloading(progress.coerceIn(0, 99), received)
                                onProgress(progress, received)
                            }
                        }
                    }
                }
                connection.disconnect()
                Reader(temp).use { }
                val target = File(dir, FILE_NAME)
                if (target.exists()) target.delete()
                if (!temp.renameTo(target)) {
                    temp.copyTo(target, overwrite = true)
                    temp.delete()
                }
                databaseFile = target
                synchronized(readerLock) {
                    runCatching { reader?.close() }
                    reader = null
                }
                prefs?.edit()?.putString(KEY_MONTH, month)?.apply()
                _status.value = GeoIpStatus.Ready(month)
                return@withContext true
            } catch (t: Throwable) {
                lastError = t.message ?: t.javaClass.simpleName
                temp.delete()
            }
        }
        _status.value = GeoIpStatus.Error(lastError)
        false
    }

    fun delete() {
        synchronized(readerLock) {
            runCatching { reader?.close() }
            reader = null
        }
        databaseFile?.delete()
        _status.value = GeoIpStatus.NotDownloaded
    }

    private fun openReader(): Reader? {
        val file = databaseFile ?: return null
        if (!file.exists()) return null
        return try {
            Reader(file).also { reader = it }
        } catch (_: Exception) {
            null
        }
    }

    private fun cityName(record: Map<*, *>): String? {
        val city = record["city"]
        if (city is String) return translateCity(city)
        val cityMap = city as? Map<*, *> ?: return null
        val names = cityMap["names"] as? Map<*, *>
        val ru = names?.get("ru") as? String
        val en = names?.get("en") as? String
        return translateCity(ru ?: en)
    }

    private fun countryCode(record: Map<*, *>): String? {
        return ((record["country"] as? Map<*, *>)?.get("iso_code") as? String)
            ?: record["country_code"] as? String
    }

    private fun translateCity(name: String?): String? {
        if (name.isNullOrBlank()) return null
        return when (name.lowercase(Locale.ROOT)) {
            "moscow" -> "Москва"
            "novosibirsk" -> "Новосибирск"
            "yekaterinburg", "ekaterinburg" -> "Екатеринбург"
            "rostov-on-don", "rostov na donu" -> "Ростов-на-Дону"
            "khabarovsk" -> "Хабаровск"
            "vladivostok" -> "Владивосток"
            "abakan" -> "Абакан"
            "frankfurt am main", "frankfurt" -> "Франкфурт"
            "tbilisi" -> "Тбилиси"
            "vientiane" -> "Вьентьян"
            else -> name
        }
    }

    private fun recentMonths(): List<String> {
        val format = SimpleDateFormat("yyyy-MM", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val now = System.currentTimeMillis()
        return (0..3).map { offset ->
            format.format(Date(now - offset * 31L * 24 * 60 * 60 * 1000))
        }
    }
}
