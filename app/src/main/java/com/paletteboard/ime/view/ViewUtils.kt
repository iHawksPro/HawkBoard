package com.paletteboard.ime.view

import android.content.Context
import android.util.TypedValue

fun Context.dp(value: Float): Float = value * resources.displayMetrics.density

fun Context.sp(value: Float): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_SP,
    value,
    resources.displayMetrics,
)
