package com.paletteboard.util

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import com.paletteboard.domain.model.ColorToken
import com.paletteboard.domain.model.FillStyle

fun ColorToken.toComposeColor(): Color = Color(argb)

fun ColorToken.toAndroidColor(): Int = argb.toInt()

fun FillStyle.primaryColor(): ColorToken = gradient?.colors?.firstOrNull() ?: solidColor ?: ColorToken(0xFF444444)

fun FillStyle.androidColors(): List<Int> = gradient?.colors?.map { it.toAndroidColor() }?.takeIf { it.isNotEmpty() }
    ?: listOf(primaryColor().toAndroidColor())

fun blendArgb(start: Int, end: Int, fraction: Float): Int {
    val clamped = fraction.coerceIn(0f, 1f)
    val inverse = 1f - clamped
    return AndroidColor.argb(
        (AndroidColor.alpha(start) * inverse + AndroidColor.alpha(end) * clamped).toInt(),
        (AndroidColor.red(start) * inverse + AndroidColor.red(end) * clamped).toInt(),
        (AndroidColor.green(start) * inverse + AndroidColor.green(end) * clamped).toInt(),
        (AndroidColor.blue(start) * inverse + AndroidColor.blue(end) * clamped).toInt(),
    )
}
