package ru.mlbb.blocker.vpn

import android.content.Context
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions
import ru.mlbb.blocker.BuildConfig
import java.io.File

object LibboxSetup {

    @Volatile
    private var initialized = false

    @Synchronized
    fun ensure(context: Context) {
        if (initialized) return

        System.setProperty("GODEBUG", "stacktraceback=2")
        try {
            go.Seq.setContext(context.applicationContext)
        } catch (_: Throwable) {
            // gomobile already initialized
        }

        val base = File(context.filesDir, "sing-box").apply { mkdirs() }
        val working = File(context.getExternalFilesDir(null) ?: context.filesDir, "sing-box").apply { mkdirs() }
        val temp = File(context.cacheDir, "sing-box").apply { mkdirs() }

        val options = SetupOptions().apply {
            basePath = base.absolutePath
            workingPath = working.absolutePath
            tempPath = temp.absolutePath
            fixAndroidStack = true
            oomKillerEnabled = true
            logMaxLines = 1000L
            debug = false
            appVersion = BuildConfig.VERSION_NAME
            appMarketingVersion = BuildConfig.VERSION_NAME
        }
        Libbox.setup(options)
        initialized = true
    }
}
