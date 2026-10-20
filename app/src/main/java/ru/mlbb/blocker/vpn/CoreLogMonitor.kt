package ru.mlbb.blocker.vpn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile

class CoreLogMonitor(
    private val scope: CoroutineScope,
    private val file: File,
    private val onLine: (String) -> Unit,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch(Dispatchers.IO) {
            var position = if (file.exists()) file.length() else 0L
            val partial = StringBuilder()
            while (isActive) {
                if (file.exists() && file.length() > position) {
                    try {
                        RandomAccessFile(file, "r").use { raf ->
                            raf.seek(position)
                            val bytes = ByteArray((file.length() - position).toInt().coerceAtMost(256 * 1024))
                            val read = raf.read(bytes)
                            if (read > 0) {
                                position += read
                                partial.append(String(bytes, 0, read, Charsets.UTF_8))
                                var newline = partial.indexOf('\n')
                                while (newline >= 0) {
                                    val line = partial.substring(0, newline).trimEnd('\r')
                                    partial.delete(0, newline + 1)
                                    if (line.isNotBlank()) onLine(line)
                                    newline = partial.indexOf('\n')
                                }
                                if (partial.length > 8 * 1024) {
                                    partial.clear()
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // file may be rotated while reading
                    }
                }
                delay(500)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
