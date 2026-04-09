package com.paletteboard.ime.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import com.paletteboard.domain.model.KeyGeometry
import com.paletteboard.domain.model.KeyCodes
import com.paletteboard.domain.model.KeyKind
import com.paletteboard.domain.model.KeyPressAnimationPreset
import com.paletteboard.domain.model.KeyShapeStyle
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.KeyboardLayout
import com.paletteboard.domain.model.KeyboardMode
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.ThemeMotionPreset
import com.paletteboard.engine.theme.DefaultThemes
import com.paletteboard.engine.theme.ThemeManager
import com.paletteboard.ime.controller.ShiftState
import com.paletteboard.util.androidColors
import com.paletteboard.util.blendArgb
import com.paletteboard.util.primaryColor
import com.paletteboard.util.toAndroidColor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

class KeyboardCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    interface InteractionListener {
        fun onKeyTapped(key: KeyboardKeySpec)
        fun onInteractionFinished()
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val popupPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val popupBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var layoutSpec = KeyboardLayout(
        id = "empty",
        mode = com.paletteboard.domain.model.KeyboardMode.LETTERS,
        localeTag = "en-US",
        rows = emptyList(),
        supportsGlideTyping = false,
    )
    private var theme: Theme = DefaultThemes.midnightPulse
    private var themeManager: ThemeManager? = null
    private var shiftState: ShiftState = ShiftState.OFF
    private var popupPreviewEnabled = true
    private var geometries: List<KeyGeometry> = emptyList()
    private var interactionListener: InteractionListener? = null
    private var pressedKeyId: String? = null
    private var downKey: KeyGeometry? = null
    private var handledOnDownKeyId: String? = null
    private var repeatingKeyId: String? = null
    private var longPressKeyId: String? = null
    private var popupOverrideText: String? = null
    private var pressProgress = 0f
    private var pressAnimator: ValueAnimator? = null
    private var averageCharacterKeyWidth = 0f
    private var averageCharacterKeyHeight = 0f

    private val repeatBackspaceRunnable = object : Runnable {
        override fun run() {
            val key = downKey ?: return
            if (key.spec.code != KeyCodes.BACKSPACE || key.spec.id != repeatingKeyId) return
            interactionListener?.onKeyTapped(key.spec)
            postDelayed(this, BACKSPACE_REPEAT_INTERVAL_MS)
        }
    }

    private val longPressRunnable = object : Runnable {
        override fun run() {
            val key = downKey ?: return
            if (key.spec.id != longPressKeyId) return
            val overrideKey = longPressVariantFor(key.spec) ?: return
            handledOnDownKeyId = key.spec.id
            popupOverrideText = previewTextForKey(overrideKey)
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            interactionListener?.onKeyTapped(overrideKey)
            invalidate()
        }
    }

    fun setInteractionListener(listener: InteractionListener) {
        interactionListener = listener
    }

