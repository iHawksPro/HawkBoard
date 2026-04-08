package com.paletteboard.ime.view

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class SuggestionStripView(context: Context) : LinearLayout(context) {
    private var suggestionClickListener: ((String) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
    }

    fun setOnSuggestionClickListener(listener: (String) -> Unit) {
        suggestionClickListener = listener
    }

    fun setSuggestions(suggestions: List<String>) {
        removeAllViews()
        if (suggestions.isEmpty()) return

        suggestions.take(4).forEach { suggestion ->
            addView(
                TextView(context).apply {
                    text = suggestion
                    gravity = Gravity.CENTER
                    setPadding(
                        context.dp(14f).toInt(),
                        context.dp(10f).toInt(),
                        context.dp(14f).toInt(),
                        context.dp(10f).toInt(),
                    )
                    setTypeface(typeface, Typeface.BOLD)
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
