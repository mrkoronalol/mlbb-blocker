package ru.mlbb.blocker

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import ru.mlbb.blocker.config.BlockerConfig
import ru.mlbb.blocker.data.BlockerRepository
import ru.mlbb.blocker.data.BlockerStatus
import ru.mlbb.blocker.data.BlocklistStore
import ru.mlbb.blocker.vpn.BlockerController

@RunWith(AndroidJUnit4::class)
class VpnSmokeTest {

    @Test
    fun blockedDestinationFailsFastThroughSink() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val automation = instrumentation.uiAutomation

        automation.executeShellCommand(
            "appops set ${context.packageName} ACTIVATE_VPN allow",
        ).close()
        automation.executeShellCommand(
            "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS",
        ).close()

        Log.i("VpnSmokeTest", "prepare=${VpnService.prepare(context)}")

        val store = BlocklistStore(context)
        store.clearRules()
        store.addRules(listOf("203.0.113.1/32"), "instrumentation")
        store.discoveryMode = false
        store.fastReject = true

        try {
            BlockerController.start(
                context,
                BlockerConfig.newPort(),
                BlockerConfig.newSecret(),
            )
            val deadline = System.currentTimeMillis() + 12_000
            while (System.currentTimeMillis() < deadline) {
                if (BlockerRepository.status.value is BlockerStatus.Running) break
                Thread.sleep(200)
            }
            if (BlockerRepository.status.value !is BlockerStatus.Running) {
                fail("VPN did not start: ${BlockerRepository.status.value}")
            }

            val startedAt = System.currentTimeMillis()
            val output = automation.executeShellCommand(
                "toybox nc -w 3 203.0.113.1 10003",
            ).use(::readAll)
            val elapsed = System.currentTimeMillis() - startedAt
            assertTrue("nc failed: $output", !output.contains("not found"))
            assertTrue("blocked destination took ${elapsed}ms", elapsed < 2_800)
        } finally {
            BlockerController.stop(context)
        }
    }

    private fun readAll(descriptor: ParcelFileDescriptor): String {
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            .bufferedReader()
            .use { it.readText() }
    }
}
