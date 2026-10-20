package ru.mlbb.blocker.vpn

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MatchOverlayView(
    context: Context,
    private val onClose: () -> Unit,
    private val onOpen: () -> Unit,
    private val onAllow: () -> Unit,
) : FrameLayout(context) {

    private val titleView = TextView(context)
    private val cityView = TextView(context)
    private val addressView = TextView(context)
    private val detailsView = TextView(context)
    private val pingView = TextView(context)
    private val allowButton = TextView(context)

    init {
        setPadding(dp(14), dp(4), dp(14), dp(4))

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(CARD_BG, 20, STROKE)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            elevation = dp(10).toFloat()
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleView.apply {
            text = "МАТЧ НАЙДЕН"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
            letterSpacing = 0.12f
            setTextColor(ACCENT)
            typeface = Typeface.DEFAULT_BOLD
        }
        val closeView = TextView(context).apply {
            text = "×"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
            setTextColor(SECONDARY)
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(2), 0)
            contentDescription = "Скрыть"
            isClickable = true
            setOnClickListener { onClose() }
        }
        header.addView(
            titleView,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        header.addView(
            closeView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        cityView.apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
        }
        addressView.apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(SECONDARY)
            typeface = Typeface.MONOSPACE
        }
        detailsView.apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(SECONDARY)
        }
        pingView.apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(SECONDARY)
        }

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val openButton = TextView(context).apply {
            text = "Открыть"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#FF111820"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = rounded(ACCENT, 14)
            isClickable = true
            setOnClickListener { onOpen() }
        }
        allowButton.apply {
            text = "Разрешить 5 мин"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(BLOCKED)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = rounded(BLOCKED_BG, 14, BLOCKED)
            isClickable = true
            visibility = View.GONE
            setOnClickListener { onAllow() }
        }
        val openParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        openParams.marginEnd = dp(8)
        buttons.addView(openButton, openParams)
        buttons.addView(
            allowButton,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )

        card.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        card.addView(
            cityView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(6) },
        )
        card.addView(
            addressView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(2) },
        )
        card.addView(
            detailsView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )
        card.addView(
            pingView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(2) },
        )
        card.addView(
            buttons,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(12) },
        )

        addView(
            card,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
    }

    fun bind(info: MatchAlertInfo) {
        titleView.text = if (info.demo) "ПРОВЕРКА КАРТОЧКИ" else "МАТЧ НАЙДЕН"
        cityView.text = info.city ?: "Сервер"
        addressView.text = info.address
        val status = if (info.blocked) "заблокирован профилем" else "разрешён"
        detailsView.text = "Профиль: ${info.profile ?: "не выбран"} · $status"
        detailsView.setTextColor(if (info.blocked) BLOCKED else ALLOWED)
        pingView.text = "Пинг: ${info.pingText ?: "измеряется…"}"
        allowButton.visibility = if (info.blocked) View.VISIBLE else View.GONE
    }

    private fun rounded(color: Int, radiusDp: Int, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
            if (strokeColor != null) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private companion object {
        val TEXT: Int = Color.parseColor("#FFFFFFFF")
        val SECONDARY: Int = Color.parseColor("#FFB8C0CC")
        val ACCENT: Int = Color.parseColor("#FF8FB4FF")
        val ALLOWED: Int = Color.parseColor("#FF54D68C")
        val BLOCKED: Int = Color.parseColor("#FFFF7070")
        val BLOCKED_BG: Int = Color.parseColor("#22FF7070")
        val CARD_BG: Int = Color.parseColor("#F21A1F27")
        val STROKE: Int = Color.parseColor("#33FFFFFF")
    }
}
