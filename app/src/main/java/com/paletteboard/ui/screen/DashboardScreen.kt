package com.paletteboard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paletteboard.ui.state.MainUiState

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    isKeyboardEnabled: Boolean,
    isKeyboardSelected: Boolean,
    onEnableKeyboard: () -> Unit,
    onUseHawkBoardKeyboard: () -> Unit,
    onShowKeyboardPicker: () -> Unit,
    onApplyTheme: (String) -> Unit,
    onOpenThemeLibrary: () -> Unit,
    onOpenThemeBuilder: () -> Unit,
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
                title = dashboardTitle(isKeyboardEnabled, isKeyboardSelected),
                subtitle = dashboardSubtitle(isKeyboardEnabled, isKeyboardSelected),
                overline = "Keyboard Studio",
                fill = uiState.activeTheme.background.fill,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Enabled",
                        value = if (isKeyboardEnabled) "Yes" else "Not yet",
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Selected",
                        value = if (isKeyboardSelected) "Active" else "Waiting",
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Theme",
                        value = uiState.activeTheme.name,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onEnableKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open keyboard settings")
                    }
                    Button(
                        onClick = onUseHawkBoardKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (isKeyboardSelected) "Switch again in picker" else "Switch to Hawk Board")
                    }
                    OutlinedButton(
                        onClick = onShowKeyboardPicker,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Show input picker")
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Active Theme",
                subtitle = "This is the look your keyboard is using right now. Update it live, then jump into the builder for deeper control.",
            ) {
                KeyboardThemePreview(uiState.activeTheme)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TagChip(label = uiState.activeTheme.animationStyle.keyPressPreset.displayName())
                    TagChip(label = uiState.activeTheme.animationStyle.keyboardTransitionPreset.displayName())
                    TagChip(label = if (uiState.settings.showNumberRow) "Number row on" else "Number row off")
                    TagChip(label = if (uiState.settings.gestureSettings.enabled) "Glide on" else "Glide off")
                }
                Button(
                    onClick = onOpenThemeBuilder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Customize active theme")
                }
            }
        }
        item {
            SectionCard(
                title = "Quick Theme Layouts",
                subtitle = "Swap the whole keyboard vibe instantly, then refine colors, spacing, and animations in the builder.",
            ) {
                ThemeQuickPicker(
                    themes = uiState.themes.take(8),
                    activeThemeId = uiState.activeTheme.id,
                    onApplyTheme = onApplyTheme,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onOpenThemeLibrary,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Theme library")
                    }
                    OutlinedButton(
                        onClick = onOpenThemeBuilder,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Theme builder")
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Studio Shortcuts",
                subtitle = "Everything here stays local-first and geared toward fast iteration on the keyboard experience.",
            ) {
                NavigationTile(
                    title = "Theme Library",
                    subtitle = "${uiState.themes.size} themes ready to browse, duplicate, and apply.",
                    icon = Icons.Rounded.Palette,
                    onClick = onOpenThemeLibrary,
                    actionLabel = "Browse",
                )
                NavigationTile(
                    title = "Theme Builder",
                    subtitle = "Adjust shape, spacing, label sizing, animation, and gesture trail styling.",
                    icon = Icons.Rounded.Tune,
                    onClick = onOpenThemeBuilder,
                    actionLabel = "Create",
                )
                NavigationTile(
                    title = "Import and Export",
                    subtitle = "Save JSON backups, share designs, or paste in a theme payload from another device.",
                    icon = Icons.Rounded.FileUpload,
                    onClick = onOpenImportExport,
                    actionLabel = "Share",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TagChip(label = "Local-only typing")
                    TagChip(label = "Custom animations")
                    TagChip(label = "Deep theming")
                }
                Text(
                    text = "Hawk Board is set up like a personal keyboard studio, not just a single fixed keyboard skin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun dashboardTitle(
    isKeyboardEnabled: Boolean,
    isKeyboardSelected: Boolean,
): String = when {
    isKeyboardSelected -> "Hawk Board is ready to type."
    isKeyboardEnabled -> "One more step and Hawk Board is live."
    else -> "Set up Hawk Board in under a minute."
}

private fun dashboardSubtitle(
    isKeyboardEnabled: Boolean,
    isKeyboardSelected: Boolean,
): String = when {
    isKeyboardSelected -> "The keyboard is enabled and selected. Now the fun part is building the perfect look and feel."
    isKeyboardEnabled -> "The IME is enabled already. Open the picker and switch to Hawk Board anywhere you type."
    else -> "Enable the keyboard first, then use the input picker to switch to it from any text field."
}
