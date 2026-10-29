package ru.mlbb.blocker.vpn

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MatchOverlayView(
    context: Context,
    private val onClose: () -> Unit,
    private val onOpen: () -> Unit,
    private val onAllow: () -> Unit = {},
    private val onBlock: () -> Unit = {},
    private val onMute: () -> Unit = {},
) : FrameLayout(context) {

    private val titleView = TextView(context)
    private val cityView = TextView(context)
    private val statusView = TextView(context)
    private val addressView = TextView(context)
    private val pingView = TextView(context)
    private val allowButton = TextView(context)
    private val blockButton = TextView(context)

    init {
        setPadding(dp(12), dp(4), dp(12), dp(4))

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(CARD_BG, 16, STROKE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            elevation = dp(8).toFloat()
            minimumWidth = dp(280)
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleView.apply {
            text = "МАТЧ НАЙДЕН"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            letterSpacing = 0.1f
            setTextColor(ACCENT)
            typeface = Typeface.DEFAULT_BOLD
        }
        val closeView = TextView(context).apply {
            text = "×"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(SECONDARY)
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, 0, dp(2))
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
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(TEXT)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        statusView.apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
        }
        addressView.apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(SECONDARY)
            typeface = Typeface.MONOSPACE
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        pingView.apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(ACCENT)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, 0, 0)
        }

        val cityRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cityRow.addView(
            cityView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        cityRow.addView(
            statusView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginStart = dp(6) },
        )

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        textColumn.addView(
            cityRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        textColumn.addView(
            addressView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val mainRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        mainRow.addView(
            textColumn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        mainRow.addView(
            pingView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val openButton = actionButton(
            label = "Открыть",
            textColor = Color.parseColor("#FF111820"),
            background = rounded(ACCENT, 12),
            onClick = onOpen,
        )
        allowButton.apply {
            text = "Разрешить 5 мин"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(BLOCKED)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = rounded(BLOCKED_BG, 12, BLOCKED)
            isClickable = true
            visibility = View.GONE
            setOnClickListener { onAllow() }
        }
        blockButton.apply {
            text = "Заблокировать"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(BLOCKED)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = rounded(BLOCKED_BG, 12, BLOCKED)
            isClickable = true
            setOnClickListener { onBlock() }
        }
        val muteButton = actionButton(
            label = "Не показывать",
            textColor = SECONDARY,
            background = rounded(MUTED_BG, 12, MUTED_STROKE),
            onClick = onMute,
        )

        val primaryActions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        primaryActions.addView(
            openButton,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        primaryActions.addView(
            allowButton,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(8) },
        )

        val secondaryActions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        secondaryActions.addView(
            blockButton,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        secondaryActions.addView(
            muteButton,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(8) },
        )

        card.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        card.addView(
            mainRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(3) },
        )
        card.addView(
            primaryActions,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )
        card.addView(
            secondaryActions,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(6) },
        )

        addView(
            card,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
    }

    fun bind(info: MatchAlertInfo) {
        titleView.text = if (info.demo) "ПРОВЕРКА КАРТОЧКИ" else "МАТЧ НАЙДЕН"
        cityView.text = info.location
        addressView.text = buildString {
            append(info.address)
            info.profile?.let { append(" · ").append(it) }
        }
        statusView.text = if (info.blocked) "заблокирован" else "разрешён"
        statusView.setTextColor(if (info.blocked) BLOCKED else ALLOWED)
        pingView.text = info.pingText ?: "пинг…"
        allowButton.visibility = if (info.blocked) View.VISIBLE else View.GONE
        blockButton.visibility = if (info.blocked) View.GONE else View.VISIBLE
    }

    private fun actionButton(
        label: String,
        textColor: Int,
        background: GradientDrawable,
        onClick: () -> Unit,
    ): TextView = TextView(context).apply {
        text = label
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(textColor)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(6), dp(12), dp(6))
        this.background = background
        isClickable = true
        setOnClickListener { onClick() }
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
        val MUTED_BG: Int = Color.parseColor("#14FFFFFF")
        val MUTED_STROKE: Int = Color.parseColor("#33FFFFFF")
        val CARD_BG: Int = Color.parseColor("#F21A1F27")
        val STROKE: Int = Color.parseColor("#33FFFFFF")
    }
}
