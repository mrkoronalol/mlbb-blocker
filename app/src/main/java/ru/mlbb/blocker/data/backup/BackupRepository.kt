package ru.mlbb.blocker.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.mlbb.blocker.BuildConfig
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.data.RegionProfiles
import ru.mlbb.blocker.data.db.MonitorDatabase
import java.io.File

object BackupRepository {

    suspend fun snapshot(context: Context): BackupData = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val store = BlocklistStore(app)
        val dao = MonitorDatabase.get(app).monitorDao()
        BackupData(
            appVersion = BuildConfig.VERSION_NAME,
            createdAt = System.currentTimeMillis(),
            selectedProfile = store.selectedProfile,
            settings = BackupSettings(
                discoveryMode = store.discoveryMode,
                fastReject = store.fastReject,
                matchAlerts = store.matchAlerts,
                hideStartupAlerts = store.hideStartupAlerts,
                autoStart = store.autoStart,
                onboardingDone = store.onboardingDone,
                logBlockedAttempts = store.logBlockedAttempts,
            ),
            rules = store.loadRules(),
            allowRules = store.loadAllowRules(),
            endpoints = dao.allEndpoints(),
            blockedAttempts = dao.allBlockedAttempts(),
            matchSessions = dao.allMatchSessions(),
            pingSamples = dao.allPingSamples(),
        )
    }

    suspend fun export(context: Context, uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val data = snapshot(context)
        val text = BackupCodec.encode(data)
        val resolver = context.applicationContext.contentResolver
        resolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write(text)
        } ?: throw IllegalStateException("Не удалось открыть файл для записи")
        BackupCodec.summary(data)
    }

    suspend fun read(context: Context, uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val resolver = context.applicationContext.contentResolver
        val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Не удалось открыть файл")
        BackupCodec.decode(text)
    }

    suspend fun apply(context: Context, data: BackupData, merge: Boolean) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val store = BlocklistStore(app)
        val db = MonitorDatabase.get(app)
        val dao = db.monitorDao()

        db.withTransaction {
            if (!merge) {
                dao.clearEndpoints()
                dao.clearBlockedAttempts()
                dao.clearMatchSessions()
                dao.clearPingSamples()
                dao.insertEndpoints(data.endpoints)
                dao.insertBlockedAttempts(data.blockedAttempts.map { it.copy(id = 0) })
                dao.insertMatchSessions(data.matchSessions.map { it.copy(id = 0) })
                dao.insertPingSamples(data.pingSamples.map { it.copy(id = 0) })
            } else {
                dao.insertEndpoints(data.endpoints)
                dao.insertBlockedAttempts(data.blockedAttempts.map { it.copy(id = 0) })
                dao.insertMatchSessions(data.matchSessions.map { it.copy(id = 0) })
                dao.insertPingSamples(data.pingSamples.map { it.copy(id = 0) })
            }
        }

        store.discoveryMode = data.settings.discoveryMode
        store.fastReject = data.settings.fastReject
        store.matchAlerts = data.settings.matchAlerts
        store.hideStartupAlerts = data.settings.hideStartupAlerts
        store.autoStart = data.settings.autoStart
        store.onboardingDone = data.settings.onboardingDone
        store.logBlockedAttempts = data.settings.logBlockedAttempts
        store.selectedProfile = data.selectedProfile
            ?.takeIf { RegionProfiles.byId(it) != null }

        if (merge) {
            val currentRules = store.loadRules()
            val existingRuleValues = currentRules.map { it.value }.toSet()
            store.saveRules(
                currentRules + data.rules.filterNot { it.value in existingRuleValues },
            )
            val currentAllow = store.loadAllowRules()
            val existingAllowValues = currentAllow.map { it.value }.toSet()
            store.saveAllowRules(
                currentAllow + data.allowRules.filterNot { it.value in existingAllowValues },
            )
        } else {
            store.saveRules(data.rules)
            store.saveAllowRules(data.allowRules)
        }
    }

    suspend fun writeAutoBackup(context: Context): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.applicationContext.cacheDir, "backups").apply { mkdirs() }
            val file = File(dir, "auto-backup-${System.currentTimeMillis()}.json")
            val data = snapshot(context)
            file.writeText(BackupCodec.encode(data))
            file
        }.getOrNull()
    }
}
