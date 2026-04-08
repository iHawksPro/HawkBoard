package com.paletteboard.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class KeyboardLayout(
    val id: String,
    val mode: KeyboardMode,
    val localeTag: String,
    val rows: List<KeyRow>,
    val supportsGlideTyping: Boolean = true,
    val supportsSplit: Boolean = false,
)

@Serializable
data class KeyRow(
    val keys: List<KeyboardKeySpec>,
    val heightWeight: Float = 1f,
)

@Serializable
data class KeyboardKeySpec(
    val id: String,
    val label: String,
    val commitText: String? = null,
    val code: Int = 0,
    val kind: KeyKind = KeyKind.CHARACTER,
    val widthWeight: Float = 1f,
    val popupChars: List<String> = emptyList(),
    val specialStyleTarget: SpecialKeyStyleTarget? = null,
)

data class KeyGeometry(
    val spec: KeyboardKeySpec,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
    fun centerX(): Float = (left + right) / 2f
    fun centerY(): Float = (top + bottom) / 2f
}

data class TouchPoint(
    val x: Float,
    val y: Float,
    val eventTime: Long,
)

data class GestureSample(
    val points: List<TouchPoint>,
    val traversedKeys: List<KeyGeometry>,
    val allKeys: List<KeyGeometry>,
)

data class GestureCandidate(
    val word: String,
    val score: Float,
)

@Serializable
data class GestureSettings(
    val enabled: Boolean = true,
    val sensitivity: Float = 0.5f,
    val trailEnabled: Boolean = true,
    val gestureDeleteEnabled: Boolean = false,
)

@Serializable
enum class KeyboardMode {
    LETTERS,
    SYMBOLS,
    EMOJI,
}

@Serializable
enum class KeyKind {
    CHARACTER,
    FUNCTION,
    MODE_SWITCH,
    EMOJI,
    CLIPBOARD,
    ACTION,
}

@Serializable
enum class SpecialKeyStyleTarget {
    DEFAULT,
    FUNCTION,
    SHIFT,
    ENTER,
    BACKSPACE,
    SPACEBAR,
}

object KeyCodes {
    const val SHIFT = -1
    const val BACKSPACE = -5
    const val SPACE = 32
    const val ENTER = 10
    const val MODE_SYMBOLS = -100
    const val MODE_LETTERS = -101
    const val MODE_EMOJI = -102
    const val CLIPBOARD = -103
}
