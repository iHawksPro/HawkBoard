package com.paletteboard.ime.view

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.paletteboard.domain.model.Theme
import com.paletteboard.ime.controller.EmojiUiState
import com.paletteboard.util.blendArgb
import com.paletteboard.util.primaryColor
import com.paletteboard.util.toAndroidColor

class EmojiHeaderView(context: Context) : LinearLayout(context) {
    private val topRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val pageIndicatorView = buildPillTextView(compact = true)
    private val searchFieldView = buildPillTextView()
    private val searchActionView = buildPillTextView(compact = true)

    private val navRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val previousPageView = buildPillTextView(compact = true)
    private val nextPageView = buildPillTextView(compact = true)
    private val groupContainer = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val groupScrollView = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        addView(
            groupContainer,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
    }

    private var onSearchTapped: (() -> Unit)? = null
    private var onSearchCleared: (() -> Unit)? = null
    private var onGroupSelected: ((String) -> Unit)? = null
    private var onPreviousPage: (() -> Unit)? = null
    private var onNextPage: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        clipChildren = false
        clipToPadding = false
        setPadding(0, context.dp(1f).toInt(), 0, context.dp(2f).toInt())

        topRow.addView(
            pageIndicatorView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = context.dp(8f).toInt()
            },
        )
        topRow.addView(
            searchFieldView,
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = context.dp(8f).toInt()
            },
        )
        topRow.addView(
            searchActionView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )

        navRow.addView(
            previousPageView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = context.dp(8f).toInt()
            },
        )
        navRow.addView(
            groupScrollView,
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f),
        )
        navRow.addView(
            nextPageView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = context.dp(8f).toInt()
            },
        )

        addView(
            topRow,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = context.dp(4f).toInt()
            },
        )
        addView(
            navRow,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )

        searchFieldView.setOnClickListener { onSearchTapped?.invoke() }
        searchActionView.setOnClickListener {
            if (searchActionView.text == "Clear") {
                onSearchCleared?.invoke()
            } else {
                onSearchTapped?.invoke()
            }
        }
        previousPageView.setOnClickListener { onPreviousPage?.invoke() }
        nextPageView.setOnClickListener { onNextPage?.invoke() }
    }

    fun setCallbacks(
        onSearchTapped: () -> Unit,
        onSearchCleared: () -> Unit,
        onGroupSelected: (String) -> Unit,
        onPreviousPage: () -> Unit,
        onNextPage: () -> Unit,
    ) {
        this.onSearchTapped = onSearchTapped
        this.onSearchCleared = onSearchCleared
        this.onGroupSelected = onGroupSelected
        this.onPreviousPage = onPreviousPage
        this.onNextPage = onNextPage
    }

    fun render(
        state: EmojiUiState?,
        theme: Theme,
    ) {
        if (state == null || !state.isVisible) {
            visibility = GONE
            return
        }
        visibility = VISIBLE

        val accentColor = theme.enterKeyStyle.fill.primaryColor().toAndroidColor()
        val baseChipColor = blendArgb(
            theme.toolbarStyle.fill.primaryColor().toAndroidColor(),
            theme.functionalKeyStyle.fill.primaryColor().toAndroidColor(),
            0.2f,
        )
        val surfaceColor = blendArgb(
            theme.background.fill.primaryColor().toAndroidColor(),
            theme.defaultKeyStyle.fill.primaryColor().toAndroidColor(),
            0.72f,
        )
        val labelColor = theme.toolbarStyle.labelColor.toAndroidColor()
        val borderColor = blendArgb(theme.defaultKeyStyle.border.color.toAndroidColor(), labelColor, 0.2f)
        val accentLabelColor = theme.enterKeyStyle.labelColor.toAndroidColor()

        pageIndicatorView.text = if (state.searchActive) "Search" else "${state.pageIndex + 1}/${state.totalPages}"
        stylePill(
            view = pageIndicatorView,
            fillColor = baseChipColor,
            labelColor = labelColor,
            borderColor = borderColor,
            emphasized = false,
        )

        searchFieldView.text = when {
            state.searchActive && state.searchQuery.isNotBlank() -> "\u2315 ${state.searchQuery}"
            state.searchActive -> "\u2315 Search emoji"
            else -> "\u2315 Search emoji"
        }
        stylePill(
            view = searchFieldView,
            fillColor = if (state.searchActive) blendArgb(accentColor, surfaceColor, 0.45f) else surfaceColor,
            labelColor = if (state.searchActive) accentLabelColor else labelColor,
            borderColor = borderColor,
            emphasized = state.searchActive,
        )

        searchActionView.text = if (state.searchActive) "Clear" else "Find"
        stylePill(
            view = searchActionView,
            fillColor = if (state.searchActive) surfaceColor else baseChipColor,
            labelColor = labelColor,
            borderColor = borderColor,
            emphasized = false,
        )

        previousPageView.text = "\u2039"
        previousPageView.visibility = if (state.searchActive) INVISIBLE else VISIBLE
        previousPageView.isEnabled = state.canGoPrevious && !state.searchActive
        previousPageView.alpha = if (state.canGoPrevious && !state.searchActive) 1f else 0.45f
        stylePill(
            view = previousPageView,
            fillColor = baseChipColor,
            labelColor = labelColor,
            borderColor = borderColor,
            emphasized = false,
        )

        nextPageView.text = "\u203A"
        nextPageView.visibility = if (state.searchActive) INVISIBLE else VISIBLE
        nextPageView.isEnabled = state.canGoNext && !state.searchActive
        nextPageView.alpha = if (state.canGoNext && !state.searchActive) 1f else 0.45f
        stylePill(
            view = nextPageView,
            fillColor = baseChipColor,
            labelColor = labelColor,
            borderColor = borderColor,
            emphasized = false,
        )

        groupContainer.removeAllViews()
        state.groups.forEachIndexed { index, group ->
            val selected = group == state.selectedGroup && !state.searchActive
            groupContainer.addView(
                buildPillTextView(compact = true).apply {
                    text = groupLabel(group)
                    stylePill(
                        view = this,
                        fillColor = if (selected) accentColor else surfaceColor,
                        labelColor = if (selected) accentLabelColor else labelColor,
                        borderColor = borderColor,
                        emphasized = selected,
                    )
                    setOnClickListener { onGroupSelected?.invoke(group) }
                    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        marginEnd = if (index == state.groups.lastIndex) 0 else context.dp(6f).toInt()
                    }
                },
            )
        }
    }

    private fun buildPillTextView(compact: Boolean = false): TextView = TextView(context).apply {
        gravity = Gravity.CENTER
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setPadding(
            context.dp(if (compact) 11f else 13f).toInt(),
            context.dp(if (compact) 7f else 9f).toInt(),
            context.dp(if (compact) 11f else 13f).toInt(),
            context.dp(if (compact) 7f else 9f).toInt(),
        )
        textSize = if (compact) 12.5f else 13.5f
    }

    private fun stylePill(
        view: TextView,
        fillColor: Int,
        labelColor: Int,
        borderColor: Int,
        emphasized: Boolean,
    ) {
        view.setTextColor(labelColor)
        view.setTypeface(view.typeface, if (emphasized) Typeface.BOLD else Typeface.NORMAL)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.dp(14f)
            setColor(fillColor)
            setStroke(context.dp(1f).toInt(), borderColor)
        }
    }

    private fun groupLabel(group: String): String = when (group) {
        "Recent" -> "\uD83D\uDD58 Recent"
        "Smileys & Emotion" -> "\uD83D\uDE42 Smileys"
        "People & Body" -> "\uD83D\uDC4D People"
        "Animals & Nature" -> "\uD83D\uDC3B Nature"
        "Food & Drink" -> "\uD83C\uDF54 Food"
        "Travel & Places" -> "\uD83D\uDE97 Travel"
        "Activities" -> "\u26BD Play"
        "Objects" -> "\uD83D\uDCA1 Objects"
        "Symbols" -> "\u2764\uFE0F Symbols"
        "Flags" -> "\uD83C\uDFC1 Flags"
        else -> group
    }
}
