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

private val soundPacks = listOf("classic", "soft", "arcade")
private val hapticProfiles = listOf("light", "balanced", "strong")

@Composable
fun SoundHapticSettingsScreen(
    uiState: MainUiState,
    onSoundPackChanged: (String) -> Unit,
    onHapticProfileChanged: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroCard(
                title = "Sound and Haptics",
                subtitle = "Audio and vibration matter most when they feel intentional, light, and low-latency rather than loud for the sake of it.",
                overline = "Feedback Tuning",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Sound",
                        value = uiState.settings.soundPackId.replaceFirstChar(Char::titlecase),
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Haptic",
                        value = uiState.settings.hapticProfileId.replaceFirstChar(Char::titlecase),
                    )
                }
            }
        }
        item {
            SectionCard(
                title = "Sound Packs",
                subtitle = "Pick a pack that matches the visual style of your keyboard instead of competing with it.",
            ) {
                OptionChipRow(
                    title = "Typing sound",
                    selected = uiState.settings.soundPackId,
                    options = soundPacks,
                    labelFor = { it.replaceFirstChar(Char::titlecase) },
                    onSelected = onSoundPackChanged,
                )
                Text(
                    text = soundPackDescription(uiState.settings.soundPackId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(
                title = "Haptic Profiles",
                subtitle = "Vibration should reinforce confidence on key press without making the keyboard feel heavy or fatiguing.",
            ) {
                OptionChipRow(
                    title = "Haptic feel",
                    selected = uiState.settings.hapticProfileId,
                    options = hapticProfiles,
                    labelFor = { it.replaceFirstChar(Char::titlecase) },
                    onSelected = onHapticProfileChanged,
                )
                Text(
                    text = hapticDescription(uiState.settings.hapticProfileId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun soundPackDescription(soundPackId: String): String = when (soundPackId) {
    "classic" -> "Classic stays crisp and familiar, which is usually the safest default for daily typing."
    "soft" -> "Soft keeps the feedback present without turning every session into a noisy one."
    "arcade" -> "Arcade is more playful and works best when the theme itself is bright or high energy."
    else -> "Choose the pack that feels best over longer typing sessions."
}

private fun hapticDescription(hapticProfileId: String): String = when (hapticProfileId) {
    "light" -> "Light keeps the keyboard airy and is ideal if you prefer subtle confirmation."
    "balanced" -> "Balanced aims for everyday comfort and is the best general-purpose profile."
    "strong" -> "Strong adds more punch, which some users like on larger phones or louder themes."
    else -> "Pick the haptic profile that matches your typing style."
}
