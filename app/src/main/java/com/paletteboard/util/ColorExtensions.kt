package com.paletteboard.util

import androidx.compose.ui.graphics.Color
import com.paletteboard.domain.model.ColorToken
import com.paletteboard.domain.model.FillStyle

fun ColorToken.toComposeColor(): Color = Color(argb)

fun ColorToken.toAndroidColor(): Int = argb.toInt()

fun FillStyle.primaryColor(): ColorToken = gradient?.colors?.firstOrNull() ?: solidColor ?: ColorToken(0xFF444444)
