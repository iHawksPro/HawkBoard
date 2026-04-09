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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paletteboard.domain.model.KeyboardTransitionPreset
import com.paletteboard.domain.model.KeyPressAnimationPreset
import com.paletteboard.domain.model.ThemeMotionPreset
import com.paletteboard.ui.state.MainUiState
import com.paletteboard.util.primaryColor

private val ThemeColorOptions = listOf(
    0xFF0F172AL,
    0xFF1E293BL,
    0xFF450A0AL,
    0xFF7F1D1DL,
    0xFF0F766EL,
    0xFF15803DL,
    0xFF65A30DL,
    0xFFF97316L,
    0xFFF59E0BL,
    0xFFE11D48L,
    0xFFEC4899L,
    0xFF2563EBL,
    0xFF06B6D4L,
    0xFF7C3AEDL,
    0xFFA855F7L,
    0xFFF8FAFCL,
    0xFFFFFFFFL,
)

@Composable
fun ThemeBuilderScreen(
    uiState: MainUiState,
    onNameChanged: (String) -> Unit,
    onCornerRadiusChanged: (Float) -> Unit,
    onSpacingChanged: (Float) -> Unit,
    onLabelSizeChanged: (Float) -> Unit,
    onPrimaryColorChanged: (Long) -> Unit,
    onFunctionColorChanged: (Long) -> Unit,
    onBackgroundColorChanged: (Long) -> Unit,
    onKeyPressAnimationChanged: (KeyPressAnimationPreset) -> Unit,
    onKeyboardTransitionAnimationChanged: (KeyboardTransitionPreset) -> Unit,
    onThemeMotionChanged: (ThemeMotionPreset) -> Unit,
    onAnimationDurationChanged: (Int) -> Unit,
    onSaveTheme: () -> Unit,
    onReset: () -> Unit,
    onRandomize: () -> Unit,
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
                title = draftTheme.name,
                subtitle = "This is a live editing surface for the exact theme model the IME reads. Every change here should feel instant and visual.",
                overline = "Theme Builder",
                fill = draftTheme.background.fill,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Corners",
                        value = "${draftTheme.defaultKeyStyle.cornerRadiusDp.toInt()} dp",
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Spacing",
                        value = "${draftTheme.layoutMetrics.keyGapDp.toInt()} dp",
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Press",
                        value = draftTheme.animationStyle.keyPressPreset.displayName(),
                    )
                }
                KeyboardThemePreview(draftTheme)
            }
        }
        item {
            SectionCard(
                title = "Identity",
                subtitle = "Give the theme a clear name so it feels like a design you can come back to, share, and evolve later.",
            ) {
                OutlinedTextField(
                    value = draftTheme.name,
                    onValueChange = onNameChanged,
                    label = { Text("Theme name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(
                    text = "Presets become editable variants automatically, so you can customize aggressively without losing the originals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(
                title = "Layout Feel",
                subtitle = "These controls shape how the keyboard reads at a glance before users notice individual colors.",
            ) {
                SettingSlider(
                    title = "Corner radius",
                    value = draftTheme.defaultKeyStyle.cornerRadiusDp,
                    valueRange = 4f..30f,
                    valueLabel = "${draftTheme.defaultKeyStyle.cornerRadiusDp.toInt()} dp",
                    onValueChange = onCornerRadiusChanged,
                )
                SettingSlider(
                    title = "Key spacing",
                    value = draftTheme.layoutMetrics.keyGapDp,
                    valueRange = 2f..16f,
                    valueLabel = "${draftTheme.layoutMetrics.keyGapDp.toInt()} dp",
                    onValueChange = onSpacingChanged,
                )
                SettingSlider(
                    title = "Label size",
                    value = draftTheme.defaultKeyStyle.labelSizeSp,
                    valueRange = 14f..26f,
                    valueLabel = "${draftTheme.defaultKeyStyle.labelSizeSp.toInt()} sp",
                    onValueChange = onLabelSizeChanged,
                )
            }
        }
        item {
            SectionCard(
                title = "Color System",
                subtitle = "Tune the surface, the standard keys, and the functional keys so the whole keyboard feels intentional and easy to read.",
            ) {
                ColorSwatchPicker(
                    title = "Background",
                    selected = draftTheme.background.fill.primaryColor().argb,
                    options = ThemeColorOptions,
                    onSelected = onBackgroundColorChanged,
                )
                ColorSwatchPicker(
                    title = "Standard keys",
                    selected = draftTheme.defaultKeyStyle.fill.primaryColor().argb,
                    options = ThemeColorOptions,
                    onSelected = onPrimaryColorChanged,
                )
                ColorSwatchPicker(
                    title = "Function keys",
                    selected = draftTheme.functionalKeyStyle.fill.primaryColor().argb,
                    options = ThemeColorOptions,
                    onSelected = onFunctionColorChanged,
                )
            }
        }
        item {
            SectionCard(
                title = "Motion",
                subtitle = "These animation presets give the keyboard personality without pushing the IME into jank territory.",
            ) {
                OptionChipRow(
                    title = "Key press animation",
                    selected = draftTheme.animationStyle.keyPressPreset,
                    options = listOf(
                        KeyPressAnimationPreset.NONE,
                        KeyPressAnimationPreset.SCALE,
                        KeyPressAnimationPreset.POP,
                        KeyPressAnimationPreset.LIFT,
                        KeyPressAnimationPreset.GLOW,
                        KeyPressAnimationPreset.SLIDE,
                        KeyPressAnimationPreset.FLASH,
                        KeyPressAnimationPreset.SINK,
                        KeyPressAnimationPreset.BLOOM,
                    ),
                    labelFor = { it.displayName() },
                    onSelected = onKeyPressAnimationChanged,
                )
                OptionChipRow(
                    title = "Theme motion",
                    selected = draftTheme.animationStyle.themeMotionPreset,
                    options = listOf(
                        ThemeMotionPreset.NONE,
                        ThemeMotionPreset.AURORA,
                        ThemeMotionPreset.SHIMMER,
                        ThemeMotionPreset.PULSE,
                        ThemeMotionPreset.SPECTRUM,
                    ),
                    labelFor = { it.displayName() },
                    onSelected = onThemeMotionChanged,
                )
                OptionChipRow(
                    title = "Keyboard transition",
                    selected = draftTheme.animationStyle.keyboardTransitionPreset,
                    options = listOf(
                        KeyboardTransitionPreset.NONE,
                        KeyboardTransitionPreset.FADE_SLIDE,
                        KeyboardTransitionPreset.RISE,
                        KeyboardTransitionPreset.ZOOM,
                        KeyboardTransitionPreset.WAVE,
                    ),
                    labelFor = { it.displayName() },
                    onSelected = onKeyboardTransitionAnimationChanged,
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
                title = "Builder Actions",
                subtitle = "Save the current variant, roll the dice for new ideas, or reset back to the live theme you started from.",
            ) {
                Button(
                    onClick = onSaveTheme,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save theme")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onRandomize,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Randomize")
                    }
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}
