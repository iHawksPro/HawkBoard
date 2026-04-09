package com.paletteboard.ime.view

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.paletteboard.domain.model.KeyboardTransitionPreset
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.KeyboardLayout
import com.paletteboard.domain.model.OneHandedMode
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.ThemeMotionPreset
import com.paletteboard.domain.model.ToolbarAction
import com.paletteboard.domain.model.ToolbarItem
import com.paletteboard.engine.theme.ThemeManager
import com.paletteboard.engine.theme.DefaultThemes
import com.paletteboard.ime.controller.EmojiUiState
import com.paletteboard.ime.controller.ShiftState
import com.paletteboard.util.androidColors
import com.paletteboard.util.blendArgb
import com.paletteboard.util.primaryColor
import com.paletteboard.util.toAndroidColor
import android.os.SystemClock
import kotlin.math.max
import kotlin.math.sin

class KeyboardRootView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    interface Callback {
        fun onKeyTapped(key: KeyboardKeySpec)
        fun onSuggestionSelected(text: String)
        fun onToolbarAction(action: ToolbarAction)
        fun onEmojiSearchTapped()
        fun onEmojiSearchCleared()
        fun onEmojiGroupSelected(group: String)
        fun onEmojiPreviousPage()
        fun onEmojiNextPage()
    }

    private val toolbarView = KeyboardToolbarView(context)
    private val emojiHeaderView = EmojiHeaderView(context)
    private val suggestionStripView = SuggestionStripView(context)
    private val keyboardCanvasView = KeyboardCanvasView(context)
    private val keyboardStage = FrameLayout(context)
    private val bottomSpacer = View(context)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var callback: Callback? = null
    private var lastThemeId: String? = null
    private var lastLayoutId: String? = null
    private var bottomSystemInsetPx: Int = 0
    private var currentTheme: Theme = DefaultThemes.midnightPulse

    init {
        orientation = VERTICAL
        clipChildren = false
        clipToPadding = false
        setWillNotDraw(false)

        addView(
            toolbarView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
        addView(
            emojiHeaderView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
        addView(
            suggestionStripView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )

        keyboardStage.addView(
            keyboardCanvasView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
        addView(
            keyboardStage,
            LayoutParams(LayoutParams.MATCH_PARENT, context.dp(BASE_KEYBOARD_HEIGHT_DP).toInt()),
        )
        addView(
            bottomSpacer,
            LayoutParams(LayoutParams.MATCH_PARENT, context.dp(DEFAULT_BOTTOM_LIFT_DP).toInt()),
        )

        toolbarView.setOnActionClickListener { action -> callback?.onToolbarAction(action) }
        emojiHeaderView.setCallbacks(
            onSearchTapped = { callback?.onEmojiSearchTapped() },
            onSearchCleared = { callback?.onEmojiSearchCleared() },
            onGroupSelected = { group -> callback?.onEmojiGroupSelected(group) },
            onPreviousPage = { callback?.onEmojiPreviousPage() },
            onNextPage = { callback?.onEmojiNextPage() },
        )
        suggestionStripView.setOnSuggestionClickListener { suggestion -> callback?.onSuggestionSelected(suggestion) }
        keyboardCanvasView.setInteractionListener(
            object : KeyboardCanvasView.InteractionListener {
                override fun onKeyTapped(key: KeyboardKeySpec) {
                    callback?.onKeyTapped(key)
                }

                override fun onInteractionFinished() = Unit
            },
        )
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val gesturesBottom = insets.getInsets(WindowInsetsCompat.Type.systemGestures()).bottom
            bottomSystemInsetPx = max(systemBarsBottom, gesturesBottom)
            updateBottomSpacer()
            insets
        }
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
        shiftState: ShiftState,
        emojiUiState: EmojiUiState?,
        popupPreviewEnabled: Boolean,
        keyboardHeightScale: Float,
        oneHandedMode: OneHandedMode,
    ) {
        currentTheme = theme
        toolbarView.setItems(toolbarItems, theme, oneHandedMode)
        emojiHeaderView.render(emojiUiState, theme)
        suggestionStripView.setSuggestions(suggestions, theme)
        keyboardCanvasView.render(
            layout = layout,
            keyboardTheme = theme,
            manager = themeManager,
            shiftState = shiftState,
            popupPreviewEnabled = popupPreviewEnabled,
        )
        val contentWidth = stageWidthForMode(oneHandedMode)
        val contentGravity = when (oneHandedMode) {
            OneHandedMode.LEFT -> Gravity.START
            OneHandedMode.RIGHT -> Gravity.END
            OneHandedMode.OFF -> Gravity.CENTER_HORIZONTAL
        }
        toolbarView.layoutParams = (toolbarView.layoutParams as LayoutParams).apply {
            width = contentWidth
            gravity = contentGravity
            topMargin = context.dp(1f).toInt()
            bottomMargin = context.dp(3f).toInt()
        }
        emojiHeaderView.layoutParams = (emojiHeaderView.layoutParams as LayoutParams).apply {
            width = contentWidth
            gravity = contentGravity
            bottomMargin = if (emojiUiState?.isVisible == true) context.dp(4f).toInt() else 0
        }
        suggestionStripView.layoutParams = (suggestionStripView.layoutParams as LayoutParams).apply {
            width = contentWidth
            gravity = contentGravity
            bottomMargin = context.dp(4f).toInt()
        }
        keyboardStage.layoutParams = (keyboardStage.layoutParams as LayoutParams).apply {
            height = context.dp(BASE_KEYBOARD_HEIGHT_DP * keyboardHeightScale).toInt()
            width = contentWidth
            gravity = contentGravity
        }
        updateBottomSpacer()
        setBackgroundColor(theme.background.fill.primaryColor().toAndroidColor())
        toolbarView.setBackgroundColor(Color.TRANSPARENT)
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
        if (theme.animationStyle.themeMotionPreset != ThemeMotionPreset.NONE) {
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        if (rect.width() > 0f && rect.height() > 0f) {
            configureBackgroundPaint(rect)
            canvas.drawRect(rect, backgroundPaint)
        }
        if (currentTheme.animationStyle.themeMotionPreset != ThemeMotionPreset.NONE) {
            postInvalidateOnAnimation()
        }
        super.onDraw(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)
    }

    private fun stageWidthForMode(oneHandedMode: OneHandedMode): Int {
        val referenceWidth = if (width > 0) width else resources.displayMetrics.widthPixels
        return when (oneHandedMode) {
            OneHandedMode.OFF -> (referenceWidth - context.dp(4f) * 2f).toInt()
            OneHandedMode.LEFT, OneHandedMode.RIGHT -> (referenceWidth * 0.84f).toInt()
        }
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

    private fun updateBottomSpacer() {
        bottomSpacer.layoutParams = (bottomSpacer.layoutParams as LayoutParams).apply {
            height = max(context.dp(DEFAULT_BOTTOM_LIFT_DP).toInt(), bottomSystemInsetPx + context.dp(4f).toInt())
        }
    }

    private fun configureBackgroundPaint(rect: RectF) {
        val fill = currentTheme.background.fill
        val colors = fill.androidColors()
        val motionPreset = currentTheme.animationStyle.themeMotionPreset
        val motionDuration = currentTheme.animationStyle.motionDurationMs.coerceAtLeast(1200)
        val phase = (SystemClock.uptimeMillis() % motionDuration).toFloat() / motionDuration.toFloat()
        val wave = ((sin(phase * Math.PI * 2) + 1f) / 2f).toFloat()
        val xOscillation = sin(phase * Math.PI * 2).toFloat()
        val yOscillation = sin((phase + 0.25f) * Math.PI * 2).toFloat()
        backgroundPaint.shader = null

        if (motionPreset == ThemeMotionPreset.PULSE && colors.size == 1) {
            backgroundPaint.color = blendArgb(
                colors.first(),
                currentTheme.enterKeyStyle.fill.primaryColor().toAndroidColor(),
                wave * 0.24f,
            )
            return
        }

        val resolvedColors = when {
            colors.size >= 2 -> colors.toIntArray()
            motionPreset == ThemeMotionPreset.NONE -> intArrayOf(colors.first())
            else -> intArrayOf(
                colors.first(),
                currentTheme.shiftKeyStyle.fill.primaryColor().toAndroidColor(),
                currentTheme.enterKeyStyle.fill.primaryColor().toAndroidColor(),
                colors.first(),
            )
        }

        if (resolvedColors.size == 1) {
            backgroundPaint.color = resolvedColors.first()
            return
        }

        val shaderSpan = max(rect.width(), rect.height()) * 1.8f
        val shader = LinearGradient(
            rect.centerX() - shaderSpan,
            rect.centerY() - shaderSpan,
            rect.centerX() + shaderSpan,
            rect.centerY() + shaderSpan,
            resolvedColors,
            null,
            if (motionPreset == ThemeMotionPreset.NONE) Shader.TileMode.CLAMP else Shader.TileMode.MIRROR,
        )
        if (motionPreset != ThemeMotionPreset.NONE) {
            val matrix = Matrix()
            when (motionPreset) {
                ThemeMotionPreset.AURORA -> {
                    matrix.setScale(
                        1.08f + wave * 0.05f,
                        1.02f + wave * 0.04f,
                        rect.centerX(),
                        rect.centerY(),
                    )
                    matrix.postTranslate(
                        rect.width() * 0.14f * xOscillation,
                        rect.height() * 0.08f * yOscillation,
                    )
                }
                ThemeMotionPreset.SHIMMER -> {
                    matrix.setScale(
                        1.16f + wave * 0.06f,
                        1f,
                        rect.centerX(),
                        rect.centerY(),
                    )
                    matrix.postTranslate(rect.width() * 0.18f * xOscillation, 0f)
                }
                ThemeMotionPreset.SPECTRUM -> {
                    matrix.setRotate(phase * 360f, rect.centerX(), rect.centerY())
                    matrix.postScale(
                        1.03f + wave * 0.04f,
                        1.03f + wave * 0.04f,
                        rect.centerX(),
                        rect.centerY(),
                    )
                    matrix.postTranslate(
                        rect.width() * 0.06f * xOscillation,
                        rect.height() * 0.04f * yOscillation,
                    )
                }
                ThemeMotionPreset.PULSE -> {
                    matrix.setScale(
                        1f + 0.05f * xOscillation,
                        1f + 0.05f * xOscillation,
                        rect.centerX(),
                        rect.centerY(),
                    )
                }
                ThemeMotionPreset.NONE -> Unit
            }
            shader.setLocalMatrix(matrix)
        }
        backgroundPaint.shader = shader
    }

    private companion object {
        const val BASE_KEYBOARD_HEIGHT_DP = 300f
        const val DEFAULT_BOTTOM_LIFT_DP = 14f
    }
}
