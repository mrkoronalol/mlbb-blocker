package ru.mlbb.blocker

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.mlbb.blocker.vpn.MatchAlertInfo
import ru.mlbb.blocker.vpn.MatchOverlayView

@RunWith(AndroidJUnit4::class)
class MatchOverlayViewTest {

    @Test
    fun bindsCityAddressAndStatus() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var view: MatchOverlayView? = null
        instrumentation.runOnMainSync {
            view = MatchOverlayView(
                instrumentation.targetContext,
                onClose = {},
                onOpen = {},
                onAllow = {},
            ).apply {
                bind(
                    MatchAlertInfo(
                        ip = "154.85.91.153",
                        port = 10003,
                        network = "tcp",
                        city = "Москва",
                        profile = "Москва",
                        blocked = false,
                        pingText = "126 мс",
                    ),
                )
            }
        }

        val texts = collectTexts(view!!)
        assertTrue(texts.any { it == "МАТЧ НАЙДЕН" })
        assertTrue(texts.any { it == "Москва" })
        assertTrue(texts.any { it == "154.85.91.153:10003" })
        assertTrue(texts.any { it == "разрешён" })
        assertTrue(texts.any { it == "126 мс" })
    }

    private fun collectTexts(view: View): List<String> {
        val result = mutableListOf<String>()
        if (view is TextView) result.add(view.text.toString())
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                result.addAll(collectTexts(view.getChildAt(index)))
            }
        }
        return result
    }
}
