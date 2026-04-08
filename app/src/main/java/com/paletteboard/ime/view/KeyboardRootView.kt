package com.paletteboard.ime.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.paletteboard.domain.model.GestureSample
import com.paletteboard.domain.model.KeyboardTransitionPreset
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.KeyboardLayout
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.ToolbarAction
import com.paletteboard.domain.model.ToolbarItem
import com.paletteboard.domain.model.TouchPoint
import com.paletteboard.engine.theme.ThemeManager
import com.paletteboard.util.primaryColor
import com.paletteboard.util.toAndroidColor
import kotlin.math.max

class KeyboardRootView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    interface Callback {
        fun onKeyTapped(key: KeyboardKeySpec)
        fun onGestureCompleted(sample: GestureSample)
        fun onSuggestionSelected(text: String)
        fun onToolbarAction(action: ToolbarAction)
    }

    private val toolbarView = KeyboardToolbarView(context)
    private val suggestionStripView = SuggestionStripView(context)
    private val keyboardCanvasView = KeyboardCanvasView(context)
    private val gestureTrailOverlayView = GestureTrailOverlayView(context)
    private val keyboardStage = FrameLayout(context)

    private var callback: Callback? = null
    private var currentTheme: Theme? = null
    private var lastThemeId: String? = null
    private var lastLayoutId: String? = null

    init {
        orientation = VERTICAL
        clipChildren = false
        clipToPadding = false

        addView(
            toolbarView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(
                    context.dp(4f).toInt(),
                    context.dp(4f).toInt(),
                    context.dp(4f).toInt(),
                    context.dp(4f).toInt(),
                )
            },
        )
        addView(
            suggestionStripView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(
                    context.dp(4f).toInt(),
                    0,
                    context.dp(4f).toInt(),
                    context.dp(4f).toInt(),
                )
            },
        )

        keyboardStage.addView(
            keyboardCanvasView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
        keyboardStage.addView(
            gestureTrailOverlayView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
        addView(
            keyboardStage,
            LayoutParams(LayoutParams.MATCH_PARENT, context.dp(252f).toInt()),
        )

        toolbarView.setOnActionClickListener { action -> callback?.onToolbarAction(action) }
        suggestionStripView.setOnSuggestionClickListener { suggestion -> callback?.onSuggestionSelected(suggestion) }
        keyboardCanvasView.setInteractionListener(
            object : KeyboardCanvasView.InteractionListener {
                override fun onKeyTapped(key: KeyboardKeySpec) {
                    callback?.onKeyTapped(key)
                }

                override fun onGestureProgress(points: List<TouchPoint>) {
                    currentTheme?.let { theme ->
                        gestureTrailOverlayView.updateTrail(
                            points = points,
                            color = theme.gestureTrailStyle.color.toAndroidColor(),
                            strokeWidthPx = context.dp(theme.gestureTrailStyle.thicknessDp),
                        )
                    }
                }

                override fun onGestureCompleted(sample: GestureSample) {
                    callback?.onGestureCompleted(sample)
                }

                override fun onInteractionFinished() {
                    gestureTrailOverlayView.clearTrail()
                }
            },
        )
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun render(
        layout: KeyboardLayout,
        theme: Theme,
        suggestions: List<String>,
        toolbarItems: List<ToolbarItem>,
        themeManager: ThemeManager,
        keyboardHeightScale: Float,
    ) {
        currentTheme = theme
        toolbarView.setItems(toolbarItems)
        suggestionStripView.setSuggestions(suggestions)
        keyboardCanvasView.render(layout, theme, themeManager)
        keyboardStage.layoutParams = keyboardStage.layoutParams.apply {
            height = context.dp(252f * keyboardHeightScale).toInt()
        }
        setBackgroundColor(theme.background.fill.primaryColor().toAndroidColor())
        toolbarView.setBackgroundColor(theme.toolbarStyle.fill.primaryColor().toAndroidColor())
        suggestionStripView.setBackgroundColor(Color.TRANSPARENT)
        if (lastThemeId != null && (lastThemeId != theme.id || lastLayoutId != layout.id)) {
            animateKeyboardTransition(
                preset = theme.animationStyle.keyboardTransitionPreset,
                durationMs = theme.animationStyle.durationMs,
            )
        }
        lastThemeId = theme.id
        lastLayoutId = layout.id
        requestLayout()
    }

    fun clearGestureTrail() {
        gestureTrailOverlayView.clearTrail()
    }

    private fun animateKeyboardTransition(
        preset: KeyboardTransitionPreset,
        durationMs: Int,
    ) {
        val stageDuration = max(durationMs.toLong(), 90L)
        listOf(toolbarView, suggestionStripView, keyboardStage).forEach { view ->
            view.animate().cancel()
            view.alpha = 1f
            view.translationY = 0f
            view.scaleX = 1f
            view.scaleY = 1f
        }

        when (preset) {
            KeyboardTransitionPreset.NONE -> Unit
            KeyboardTransitionPreset.FADE_SLIDE -> {
                keyboardStage.alpha = 0.45f
                keyboardStage.translationY = context.dp(14f)
                keyboardStage.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(stageDuration)
                    .start()
            }

            KeyboardTransitionPreset.RISE -> {
                keyboardStage.alpha = 0.82f
                keyboardStage.translationY = context.dp(24f)
                keyboardStage.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(stageDuration)
                    .start()
            }

            KeyboardTransitionPreset.ZOOM -> {
                keyboardStage.alpha = 0.68f
                keyboardStage.scaleX = 0.94f
                keyboardStage.scaleY = 0.94f
                keyboardStage.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(stageDuration)
                    .start()
            }

            KeyboardTransitionPreset.WAVE -> {
                listOf(toolbarView, suggestionStripView, keyboardStage).forEachIndexed { index, view ->
                    view.alpha = 0.35f
                    view.translationY = context.dp(10f + index * 4f)
                    view.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setStartDelay(index * 40L)
                        .setDuration(stageDuration)
                        .start()
                }
            }
        }
    }
}
