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
    private const val CITY_MAP_URL = "https://download.db-ip.com/free/dbip-city-lite-%s.mmdb.gz"

    private val _status = MutableStateFlow<GeoIpStatus>(GeoIpStatus.NotDownloaded)
    val status: StateFlow<GeoIpStatus> = _status.asStateFlow()

    private var directory: File? = null
    private var databaseFile: File? = null
    private var prefs: SharedPreferences? = null
    private var reader: Reader? = null
    private val readerLock = Any()

    @Synchronized
    fun initialize(context: Context) {
        if (directory != null) return
        val dir = File(context.applicationContext.filesDir, "geoip")
        dir.mkdirs()
        directory = dir
        databaseFile = File(dir, FILE_NAME)
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val month = prefs?.getString(KEY_MONTH, null)
        _status.value = if (databaseFile?.exists() == true && databaseFile?.length() ?: 0 > 1024) {
            GeoIpStatus.Ready(month ?: "локальная база")
        } else {
            GeoIpStatus.NotDownloaded
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
                val country = ((record["country"] as? Map<*, *>)?.get("iso_code") as? String)
                GeoInfo(city, country)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun download(onProgress: (Int, Long) -> Unit = { _, _ -> }): Boolean = withContext(Dispatchers.IO) {
        if (_status.value is GeoIpStatus.Downloading) return@withContext false
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
        val city = record["city"] as? Map<*, *> ?: return null
        val names = city["names"] as? Map<*, *>
        val ru = names?.get("ru") as? String
        val en = names?.get("en") as? String
        return translateCity(ru ?: en)
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
