package com.paletteboard.ui.screen

import com.paletteboard.domain.model.KeyboardTransitionPreset
import com.paletteboard.domain.model.KeyPressAnimationPreset
import com.paletteboard.domain.model.ThemeMotionPreset

fun KeyPressAnimationPreset.displayName(): String = when (this) {
    KeyPressAnimationPreset.NONE -> "None"
    KeyPressAnimationPreset.SCALE -> "Scale"
    KeyPressAnimationPreset.POP -> "Pop"
    KeyPressAnimationPreset.LIFT -> "Lift"
    KeyPressAnimationPreset.GLOW -> "Glow"
    KeyPressAnimationPreset.SLIDE -> "Slide"
    KeyPressAnimationPreset.FLASH -> "Flash"
    KeyPressAnimationPreset.SINK -> "Sink"
    KeyPressAnimationPreset.BLOOM -> "Bloom"
}

fun KeyboardTransitionPreset.displayName(): String = when (this) {
    KeyboardTransitionPreset.NONE -> "None"
    KeyboardTransitionPreset.FADE_SLIDE -> "Fade Slide"
    KeyboardTransitionPreset.RISE -> "Rise"
    KeyboardTransitionPreset.ZOOM -> "Zoom"
    KeyboardTransitionPreset.WAVE -> "Wave"
}

fun ThemeMotionPreset.displayName(): String = when (this) {
    ThemeMotionPreset.NONE -> "Static"
    ThemeMotionPreset.AURORA -> "Aurora"
    ThemeMotionPreset.SHIMMER -> "Shimmer"
    ThemeMotionPreset.PULSE -> "Pulse"
    ThemeMotionPreset.SPECTRUM -> "Spectrum"
}