    fun render(
        layout: KeyboardLayout,
        keyboardTheme: Theme,
        manager: ThemeManager,
        shiftState: ShiftState,
        popupPreviewEnabled: Boolean,
    ) {
        layoutSpec = layout
        theme = keyboardTheme
        themeManager = manager
        this.shiftState = shiftState
        this.popupPreviewEnabled = popupPreviewEnabled
        recomputeGeometry(width, height)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeGeometry(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()
        geometries.forEachIndexed { index, geometry ->
            val rowIndex = rowIndexForGeometry(index)
            val baseStyle = themeManager?.resolveKeyStyle(theme, geometry.spec, rowIndex) ?: theme.defaultKeyStyle
            val style = if (geometry.spec.code == KeyCodes.SHIFT && shiftState != ShiftState.OFF) {
                baseStyle.copy(
                    fill = theme.popupStyle.fill,
                    labelColor = theme.popupStyle.labelColor,
                    iconColor = theme.popupStyle.labelColor,
                    border = theme.popupStyle.border,
                )
            } else {
                baseStyle
            }
            val pressed = geometry.spec.id == pressedKeyId
            val keyRect = visualRect(RectF(geometry.left, geometry.top, geometry.right, geometry.bottom), geometry.spec, rowIndex)
            val preset = theme.animationStyle.keyPressPreset
            val renderedRect = if (pressed) animatedSurfaceRect(keyRect, preset, pressProgress) else keyRect
            val radius = keyCornerRadius(renderedRect, geometry.spec, style)
            drawShadowSurface(canvas, renderedRect, radius, style, pressed)
            configureKeySurface(style, renderedRect, index, now)
            if (pressed && preset == KeyPressAnimationPreset.FLASH) {
                fillPaint.color = blendArgb(fillPaint.color, 0xFFFFFFFF.toInt(), 0.2f + pressProgress * 0.18f)
                fillPaint.shader = null
            }
            borderPaint.color = style.border.color.toAndroidColor()
            borderPaint.strokeWidth = context.dp(style.border.widthDp.coerceAtLeast(0.8f))
            if (pressed && (preset == KeyPressAnimationPreset.GLOW || preset == KeyPressAnimationPreset.BLOOM)) {
                drawGlowAccent(
                    canvas = canvas,
                    rect = renderedRect,
                    radius = radius,
                    accentColor = style.pressHighlightColor.toAndroidColor(),
                    bloom = preset == KeyPressAnimationPreset.BLOOM,
                )
            }
            canvas.drawRoundRect(renderedRect, radius, radius, fillPaint)
            if (style.border.widthDp > 0f || geometry.spec.kind != KeyKind.CHARACTER) {
                canvas.drawRoundRect(renderedRect, radius, radius, borderPaint)
            }
            if (pressed) {
                pressedPaint.color = style.pressHighlightColor.toAndroidColor()
                pressedPaint.alpha = when (preset) {
                    KeyPressAnimationPreset.NONE -> 88
                    KeyPressAnimationPreset.GLOW -> 78
                    KeyPressAnimationPreset.BLOOM -> 70
                    KeyPressAnimationPreset.FLASH -> 150
                    KeyPressAnimationPreset.SINK -> 92
                    else -> 118
                }
                canvas.drawRoundRect(renderedRect, radius, radius, pressedPaint)
            }
            drawKeyContent(canvas, renderedRect, geometry.spec, style)
            drawHintIfNeeded(canvas, renderedRect, geometry.spec, style)
        }
        drawPopupPreview(canvas)
        if (theme.animationStyle.themeMotionPreset != ThemeMotionPreset.NONE) {
            postInvalidateOnAnimation()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downKey = findTapKey(event.x, event.y)
                pressedKeyId = downKey?.spec?.id
                handledOnDownKeyId = null
                popupOverrideText = null
                animatePress(if (pressedKeyId != null) 1f else 0f)
                startRepeatIfNeeded(downKey)
                startLongPressIfNeeded(downKey)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val hovered = findTapKey(event.x, event.y)
                if (hovered?.spec?.id != pressedKeyId) {
                    pressedKeyId = hovered?.spec?.id
                    popupOverrideText = null
                    if (pressedKeyId != null) animatePress(1f) else clearPress()
                }
                if (hovered != null) downKey = hovered
                if (repeatingKeyId != null && hovered?.spec?.id != repeatingKeyId) {
                    cancelRepeat(resetHandled = false)
                }
                if (longPressKeyId != null && hovered?.spec?.id != longPressKeyId) {
                    cancelLongPress(resetHandled = false)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val tapKey = findTapKey(event.x, event.y) ?: downKey
                if (tapKey != null && tapKey.spec.id != handledOnDownKeyId) {
                    interactionListener?.onKeyTapped(tapKey.spec)
                }
                finishInteraction()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                finishInteraction()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        cancelRepeat()
        cancelLongPress()
        super.onDetachedFromWindow()
    }

    private fun recomputeGeometry(width: Int, height: Int) {
        if (width == 0 || height == 0 || layoutSpec.rows.isEmpty()) {
            geometries = emptyList()
            averageCharacterKeyWidth = 0f
            averageCharacterKeyHeight = 0f
            return
        }
        val padding = context.dp((theme.layoutMetrics.keyboardPaddingDp - 0.3f).coerceAtLeast(4f))
        val keyGap = context.dp((theme.layoutMetrics.keyGapDp - 1.15f).coerceAtLeast(3.4f))
        val rowGap = context.dp((theme.layoutMetrics.rowGapDp - 0.3f).coerceAtLeast(4.9f))
        val availableWidth = width - padding * 2f
        val totalRowWeight = layoutSpec.rows.sumOf { it.heightWeight.toDouble() }.toFloat().coerceAtLeast(1f)
        val availableHeight = height - padding * 2f - rowGap * (layoutSpec.rows.size - 1).coerceAtLeast(0)
        val widestRowUnitWeight = layoutSpec.rows.maxOf { row ->
            row.leadingInsetWeight + row.trailingInsetWeight + row.keys.sumOf { it.widthWeight.toDouble() }.toFloat()
        }.coerceAtLeast(1f)
        val widestRowKeyCount = layoutSpec.rows.maxOf { it.keys.size }
        val baseUnitWidth = (availableWidth - keyGap * (widestRowKeyCount - 1).coerceAtLeast(0)) / widestRowUnitWeight
        var currentTop = padding
        val mapped = mutableListOf<KeyGeometry>()
        layoutSpec.rows.forEach { row ->
            val rowHeight = availableHeight * (row.heightWeight / totalRowWeight)
            val rowUnitWeight = row.leadingInsetWeight + row.trailingInsetWeight + row.keys.sumOf { it.widthWeight.toDouble() }.toFloat()
            val rowContentWidth =
                (rowUnitWeight * baseUnitWidth) +
                (keyGap * (row.keys.size - 1).coerceAtLeast(0))
            var rowLeft = padding +
                ((availableWidth - rowContentWidth) / 2f) +
                (row.leadingInsetWeight * baseUnitWidth)
            row.keys.forEach { key ->
                val keyWidth = baseUnitWidth * key.widthWeight
                mapped += KeyGeometry(key, rowLeft, currentTop, rowLeft + keyWidth, currentTop + rowHeight)
                rowLeft += keyWidth + keyGap
            }
            currentTop += rowHeight + rowGap
        }
        geometries = mapped
        val charKeys = mapped.filter { it.spec.kind == KeyKind.CHARACTER }
        averageCharacterKeyWidth = charKeys.map { it.right - it.left }.average().toFloat().coerceAtLeast(context.dp(36f))
        averageCharacterKeyHeight = charKeys.map { it.bottom - it.top }.average().toFloat().coerceAtLeast(context.dp(44f))
    }

    private fun findTapKey(x: Float, y: Float): KeyGeometry? {
        if (geometries.isEmpty()) return null
        val adjustedY = y - context.dp(4f)
        val candidates = geometries.filter { geometry ->
            val expandX = if (geometry.spec.kind == KeyKind.CHARACTER) averageCharacterKeyWidth * 0.3f else context.dp(12f)
            val expandY = if (geometry.spec.kind == KeyKind.CHARACTER) averageCharacterKeyHeight * 0.24f else context.dp(10f)
            x in (geometry.left - expandX)..(geometry.right + expandX) &&
                adjustedY in (geometry.top - expandY)..(geometry.bottom + expandY)
        }
        if (candidates.isNotEmpty()) {
            return candidates.minByOrNull { weightedDistanceToKey(x, adjustedY, it) }
        }
        return geometries.minByOrNull { weightedDistanceToKey(x, adjustedY, it) }
    }

    private fun weightedDistanceToKey(x: Float, y: Float, geometry: KeyGeometry): Float {
        val dx = x - geometry.centerX()
        val dy = (y - geometry.centerY()) * 1.04f
        return hypot(dx, dy)
    }

    private fun visualRect(base: RectF, key: KeyboardKeySpec, rowIndex: Int): RectF {
        val rect = RectF(base)
        val isBottomRow = rowIndex == layoutSpec.rows.lastIndex
        val horizontalInset = when {
            key.code == KeyCodes.SPACE -> 1.05f
            key.code == KeyCodes.MODE_EMOJI -> 1.45f
            key.code == KeyCodes.SHIFT || key.code == KeyCodes.BACKSPACE || key.code == KeyCodes.ENTER -> 1.45f
            key.kind == KeyKind.CHARACTER -> 0.65f
            else -> 1.4f
        }
        val verticalInset = when {
            isBottomRow -> 3.3f
            key.kind == KeyKind.CHARACTER -> 1.5f
            else -> 2.45f
        }
        rect.inset(context.dp(horizontalInset), context.dp(verticalInset))
        return rect
    }

    private fun configureKeySurface(
        style: com.paletteboard.domain.model.KeyStyle,
        rect: RectF,
        index: Int,
        now: Long,
    ) {
        val fillColors = style.fill.androidColors()
        if (fillColors.size > 1) {
            fillPaint.shader = LinearGradient(
                rect.left,
                rect.top,
                rect.left,
                rect.bottom,
                fillColors.toIntArray(),
                null,
                Shader.TileMode.CLAMP,
            )
            return
        }

        fillPaint.shader = null
        fillPaint.color = animatedKeyColor(
            fillColors.first(),
            theme.enterKeyStyle.fill.primaryColor().toAndroidColor(),
            theme.animationStyle.themeMotionPreset,
            index * 0.19f,
            now,
        )
    }

    private fun drawShadowSurface(
        canvas: Canvas,
        rect: RectF,
        radius: Float,
        style: com.paletteboard.domain.model.KeyStyle,
        pressed: Boolean,
    ) {
        val shadowColor = style.shadow.color.toAndroidColor()
        if (shadowColor == 0 || style.shadow.blurRadiusDp <= 0f) return

        shadowPaint.shader = null
        shadowPaint.color = shadowColor
        shadowPaint.alpha = if (pressed) 34 else 74
        val shadowRect = RectF(rect).apply {
            offset(
                context.dp(style.shadow.offsetXDp),
                context.dp(style.shadow.offsetYDp.coerceAtLeast(1f)) * if (pressed) 0.35f else 1f,
            )
        }
        canvas.drawRoundRect(
            shadowRect,
            radius + context.dp(0.8f),
            radius + context.dp(0.8f),
            shadowPaint,
        )
    }

    private fun drawKeyContent(
        canvas: Canvas,
        rect: RectF,
        key: KeyboardKeySpec,
        style: com.paletteboard.domain.model.KeyStyle,
    ) {
        when (key.code) {
            KeyCodes.SHIFT -> drawShiftIcon(canvas, rect, style.labelColor.toAndroidColor(), shiftState == ShiftState.LOCKED)
            KeyCodes.BACKSPACE -> drawBackspaceIcon(canvas, rect, style.labelColor.toAndroidColor())
            KeyCodes.ENTER -> {
                if (key.label.length <= 2) {
                    drawEnterIcon(canvas, rect, style.labelColor.toAndroidColor())
                } else {
                    drawLabel(canvas, rect, key, style)
                }
            }
            KeyCodes.LANGUAGE_SWITCH -> drawLanguageIcon(canvas, rect, style.labelColor.toAndroidColor())
            else -> drawLabel(canvas, rect, key, style)
        }
    }

    private fun drawLabel(
        canvas: Canvas,
        rect: RectF,
        key: KeyboardKeySpec,
        style: com.paletteboard.domain.model.KeyStyle,
    ) {
        labelPaint.color = style.labelColor.toAndroidColor()
        labelPaint.textSize = context.sp(labelSizeFor(key, style.labelSizeSp))
        labelPaint.isFakeBoldText = style.fontWeight >= 600
        val baseline = rect.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2f
        canvas.drawText(displayLabelForKey(key), rect.centerX(), baseline, labelPaint)
    }

    private fun drawHintIfNeeded(
        canvas: Canvas,
        rect: RectF,
        key: KeyboardKeySpec,
        style: com.paletteboard.domain.model.KeyStyle,
    ) {
        val hint = key.hintLabel ?: return
        if (layoutSpec.mode == KeyboardMode.LETTERS && key.kind == KeyKind.CHARACTER) return
        if (key.kind != KeyKind.CHARACTER && key.id != "space") return
        hintPaint.color = style.labelColor.toAndroidColor()
        hintPaint.alpha = if (key.id == "space") 140 else 176
        hintPaint.textSize = context.sp(if (key.id == "space") 8.5f else 9.5f)
        hintPaint.isFakeBoldText = true
        canvas.drawText(hint, rect.right - context.dp(6f), rect.top + context.dp(if (key.id == "space") 13f else 11f), hintPaint)
    }

    private fun drawPopupPreview(canvas: Canvas) {
        if (!popupPreviewEnabled) return
        val text = popupOverrideText ?: return
        val keyId = pressedKeyId ?: return
        val geometry = geometries.firstOrNull { it.spec.id == keyId } ?: return
        if (geometry.spec.code == KeyCodes.SHIFT || geometry.spec.code == KeyCodes.BACKSPACE || geometry.spec.code == KeyCodes.ENTER) return
        val rowIndex = rowIndexForGeometry(geometries.indexOf(geometry))
        val rect = visualRect(RectF(geometry.left, geometry.top, geometry.right, geometry.bottom), geometry.spec, rowIndex)
        labelPaint.color = theme.popupStyle.labelColor.toAndroidColor()
        labelPaint.textSize = context.sp(max(24f, theme.defaultKeyStyle.labelSizeSp + 5f))
        labelPaint.isFakeBoldText = true
        val bubbleWidth = max(rect.width() * 1.08f, labelPaint.measureText(text) + context.dp(28f))
        val bubbleHeight = max(rect.height() * 1.18f, context.dp(62f))
        val bubble = RectF(
            (rect.centerX() - bubbleWidth / 2f).coerceIn(context.dp(4f), width - bubbleWidth - context.dp(4f)),
            max(context.dp(4f), rect.top - bubbleHeight - context.dp(8f)),
            0f,
            0f,
        )
        bubble.right = bubble.left + bubbleWidth
        bubble.bottom = bubble.top + bubbleHeight
        popupPaint.color = theme.popupStyle.fill.primaryColor().toAndroidColor()
        popupBorderPaint.color = theme.popupStyle.border.color.toAndroidColor()
        popupBorderPaint.strokeWidth = context.dp(theme.popupStyle.border.widthDp.coerceAtLeast(1f))
        canvas.drawRoundRect(bubble, context.dp(14f), context.dp(14f), popupPaint)
        canvas.drawRoundRect(bubble, context.dp(14f), context.dp(14f), popupBorderPaint)
        val path = Path().apply {
            val pointerCenter = rect.centerX().coerceIn(bubble.left + context.dp(16f), bubble.right - context.dp(16f))
            moveTo(pointerCenter - context.dp(7f), bubble.bottom - context.dp(2f))
            lineTo(pointerCenter, bubble.bottom + context.dp(9f))
            lineTo(pointerCenter + context.dp(7f), bubble.bottom - context.dp(2f))
            close()
        }
        canvas.drawPath(path, popupPaint)
        canvas.drawPath(path, popupBorderPaint)
        val baseline = bubble.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2f
        canvas.drawText(text, bubble.centerX(), baseline, labelPaint)
    }

    private fun drawLanguageIcon(canvas: Canvas, rect: RectF, color: Int) {
        val radius = minOf(rect.width(), rect.height()) * 0.19f
        iconPaint.color = color
        iconPaint.strokeWidth = context.dp(1.7f)
        canvas.drawCircle(rect.centerX(), rect.centerY(), radius, iconPaint)
        canvas.drawLine(rect.centerX(), rect.centerY() - radius, rect.centerX(), rect.centerY() + radius, iconPaint)
        canvas.drawLine(rect.centerX() - radius, rect.centerY(), rect.centerX() + radius, rect.centerY(), iconPaint)
    }

    private fun drawEnterIcon(canvas: Canvas, rect: RectF, color: Int) {
        val left = rect.left + rect.width() * 0.28f
        val right = rect.right - rect.width() * 0.24f
        val midY = rect.centerY() + context.dp(1f)
        val top = rect.top + rect.height() * 0.3f
        val path = Path().apply {
            moveTo(right, top)
            lineTo(right, midY)
            lineTo(left + context.dp(8f), midY)
            moveTo(left, midY)
            lineTo(left + context.dp(7f), midY - context.dp(6f))
            moveTo(left, midY)
            lineTo(left + context.dp(7f), midY + context.dp(6f))
        }
        iconPaint.color = color
        iconPaint.strokeWidth = context.dp(2f)
        canvas.drawPath(path, iconPaint)
    }

    private fun drawShiftIcon(canvas: Canvas, rect: RectF, color: Int, locked: Boolean) {
        val cx = rect.centerX()
        val top = rect.top + rect.height() * 0.24f
        val midY = rect.centerY()
        val bottom = rect.bottom - rect.height() * 0.22f
        val wing = rect.width() * 0.16f
        val path = Path().apply {
            moveTo(cx, top)
            lineTo(cx - wing, midY)
            lineTo(cx - wing * 0.42f, midY)
            lineTo(cx - wing * 0.42f, bottom)
            lineTo(cx + wing * 0.42f, bottom)
            lineTo(cx + wing * 0.42f, midY)
            lineTo(cx + wing, midY)
            close()
        }
        iconPaint.color = color
        iconPaint.strokeWidth = context.dp(1.9f)
        canvas.drawPath(path, iconPaint)
        if (locked) {
            canvas.drawLine(rect.left + rect.width() * 0.3f, rect.bottom - rect.height() * 0.16f, rect.right - rect.width() * 0.3f, rect.bottom - rect.height() * 0.16f, iconPaint)
        }
    }

    private fun drawBackspaceIcon(canvas: Canvas, rect: RectF, color: Int) {
        val left = rect.left + rect.width() * 0.22f
        val right = rect.right - rect.width() * 0.2f
        val top = rect.top + rect.height() * 0.32f
        val bottom = rect.bottom - rect.height() * 0.32f
        val path = Path().apply {
            moveTo(left, rect.centerY())
            lineTo(left + rect.width() * 0.16f, top)
            lineTo(right, top)
            lineTo(right, bottom)
            lineTo(left + rect.width() * 0.16f, bottom)
            close()
        }
        iconPaint.color = color
        iconPaint.strokeWidth = context.dp(1.9f)
        canvas.drawPath(path, iconPaint)
        canvas.drawLine(rect.centerX() - rect.width() * 0.07f, rect.centerY() - rect.height() * 0.1f, rect.centerX() + rect.width() * 0.07f, rect.centerY() + rect.height() * 0.1f, iconPaint)
        canvas.drawLine(rect.centerX() - rect.width() * 0.07f, rect.centerY() + rect.height() * 0.1f, rect.centerX() + rect.width() * 0.07f, rect.centerY() - rect.height() * 0.1f, iconPaint)
    }

    private fun finishInteraction() {
        cancelRepeat()
        cancelLongPress()
        downKey = null
        interactionListener?.onInteractionFinished()
        animatePress(0f, clearOnEnd = true)
    }

    private fun animatedSurfaceRect(
        base: RectF,
        preset: KeyPressAnimationPreset,
        progress: Float,
    ): RectF {
        val rect = RectF(base)
        val insetX = rect.width() * 0.035f * progress
        val insetY = rect.height() * 0.06f * progress
        return when (preset) {
            KeyPressAnimationPreset.NONE -> rect
            KeyPressAnimationPreset.SCALE -> rect.apply { inset(insetX, insetY) }
            KeyPressAnimationPreset.POP -> rect.apply { inset(-insetX * 0.55f, -insetY * 0.4f) }
            KeyPressAnimationPreset.LIFT -> rect.apply { offset(0f, -context.dp(4f) * progress) }
            KeyPressAnimationPreset.GLOW -> rect
            KeyPressAnimationPreset.SLIDE -> rect.apply { offset(0f, context.dp(3f) * progress) }
            KeyPressAnimationPreset.FLASH -> rect
            KeyPressAnimationPreset.SINK -> rect.apply {
                inset(insetX * 0.55f, insetY * 0.55f)
                offset(0f, context.dp(2.6f) * progress)
            }
            KeyPressAnimationPreset.BLOOM -> rect.apply { inset(-insetX * 0.8f, -insetY * 0.62f) }
        }
    }

    private fun drawGlowAccent(
        canvas: Canvas,
        rect: RectF,
        radius: Float,
        accentColor: Int,
        bloom: Boolean,
    ) {
        val glowRect = RectF(rect).apply {
            val expand = if (bloom) context.dp(3.2f) else context.dp(2f)
            inset(-expand * pressProgress, -expand * pressProgress)
        }
        popupBorderPaint.color = accentColor
        popupBorderPaint.alpha = if (bloom) 150 else 118
        popupBorderPaint.strokeWidth = context.dp(if (bloom) 2.8f else 2f)
        canvas.drawRoundRect(
            glowRect,
            radius + context.dp(if (bloom) 3f else 2f),
            radius + context.dp(if (bloom) 3f else 2f),
            popupBorderPaint,
        )
    }

    private fun animatePress(target: Float, clearOnEnd: Boolean = false) {
        pressAnimator?.cancel()
        pressAnimator = ValueAnimator.ofFloat(pressProgress, target).apply {
            duration = 90L
            addUpdateListener {
                pressProgress = it.animatedValue as Float
                invalidate()
            }
            if (clearOnEnd) {
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (pressProgress <= 0.01f) pressedKeyId = null
                            invalidate()
                        }
                    },
                )
            }
            start()
        }
    }

