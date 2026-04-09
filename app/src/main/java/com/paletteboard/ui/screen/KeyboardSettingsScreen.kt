package com.paletteboard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paletteboard.ui.state.MainUiState

@Composable
fun KeyboardSettingsScreen(
    uiState: MainUiState,
    onKeyboardHeightScaleChanged: (Float) -> Unit,
    onNumberRowChanged: (Boolean) -> Unit,
    onAutoCapitalizationChanged: (Boolean) -> Unit,
    onSuggestionsChanged: (Boolean) -> Unit,
    onAutoCorrectionChanged: (Boolean) -> Unit,
    onPopupPreviewChanged: (Boolean) -> Unit,
    onOpenToolbarSettings: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenImportExport: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroCard(
                title = "Keyboard Settings",
                subtitle = "These preferences stay in DataStore so the IME can respond quickly without bouncing through heavier storage paths.",
                overline = "Typing Setup",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Height",
                        value = "${(uiState.settings.keyboardHeightScale * 100).toInt()}%",
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Number row",
                        value = if (uiState.settings.showNumberRow) "On" else "Off",
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Typing",
                        value = if (uiState.settings.popupPreviewEnabled) "Preview on" else "Preview off",
                    )
                }
            }
        }
        item {
            SectionCard(
                title = "Layout",
                subtitle = "Keyboard height is one of the highest-impact comfort controls for different devices and hand sizes.",
            ) {
                SettingSlider(
                    title = "Keyboard height",
                    value = uiState.settings.keyboardHeightScale,
                    valueRange = 0.8f..1.4f,
                    valueLabel = "${(uiState.settings.keyboardHeightScale * 100).toInt()}%",
                    onValueChange = onKeyboardHeightScaleChanged,
                )
            }
        }
        item {
            SectionCard(
                title = "Core Toggles",
                subtitle = "Keep the highest-impact layout controls close to the top so the keyboard stays easy to tune.",
            ) {
                SettingSwitchRow(
                    title = "Number row",
                    subtitle = "Keep digits visible on the main layout for faster mixed text entry.",
                    checked = uiState.settings.showNumberRow,
                    onCheckedChange = onNumberRowChanged,
                )
                SettingSwitchRow(
                    title = "Auto-capitalization",
                    subtitle = "Only capitalize at the start of a field, after a newline, or after sentence punctuation.",
                    checked = uiState.settings.autoCapitalization,
                    onCheckedChange = onAutoCapitalizationChanged,
                )
                SettingSwitchRow(
                    title = "Suggestions",
                    subtitle = "Show prediction chips above the keyboard while you type.",
                    checked = uiState.settings.suggestionsEnabled,
                    onCheckedChange = onSuggestionsChanged,
                )
                SettingSwitchRow(
                    title = "Auto-correction framework",
                    subtitle = "Keep the correction pipeline armed for future smarter replacements and suggestion ranking.",
                    checked = uiState.settings.autoCorrectionEnabled,
                    onCheckedChange = onAutoCorrectionChanged,
                )
                SettingSwitchRow(
                    title = "Popup previews",
                    subtitle = "Show enlarged popups on press and reveal long-press alternates more clearly.",
                    checked = uiState.settings.popupPreviewEnabled,
                    onCheckedChange = onPopupPreviewChanged,
                )
            }
        }
        item {
            SectionCard(
                title = "Deeper Control",
                subtitle = "These sections shape how Hawk Board behaves beyond the basic layout layer.",
            ) {
                NavigationTile(
                    title = "Toolbar customization",
                    subtitle = "Choose which shortcuts and utilities appear above the keys.",
                    icon = Icons.Rounded.Tune,
                    onClick = onOpenToolbarSettings,
                )
                NavigationTile(
                    title = "Sound and haptics",
                    subtitle = "Swap between typing sound packs and haptic profiles to match your style.",
                    icon = Icons.Rounded.GraphicEq,
                    onClick = onOpenSoundSettings,
                )
                NavigationTile(
                    title = "Import and export",
                    subtitle = "Move themes around quickly or keep a manual JSON backup of your favorites.",
                    icon = Icons.Rounded.UploadFile,
                    onClick = onOpenImportExport,
                )
            }
        }
    }
}
