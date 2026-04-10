package com.paletteboard.ime.view

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.paletteboard.domain.model.OneHandedMode
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.ToolbarAction
import com.paletteboard.domain.model.ToolbarItem
import com.paletteboard.util.blendArgb
import com.paletteboard.util.primaryColor
import com.paletteboard.util.toAndroidColor

class KeyboardToolbarView(context: Context) : HorizontalScrollView(context) {
    private val chipContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private var actionListener: ((ToolbarAction) -> Unit)? = null

    init {
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        chipContainer.setPadding(context.dp(2f).toInt(), 0, context.dp(2f).toInt(), 0)
        addView(
            chipContainer,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
    }

    fun setOnActionClickListener(listener: (ToolbarAction) -> Unit) {
        actionListener = listener
    }

    fun setItems(
        items: List<ToolbarItem>,
        theme: Theme,
        oneHandedMode: OneHandedMode,
    ) {
        chipContainer.removeAllViews()
        if (items.none { it.enabled }) {
            visibility = GONE
            return
        }
        visibility = VISIBLE

        val baseChipColor = blendArgb(
            theme.toolbarStyle.fill.primaryColor().toAndroidColor(),
            theme.functionalKeyStyle.fill.primaryColor().toAndroidColor(),
            0.28f,
        )
        val activeChipColor = theme.enterKeyStyle.fill.primaryColor().toAndroidColor()
        val labelColor = theme.toolbarStyle.labelColor.toAndroidColor()
        val activeLabelColor = theme.enterKeyStyle.labelColor.toAndroidColor()
        val borderColor = blendArgb(theme.functionalKeyStyle.border.color.toAndroidColor(), labelColor, 0.28f)

        items.filter { it.enabled }.forEach { item ->
            val isActive = item.action == ToolbarAction.ONE_HANDED && oneHandedMode != OneHandedMode.OFF
            chipContainer.addView(
                TextView(context).apply {
                    text = item.action.toolbarLabel(oneHandedMode)
                    gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    setPadding(
                        context.dp(10f).toInt(),
                        context.dp(5.5f).toInt(),
                        context.dp(10f).toInt(),
                        context.dp(5.5f).toInt(),
                    )
                    setTypeface(typeface, if (isActive) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (isActive) activeLabelColor else labelColor)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = context.dp(theme.toolbarStyle.chipCornerRadiusDp * 0.86f)
                        setColor(if (isActive) activeChipColor else baseChipColor)
                        setStroke(context.dp(1f).toInt(), borderColor)
                    }
                    setOnClickListener { actionListener?.invoke(item.action) }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        marginEnd = context.dp(6f).toInt()
                    }
                },
            )
        }
    }
}

private fun ToolbarAction.toolbarLabel(oneHandedMode: OneHandedMode): String = when (this) {
    ToolbarAction.CLIPBOARD -> "Clip"
    ToolbarAction.EMOJI -> "Emoji"
    ToolbarAction.THEMES -> "Style"
    ToolbarAction.SETTINGS -> "Prefs"
    ToolbarAction.ONE_HANDED -> when (oneHandedMode) {
        OneHandedMode.OFF -> "One hand"
        OneHandedMode.LEFT -> "Left"
        OneHandedMode.RIGHT -> "Right"
    }
    ToolbarAction.TRANSLATE -> "Translate"
    ToolbarAction.CALCULATOR -> "Calc"
}