    private fun clearPress() {
        pressAnimator?.cancel()
        pressProgress = 0f
        pressedKeyId = null
    }

    private fun keyCornerRadius(
        rect: RectF,
        key: KeyboardKeySpec,
        style: com.paletteboard.domain.model.KeyStyle,
    ): Float {
        val styleRadius = context.dp(style.cornerRadiusDp)
        return when (style.shape) {
            KeyShapeStyle.PILL -> rect.height() / 2f
            KeyShapeStyle.SQUARED -> minOf(styleRadius, rect.height() * 0.18f)
            KeyShapeStyle.BUBBLE -> minOf(styleRadius + context.dp(4f), rect.height() * 0.34f)
            KeyShapeStyle.GLASS -> minOf(styleRadius + context.dp(2f), rect.height() * 0.28f)
            KeyShapeStyle.ROUNDED -> when (key.id) {
                "space" -> minOf(styleRadius, rect.height() * 0.24f)
                else -> minOf(styleRadius, rect.height() * 0.24f)
            }
        }
    }

    private fun labelSizeFor(key: KeyboardKeySpec, base: Float): Float = when {
        key.id == "space" -> base - 3.6f
        key.code == KeyCodes.ENTER && key.label.length > 2 -> base - 5.4f
        key.code == KeyCodes.MODE_EMOJI -> base - 0.8f
        key.label.length >= 5 -> base - 3.2f
        key.kind != KeyKind.CHARACTER && key.label.length > 1 -> base - 1.6f
        else -> base
    }.coerceAtLeast(14f)

