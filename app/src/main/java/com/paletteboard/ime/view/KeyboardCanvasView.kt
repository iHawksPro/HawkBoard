package com.paletteboard.ime.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.paletteboard.domain.model.GestureSample
import com.paletteboard.domain.model.KeyGeometry
import com.paletteboard.domain.model.KeyKind
import com.paletteboard.domain.model.KeyPressAnimationPreset
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.KeyboardLayout
import com.paletteboard.domain.model.KeyboardMode
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.TouchPoint
import com.paletteboard.engine.theme.DefaultThemes
import com.paletteboard.engine.theme.ThemeManager
import com.paletteboard.util.primaryColor
import com.paletteboard.util.toAndroidColor
import kotlin.math.hypot
import kotlin.math.max

class KeyboardCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    interface InteractionListener {
        fun onKeyTapped(key: KeyboardKeySpec)
        fun onGestureProgress(points: List<TouchPoint>)
        fun onGestureCompleted(sample: GestureSample)
        fun onInteractionFinished()
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var layoutSpec: KeyboardLayout = KeyboardLayout(
        id = "empty",
        mode = KeyboardMode.LETTERS,
        localeTag = "en-US",
        rows = emptyList(),
        supportsGlideTyping = false,
    )
    private var theme: Theme = DefaultThemes.midnightPulse
    private var themeManager: ThemeManager? = null
    private var geometries: List<KeyGeometry> = emptyList()
    private var interactionListener: InteractionListener? = null
    private var activePoints = mutableListOf<TouchPoint>()
    private var activeKeys = mutableListOf<KeyGeometry>()
    private var pressedKeyId: String? = null
    private var downKey: KeyGeometry? = null
    private var gestureMode = false
    private var gestureEligible = false
    private var pressProgress = 0f
    private var pressAnimator: ValueAnimator? = null
    private var averageCharacterKeyWidth = 0f
    private var averageCharacterKeyHeight = 0f

    fun setInteractionListener(listener: InteractionListener) {
        interactionListener = listener
    }

