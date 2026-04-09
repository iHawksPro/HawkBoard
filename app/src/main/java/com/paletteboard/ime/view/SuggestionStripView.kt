package com.paletteboard.ime.view

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.paletteboard.domain.model.Theme
import com.paletteboard.util.blendArgb
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
        val textColor = theme.toolbarStyle.labelColor.toAndroidColor()
        val secondaryTextColor = theme.defaultKeyStyle.labelColor.toAndroidColor()
        val highlightFill = blendArgb(containerColor, theme.defaultKeyStyle.fill.primaryColor().toAndroidColor(), 0.46f)

        suggestions.take(3).forEachIndexed { index, suggestion ->
            addView(
                TextView(context).apply {
                    text = suggestion
                    gravity = Gravity.CENTER
                    minHeight = context.dp(30f).toInt()
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, if (index == 0) 17.5f else 16.5f)
                    setPadding(
                        context.dp(8f).toInt(),
                        context.dp(6f).toInt(),
                        context.dp(8f).toInt(),
                        context.dp(6f).toInt(),
                    )
                    setTypeface(typeface, if (index == 0) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (index == 0) textColor else secondaryTextColor)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = context.dp(16f)
                        setColor(if (index == 0) highlightFill else android.graphics.Color.TRANSPARENT)
                    }
                    setOnClickListener { suggestionClickListener?.invoke(suggestion) }
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = context.dp(3f).toInt()
                        marginEnd = context.dp(3f).toInt()
                    }
                },
            )
        }
    }
}