    private fun displayLabelForKey(key: KeyboardKeySpec): String {
        return if (key.kind == KeyKind.CHARACTER && key.label.length == 1 && shiftState != ShiftState.OFF) {
            key.label.uppercase()
        } else {
            key.label
        }
    }

    private fun previewTextForKey(key: KeyboardKeySpec): String = displayLabelForKey(key)

    private fun animatedKeyColor(baseColor: Int, accentColor: Int, motionPreset: ThemeMotionPreset, seed: Float, now: Long): Int {
        if (motionPreset == ThemeMotionPreset.NONE) return baseColor
        val duration = theme.animationStyle.motionDurationMs.coerceAtLeast(1200)
        val phase = ((now % duration).toFloat() / duration.toFloat()) + seed
        val wave = ((sin(phase * Math.PI * 2) + 1f) / 2f).toFloat()
        return when (motionPreset) {
            ThemeMotionPreset.AURORA -> blendArgb(baseColor, accentColor, 0.14f + wave * 0.2f)
            ThemeMotionPreset.SHIMMER -> blendArgb(baseColor, 0xFFFFFFFF.toInt(), 0.08f + wave * 0.18f)
            ThemeMotionPreset.PULSE -> blendArgb(baseColor, accentColor, wave * 0.18f)
            ThemeMotionPreset.SPECTRUM -> blendArgb(baseColor, accentColor, 0.1f + wave * 0.22f)
            ThemeMotionPreset.NONE -> baseColor
        }
    }