    fun render(layout: KeyboardLayout, keyboardTheme: Theme, manager: ThemeManager) {
        layoutSpec = layout
        theme = keyboardTheme
        themeManager = manager
        recomputeGeometry(width, height)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeGeometry(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        geometries.forEachIndexed { index, geometry ->
            val rowIndex = rowIndexForGeometry(index)
            val style = themeManager?.resolveKeyStyle(theme, geometry.spec, rowIndex) ?: theme.defaultKeyStyle
            val baseRect = RectF(geometry.left, geometry.top, geometry.right, geometry.bottom)
            val isPressed = pressedKeyId == geometry.spec.id
            val preset = theme.animationStyle.keyPressPreset
            val rect = if (isPressed) {
                animatedRect(baseRect, preset, pressProgress)
            } else {
                baseRect
            }
            val radius = context.dp(style.cornerRadiusDp)
            fillPaint.color = style.fill.primaryColor().toAndroidColor()
            borderPaint.color = style.border.color.toAndroidColor()
            borderPaint.strokeWidth = context.dp(style.border.widthDp)

            if (isPressed && preset == KeyPressAnimationPreset.GLOW) {
                val glowRect = RectF(rect).apply {
                    inset(-context.dp(2f) * pressProgress, -context.dp(2f) * pressProgress)
                }
                glowPaint.color = theme.gestureTrailStyle.color.toAndroidColor()
                glowPaint.strokeWidth = context.dp(2f + pressProgress * 2f)
                glowPaint.alpha = (70 + 120 * pressProgress).toInt()
                canvas.drawRoundRect(glowRect, radius + context.dp(2f), radius + context.dp(2f), glowPaint)
            }

            canvas.drawRoundRect(rect, radius, radius, fillPaint)
            if (style.border.widthDp > 0f) {
                canvas.drawRoundRect(rect, radius, radius, borderPaint)
            }

            if (isPressed) {
                pressedPaint.color = style.pressHighlightColor.toAndroidColor()
                pressedPaint.alpha = when (preset) {
                    KeyPressAnimationPreset.POP -> (90 + 70 * pressProgress).toInt()
                    KeyPressAnimationPreset.LIFT -> (80 + 60 * pressProgress).toInt()
                    else -> 110
                }
                canvas.drawRoundRect(rect, radius, radius, pressedPaint)
            }

            labelPaint.color = style.labelColor.toAndroidColor()
            labelPaint.textSize = context.sp(style.labelSizeSp)
            labelPaint.isFakeBoldText = style.fontWeight >= 600
            val baseline = geometry.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2f
            canvas.drawText(geometry.spec.label, geometry.centerX(), baseline, labelPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val point = TouchPoint(event.x, event.y, event.eventTime)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                activePoints = mutableListOf(point)
                activeKeys = mutableListOf()
                gestureMode = false
                downKey = findTapKey(event.x, event.y)
                gestureEligible = layoutSpec.supportsGlideTyping && downKey?.isGlideLetterKey() == true
                pressedKeyId = downKey?.spec?.id
                if (gestureEligible) {
                    downKey?.let(::appendActiveKey)
                }
                animatePress(target = if (pressedKeyId != null) 1f else 0f)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                activePoints += point
                if (!gestureMode && gestureEligible && activePoints.size >= 2) {
                    val first = activePoints.first()
                    val threshold = max(touchSlop.toFloat(), averageCharacterKeyWidth * 0.2f)
                    gestureMode = distance(first, point) > threshold
                }

                if (gestureMode) {
                    val gestureKey = findGestureKey(event.x, event.y)
                    gestureKey?.let(::appendActiveKey)
                    clearPressImmediately()
                    interactionListener?.onGestureProgress(activePoints.toList())
                } else {
                    val hoveredKey = findTapKey(event.x, event.y)
                    if (hoveredKey?.spec?.id != pressedKeyId) {
                        pressedKeyId = hoveredKey?.spec?.id
                        if (pressedKeyId != null) {
                            animatePress(target = 1f)
                        } else {
                            clearPressImmediately()
                        }
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                activePoints += point
                val releaseGestureKey = findGestureKey(event.x, event.y)
                val releaseTapKey = findTapKey(event.x, event.y)
                if (gestureMode) {
                    releaseGestureKey?.let(::appendActiveKey)
                    val characterKeys = activeKeys.filter { it.spec.kind == KeyKind.CHARACTER }
                    if (characterKeys.size > 1) {
                        interactionListener?.onGestureCompleted(
                            GestureSample(
                                points = activePoints.toList(),
                                traversedKeys = characterKeys,
                                allKeys = geometries,
                            ),
                        )
                        clearInteractionState(clearPressedNow = true)
                        return true
                    }
                }

                val tapKey = releaseTapKey ?: downKey
                if (tapKey != null) {
                    interactionListener?.onKeyTapped(tapKey.spec)
                    finishTapInteraction()
                    return true
                }
                clearInteractionState(clearPressedNow = true)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                clearInteractionState(clearPressedNow = true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun recomputeGeometry(width: Int, height: Int) {
        if (width == 0 || height == 0 || layoutSpec.rows.isEmpty()) {
            geometries = emptyList()
            averageCharacterKeyWidth = 0f
            averageCharacterKeyHeight = 0f
            return
        }

        val metrics = theme.layoutMetrics
        val padding = context.dp(metrics.keyboardPaddingDp)
        val keyGap = context.dp(metrics.keyGapDp)
        val rowGap = context.dp(metrics.rowGapDp)
        val availableWidth = width - padding * 2f
        val totalRowWeight = layoutSpec.rows.sumOf { it.heightWeight.toDouble() }.toFloat().coerceAtLeast(1f)
        val availableHeight = height - padding * 2f - rowGap * (layoutSpec.rows.size - 1).coerceAtLeast(0)
        val globalUnitWidth = layoutSpec.rows.minOf { row ->
            val totalKeyWeight = row.keys.sumOf { it.widthWeight.toDouble() }.toFloat().coerceAtLeast(1f)
            (availableWidth - keyGap * (row.keys.size - 1).coerceAtLeast(0)) / totalKeyWeight
        }.coerceAtLeast(1f)

        var currentTop = padding
        val mapped = mutableListOf<KeyGeometry>()

        layoutSpec.rows.forEach { row ->
            val rowHeight = availableHeight * (row.heightWeight / totalRowWeight)
            val totalKeyWeight = row.keys.sumOf { it.widthWeight.toDouble() }.toFloat().coerceAtLeast(1f)
            val rowContentWidth = totalKeyWeight * globalUnitWidth + keyGap * (row.keys.size - 1).coerceAtLeast(0)
            var currentLeft = padding + ((availableWidth - rowContentWidth) / 2f)

            row.keys.forEach { key ->
                val keyWidth = globalUnitWidth * key.widthWeight
                mapped += KeyGeometry(
                    spec = key,
                    left = currentLeft,
                    top = currentTop,
                    right = currentLeft + keyWidth,
                    bottom = currentTop + rowHeight,
                )
                currentLeft += keyWidth + keyGap
            }
            currentTop += rowHeight + rowGap
        }

        geometries = mapped
        val characterKeys = mapped.filter { it.isGlideLetterKey() }
        averageCharacterKeyWidth = characterKeys
            .map { it.right - it.left }
            .average()
            .toFloat()
            .coerceAtLeast(context.dp(32f))
        averageCharacterKeyHeight = characterKeys
            .map { it.bottom - it.top }
            .average()
            .toFloat()
            .coerceAtLeast(context.dp(40f))
    }

    private fun appendActiveKey(geometry: KeyGeometry) {
        if (!geometry.isGlideLetterKey()) return
        if (activeKeys.lastOrNull()?.spec?.id != geometry.spec.id) {
            activeKeys += geometry
        }
    }

    private fun findTapKey(x: Float, y: Float): KeyGeometry? {
        if (geometries.isEmpty()) return null
        val adjustedY = y - context.dp(6f)
        val candidates = geometries.filter { geometry ->
            val expandX = if (geometry.spec.kind == KeyKind.CHARACTER) averageCharacterKeyWidth * 0.16f else context.dp(8f)
            val expandTop = if (geometry.spec.kind == KeyKind.CHARACTER) averageCharacterKeyHeight * 0.18f else context.dp(8f)
            val expandBottom = if (geometry.spec.kind == KeyKind.CHARACTER) averageCharacterKeyHeight * 0.10f else context.dp(8f)
            adjustedXInBounds(x, geometry, expandX) &&
                adjustedY in (geometry.top - expandTop)..(geometry.bottom + expandBottom)
        }

        if (candidates.isNotEmpty()) {
            return candidates.minByOrNull { geometry -> weightedDistanceToKey(x, adjustedY, geometry) }
        }

        return geometries
            .minByOrNull { geometry -> weightedDistanceToKey(x, adjustedY, geometry) }
            ?.takeIf { geometry ->
                weightedDistanceToKey(x, adjustedY, geometry) <= averageCharacterKeyWidth * 0.92f
            }
    }

    private fun findGestureKey(x: Float, y: Float): KeyGeometry? {
        val characterKeys = geometries.filter { it.isGlideLetterKey() }
        if (characterKeys.isEmpty()) return null
        val adjustedY = y - context.dp(8f)
        val candidates = characterKeys.filter { geometry ->
            adjustedXInBounds(x, geometry, averageCharacterKeyWidth * 0.34f) &&
                adjustedY in
                (geometry.top - averageCharacterKeyHeight * 0.25f)..(geometry.bottom + averageCharacterKeyHeight * 0.18f)
        }

        val target = if (candidates.isNotEmpty()) {
            candidates.minByOrNull { geometry -> weightedDistanceToKey(x, adjustedY, geometry) }
        } else {
            characterKeys.minByOrNull { geometry -> weightedDistanceToKey(x, adjustedY, geometry) }
        }

        return target?.takeIf {
            weightedDistanceToKey(x, adjustedY, it) <= averageCharacterKeyWidth * 0.95f
        }
    }

    private fun adjustedXInBounds(
        x: Float,
        geometry: KeyGeometry,
        expandX: Float,
    ): Boolean = x in (geometry.left - expandX)..(geometry.right + expandX)

    private fun weightedDistanceToKey(
        x: Float,
        y: Float,
        geometry: KeyGeometry,
    ): Float {
        val dx = x - geometry.centerX()
        val dy = (y - geometry.centerY()) * 1.12f
        return hypot(dx, dy)
    }

    private fun distance(start: TouchPoint, end: TouchPoint): Float = hypot(start.x - end.x, start.y - end.y)

    private fun finishTapInteraction() {
        activePoints.clear()
        activeKeys.clear()
        gestureMode = false
        gestureEligible = false
        downKey = null
        interactionListener?.onInteractionFinished()
        animatePress(target = 0f, clearOnEnd = true)
    }

    private fun clearInteractionState(clearPressedNow: Boolean) {
        activePoints.clear()
        activeKeys.clear()
        gestureMode = false
        gestureEligible = false
        downKey = null
        if (clearPressedNow) {
            clearPressImmediately()
        }
        interactionListener?.onInteractionFinished()
        invalidate()
    }

    private fun animatePress(target: Float, clearOnEnd: Boolean = false) {
        pressAnimator?.cancel()
        pressAnimator = ValueAnimator.ofFloat(pressProgress, target).apply {
            duration = (theme.animationStyle.durationMs / 1.35f).toLong().coerceAtLeast(70L)
            addUpdateListener { animator ->
                pressProgress = animator.animatedValue as Float
                invalidate()
            }
            if (clearOnEnd) {
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (pressProgress <= 0.01f) {
                                pressedKeyId = null
                                invalidate()
                            }
                        }
                    },
                )
            }
            start()
        }
    }

    private fun clearPressImmediately() {
        pressAnimator?.cancel()
        pressProgress = 0f
        pressedKeyId = null
    }

    private fun animatedRect(
        base: RectF,
        preset: KeyPressAnimationPreset,
        progress: Float,
    ): RectF {
        val rect = RectF(base)
        val insetX = rect.width() * 0.03f * progress
        val insetY = rect.height() * 0.06f * progress
        return when (preset) {
            KeyPressAnimationPreset.NONE -> rect
            KeyPressAnimationPreset.SCALE -> rect.apply { inset(insetX, insetY) }
            KeyPressAnimationPreset.POP -> rect.apply { inset(-insetX * 0.55f, -insetY * 0.35f) }
            KeyPressAnimationPreset.LIFT -> rect.apply { offset(0f, -context.dp(5f) * progress) }
            KeyPressAnimationPreset.GLOW -> rect
            KeyPressAnimationPreset.SLIDE -> rect.apply { offset(0f, context.dp(4f) * progress) }
        }
    }

    private fun rowIndexForGeometry(geometryIndex: Int): Int {
        var runningIndex = 0
        layoutSpec.rows.forEachIndexed { rowIndex, row ->
            val rowEnd = runningIndex + row.keys.size
            if (geometryIndex in runningIndex until rowEnd) return rowIndex
            runningIndex = rowEnd
        }
        return 0
    }
}

private fun KeyGeometry.isGlideLetterKey(): Boolean = spec.kind == KeyKind.CHARACTER &&
    spec.label.firstOrNull()?.isLetter() == true
