package ru.mlbb.blocker

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.mlbb.blocker.vpn.MatchAlertInfo
import ru.mlbb.blocker.vpn.MatchOverlayView

@RunWith(AndroidJUnit4::class)
class MatchOverlayViewTest {

    @Test
    fun blockedCardShowsAllowButtonAndHidesBlockButton() {
        val view = bind(
            MatchAlertInfo(
                ip = "154.85.91.156",
                port = 5507,
                network = "tcp",
                city = "Москва",
                country = "RU",
                profile = "Москва",
                blocked = true,
                pingText = "126 мс · ±8",
            ),
        )
        val texts = collectTexts(view)
        assertTrue(texts.contains("Москва · RU"))
        assertTrue(texts.contains("154.85.91.156:5507 · Москва"))
        assertTrue(texts.contains("заблокирован"))
        assertTrue(texts.contains("126 мс · ±8"))
        assertTrue(texts.contains("Разрешить 5 мин"))
        assertEquals(View.VISIBLE, findText(view, "Разрешить 5 мин").visibility)
        assertEquals(View.GONE, findText(view, "Заблокировать").visibility)
    }

    @Test
    fun allowedCardShowsBlockAndMuteButtons() {
        val view = bind(
            MatchAlertInfo(
                ip = "92.223.80.65",
                port = 10003,
                network = "udp",
                city = "Хабаровск",
                profile = "ДВ",
                blocked = false,
                pingText = "38 мс",
            ),
        )
        val texts = collectTexts(view)
        assertTrue(texts.contains("Хабаровск"))
        assertTrue(texts.contains("92.223.80.65:10003 · ДВ"))
        assertTrue(texts.contains("разрешён"))
        assertTrue(texts.contains("Заблокировать"))
        assertTrue(texts.contains("Не показывать"))
        assertEquals(View.GONE, findText(view, "Разрешить 5 мин").visibility)
        assertEquals(View.VISIBLE, findText(view, "Заблокировать").visibility)
    }

    private fun bind(info: MatchAlertInfo): MatchOverlayView {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var view: MatchOverlayView? = null
        instrumentation.runOnMainSync {
            view = MatchOverlayView(
                instrumentation.targetContext,
                onClose = {},
                onOpen = {},
                onAllow = {},
                onBlock = {},
                onMute = {},
            ).apply { bind(info) }
        }
        return view!!
    }

    private fun findText(view: View, text: String): TextView {
        if (view is TextView && view.text.toString() == text) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                val match = runCatching { findText(view.getChildAt(index), text) }.getOrNull()
                if (match != null) return match
            }
        }
        throw AssertionError("Не найден TextView с текстом '$text'")
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