    private fun startRepeatIfNeeded(key: KeyGeometry?) {
        cancelRepeat(resetHandled = false)
        if (key?.spec?.code != KeyCodes.BACKSPACE) return
        handledOnDownKeyId = key.spec.id
        repeatingKeyId = key.spec.id
        interactionListener?.onKeyTapped(key.spec)
        postDelayed(repeatBackspaceRunnable, BACKSPACE_REPEAT_INITIAL_DELAY_MS)
    }

    private fun cancelRepeat(resetHandled: Boolean = true) {
        removeCallbacks(repeatBackspaceRunnable)
        repeatingKeyId = null
        if (resetHandled) handledOnDownKeyId = null
    }

    private fun startLongPressIfNeeded(key: KeyGeometry?) {
        cancelLongPress(resetHandled = false)
        val spec = key?.spec ?: return
        if (spec.code == KeyCodes.BACKSPACE) return
        if (longPressVariantFor(spec) == null) return
        longPressKeyId = spec.id
        postDelayed(longPressRunnable, LONG_PRESS_DELAY_MS)
    }

    private fun cancelLongPress(resetHandled: Boolean = true) {
        removeCallbacks(longPressRunnable)
        longPressKeyId = null
        popupOverrideText = null
        if (resetHandled && repeatingKeyId == null) handledOnDownKeyId = null
    }

    private fun longPressVariantFor(key: KeyboardKeySpec): KeyboardKeySpec? = when {
        key.code == KeyCodes.SPACE -> key.copy(
            id = "${key.id}_language",
            label = "\uD83C\uDF10",
            commitText = null,
            code = KeyCodes.LANGUAGE_SWITCH,
            kind = KeyKind.FUNCTION,
            hintLabel = null,
        )
        key.popupChars.isNotEmpty() -> {
            val alt = key.popupChars.first()
            key.copy(
                id = "${key.id}_alt",
                label = alt,
                commitText = alt,
                code = alt.firstOrNull()?.code ?: key.code,
                hintLabel = null,
            )
        }
        else -> null
    }

    private fun rowIndexForGeometry(index: Int): Int {
        var running = 0
        layoutSpec.rows.forEachIndexed { rowIndex, row ->
            val rowEnd = running + row.keys.size
            if (index in running until rowEnd) return rowIndex
            running = rowEnd
        }
        return 0
    }

    private companion object {
        const val BACKSPACE_REPEAT_INITIAL_DELAY_MS = 325L
        const val BACKSPACE_REPEAT_INTERVAL_MS = 65L
        const val LONG_PRESS_DELAY_MS = 360L
    }
}
