package com.paletteboard.ime.view

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.paletteboard.domain.model.Theme
import com.paletteboard.util.primaryColor
import com.paletteboard.util.toAndroidColor

class SuggestionStripView(context: Context) : LinearLayout(context) {
    private var suggestionClickListener: ((String) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
    }

    fun setOnSuggestionClickListener(listener: (String) -> Unit) {
        suggestionClickListener = listener
    }

    fun setSuggestions(
        suggestions: List<String>,
        theme: Theme,
    ) {
        removeAllViews()
        if (suggestions.isEmpty()) {
            visibility = GONE
            return
        }
        visibility = VISIBLE

        val containerColor = theme.toolbarStyle.fill.primaryColor().toAndroidColor()
        val keyColor = theme.defaultKeyStyle.fill.primaryColor().toAndroidColor()
        val textColor = theme.toolbarStyle.labelColor.toAndroidColor()
        val secondaryTextColor = theme.defaultKeyStyle.labelColor.toAndroidColor()
        val borderColor = theme.defaultKeyStyle.border.color.toAndroidColor()

        suggestions.take(4).forEachIndexed { index, suggestion ->
            addView(
                TextView(context).apply {
                    text = suggestion
                    gravity = Gravity.CENTER
                    setPadding(
                        context.dp(14f).toInt(),
                        context.dp(11f).toInt(),
                        context.dp(14f).toInt(),
                        context.dp(11f).toInt(),
                    )
                    setTypeface(typeface, if (index == 0) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (index == 0) textColor else secondaryTextColor)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = context.dp(18f)
                        setColor(if (index == 0) keyColor else containerColor)
                        setStroke(context.dp(1f).toInt(), borderColor)
                    }
                    setOnClickListener { suggestionClickListener?.invoke(suggestion) }
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = context.dp(4f).toInt()
                        marginEnd = context.dp(4f).toInt()
                    }
                },
            )
        }
    }
}
