package com.paletteboard.ui.screen

import com.paletteboard.domain.model.KeyboardTransitionPreset
import com.paletteboard.domain.model.KeyPressAnimationPreset

fun KeyPressAnimationPreset.displayName(): String = when (this) {
    KeyPressAnimationPreset.NONE -> "None"
    KeyPressAnimationPreset.SCALE -> "Scale"
    KeyPressAnimationPreset.POP -> "Pop"
    KeyPressAnimationPreset.LIFT -> "Lift"
    KeyPressAnimationPreset.GLOW -> "Glow"
    KeyPressAnimationPreset.SLIDE -> "Slide"
}

fun KeyboardTransitionPreset.displayName(): String = when (this) {
    KeyboardTransitionPreset.NONE -> "None"
    KeyboardTransitionPreset.FADE_SLIDE -> "Fade Slide"
    KeyboardTransitionPreset.RISE -> "Rise"
    KeyboardTransitionPreset.ZOOM -> "Zoom"
    KeyboardTransitionPreset.WAVE -> "Wave"
}
