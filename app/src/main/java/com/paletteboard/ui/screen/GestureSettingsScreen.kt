package com.paletteboard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paletteboard.ui.state.MainUiState

@Composable
fun GestureSettingsScreen(
    uiState: MainUiState,
    onGlideTypingChanged: (Boolean) -> Unit,
    onGestureSensitivityChanged: (Float) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroCard(
                title = "Glide Typing",
                subtitle = "Hawk Board tracks the finger path, compresses it into key geometry, and scores local candidates without sending text off-device.",
                overline = "Gesture Engine",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Status",
                        value = if (uiState.settings.gestureSettings.enabled) "Enabled" else "Disabled",
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Sensitivity",
                        value = "${(uiState.settings.gestureSettings.sensitivity * 100).toInt()}%",
                    )
                }
            }
        }
        item {
            SectionCard(
                title = "Gesture Controls",
                subtitle = "Keep glide typing forgiving enough to feel natural, but not so loose that word paths become noisy.",
            ) {
                SettingSwitchRow(
                    title = "Enable glide typing",
                    subtitle = "Sliding across letters keeps the path trail live and hands the gesture to the decoder on release.",
                    checked = uiState.settings.gestureSettings.enabled,
                    onCheckedChange = onGlideTypingChanged,
                )
                SettingSlider(
                    title = "Sensitivity",
                    value = uiState.settings.gestureSettings.sensitivity,
                    valueRange = 0f..1f,
                    valueLabel = "${(uiState.settings.gestureSettings.sensitivity * 100).toInt()}%",
                    onValueChange = onGestureSensitivityChanged,
                )
            }
        }
        item {
            SectionCard(
                title = "Tuning Notes",
                subtitle = "The current engine is intentionally geometry-first so it stays fast, predictable, and realistic for an MVP keyboard app.",
            ) {
                TagChip(label = "Local decoding")
                TagChip(label = "Visual trail")
                TagChip(label = "Tap typing fallback")
                Text(
                    text = "If gesture accuracy feels off on-device, sensitivity is the safest control to expose first before layering in a more complex language model.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
