package com.paletteboard.ime.view

import android.content.Context
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.paletteboard.domain.model.ToolbarAction
import com.paletteboard.domain.model.ToolbarItem

class KeyboardToolbarView(context: Context) : HorizontalScrollView(context) {
    private val chipContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private var actionListener: ((ToolbarAction) -> Unit)? = null

    init {
        isHorizontalScrollBarEnabled = false
        addView(
            chipContainer,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
    }

    fun setOnActionClickListener(listener: (ToolbarAction) -> Unit) {
        actionListener = listener
    }

    fun setItems(items: List<ToolbarItem>) {
        chipContainer.removeAllViews()
        items.filter { it.enabled }.forEach { item ->
            chipContainer.addView(
                TextView(context).apply {
                    text = item.action.name.replace("_", " ")
                    gravity = Gravity.CENTER
                    setPadding(
                        context.dp(12f).toInt(),
                        context.dp(8f).toInt(),
                        context.dp(12f).toInt(),
                        context.dp(8f).toInt(),
                    )
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
