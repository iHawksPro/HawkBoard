package com.paletteboard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paletteboard.domain.model.KeyboardTransitionPreset
import com.paletteboard.domain.model.KeyPressAnimationPreset
import com.paletteboard.domain.model.ThemeMotionPreset
import com.paletteboard.ui.state.MainUiState

private val effectPressOptions = listOf(
    KeyPressAnimationPreset.NONE,
    KeyPressAnimationPreset.SCALE,
    KeyPressAnimationPreset.POP,
    KeyPressAnimationPreset.LIFT,
    KeyPressAnimationPreset.GLOW,
    KeyPressAnimationPreset.SLIDE,
    KeyPressAnimationPreset.FLASH,
    KeyPressAnimationPreset.SINK,
    KeyPressAnimationPreset.BLOOM,
)

private val effectTransitionOptions = listOf(
    KeyboardTransitionPreset.NONE,
    KeyboardTransitionPreset.FADE_SLIDE,
    KeyboardTransitionPreset.RISE,
    KeyboardTransitionPreset.ZOOM,
    KeyboardTransitionPreset.WAVE,
)

private val effectMotionOptions = listOf(
    ThemeMotionPreset.NONE,
    ThemeMotionPreset.AURORA,
    ThemeMotionPreset.SHIMMER,
    ThemeMotionPreset.PULSE,
    ThemeMotionPreset.SPECTRUM,
)

private val effectSoundPacks = listOf("classic", "soft", "arcade")
private val effectHapticProfiles = listOf("light", "balanced", "strong")

@Composable
fun EffectsStudioScreen(
    uiState: MainUiState,
    onKeyPressAnimationChanged: (KeyPressAnimationPreset) -> Unit,
    onKeyboardTransitionAnimationChanged: (KeyboardTransitionPreset) -> Unit,
    onThemeMotionChanged: (ThemeMotionPreset) -> Unit,
    onAnimationDurationChanged: (Int) -> Unit,
    onPopupPreviewChanged: (Boolean) -> Unit,
    onSoundPackChanged: (String) -> Unit,
    onHapticProfileChanged: (String) -> Unit,
    onSaveTheme: () -> Unit,
    onReset: () -> Unit,
) {
    val draftTheme = uiState.draftTheme
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroCard(
                title = "Effects Studio",
                subtitle = "Tune how Hawk Board feels on every press. These controls are focused on motion, response, and feedback rather than raw color design.",
                overline = "Premium Feel",
                fill = draftTheme.background.fill,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Press",
                        value = draftTheme.animationStyle.keyPressPreset.displayName(),
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Motion",
                        value = draftTheme.animationStyle.themeMotionPreset.displayName(),
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Feedback",
                        value = uiState.settings.soundPackId.replaceFirstChar(Char::titlecase),
                    )
                }
                KeyboardThemePreview(draftTheme)
            }
        }
        item {
            SectionCard(
                title = "Press Effects",
                subtitle = "Choose how each tap lands. Hawk Board now has a wider set of distinct press behaviors instead of one generic response.",
            ) {
                OptionChipRow(
                    title = "Key press style",
                    selected = draftTheme.animationStyle.keyPressPreset,
                    options = effectPressOptions,
                    labelFor = { it.displayName() },
                    onSelected = onKeyPressAnimationChanged,
                )
                Text(
                    text = pressEffectDescription(draftTheme.animationStyle.keyPressPreset),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(
                title = "Keyboard Motion",
                subtitle = "Blend key press response with stage transitions and ambient theme motion so the keyboard feels designed as one system.",
            ) {
                OptionChipRow(
                    title = "Keyboard transition",
                    selected = draftTheme.animationStyle.keyboardTransitionPreset,
                    options = effectTransitionOptions,
                    labelFor = { it.displayName() },
                    onSelected = onKeyboardTransitionAnimationChanged,
                )
                OptionChipRow(
                    title = "Ambient motion",
                    selected = draftTheme.animationStyle.themeMotionPreset,
                    options = effectMotionOptions,
                    labelFor = { it.displayName() },
                    onSelected = onThemeMotionChanged,
                )
                SettingSlider(
                    title = "Animation speed",
                    value = draftTheme.animationStyle.durationMs.toFloat(),
                    valueRange = 80f..260f,
                    valueLabel = "${draftTheme.animationStyle.durationMs} ms",
                    onValueChange = { onAnimationDurationChanged(it.toInt()) },
                )
            }
        }
        item {
            SectionCard(
                title = "Feedback Packs",
                subtitle = "Professional keyboards feel premium because touch, sound, and motion are tuned together rather than separately.",
            ) {
                OptionChipRow(
                    title = "Sound pack",
                    selected = uiState.settings.soundPackId,
                    options = effectSoundPacks,
                    labelFor = { it.replaceFirstChar(Char::titlecase) },
                    onSelected = onSoundPackChanged,
                )
                OptionChipRow(
                    title = "Haptic profile",
                    selected = uiState.settings.hapticProfileId,
                    options = effectHapticProfiles,
                    labelFor = { it.replaceFirstChar(Char::titlecase) },
                    onSelected = onHapticProfileChanged,
                )
                SettingSwitchRow(
                    title = "Popup previews",
                    subtitle = "Show enlarged key popups so every press effect feels clearer and more intentional.",
                    checked = uiState.settings.popupPreviewEnabled,
                    onCheckedChange = onPopupPreviewChanged,
                )
            }
        }
        item {
            SectionCard(
                title = "Studio Actions",
                subtitle = "Save the current effect stack to your theme, or roll back if you pushed the keyboard too far.",
            ) {
                Button(
                    onClick = onSaveTheme,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save effect setup")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Reset")
                    }
                    OutlinedButton(
                        onClick = onSaveTheme,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Apply live")
                    }
                }
            }
        }
    }
}

private fun pressEffectDescription(preset: KeyPressAnimationPreset): String = when (preset) {
    KeyPressAnimationPreset.NONE -> "No animation, just a clean press highlight for a minimal look."
    KeyPressAnimationPreset.SCALE -> "A compact inward press that feels crisp and controlled."
    KeyPressAnimationPreset.POP -> "A light outward pop that makes taps feel energetic."
    KeyPressAnimationPreset.LIFT -> "Lifts the key upward slightly for a floating premium effect."
    KeyPressAnimationPreset.GLOW -> "Adds a soft highlight ring without moving the key much."
    KeyPressAnimationPreset.SLIDE -> "Drops the key a bit lower on touch for a playful motion cue."
    KeyPressAnimationPreset.FLASH -> "Brightens the key instantly for a fast, sharp response."
    KeyPressAnimationPreset.SINK -> "Presses the key inward and downward like a deeper mechanical switch."
    KeyPressAnimationPreset.BLOOM -> "Pushes a wider accent glow outward for a more dramatic premium tap."
}
