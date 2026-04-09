package com.paletteboard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.ThemeMotionPreset
import com.paletteboard.ui.state.MainUiState

@Composable
fun ThemeLibraryScreen(
    uiState: MainUiState,
    onApplyTheme: (String) -> Unit,
    onEditTheme: (Theme) -> Unit,
    onDuplicateTheme: (Theme) -> Unit,
    onDeleteTheme: (String) -> Unit,
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
                title = "Theme Library",
                subtitle = "Presets seed the experience. Custom themes become the user's portable design collection.",
                overline = "Theme Control",
                fill = uiState.activeTheme.background.fill,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Total",
                        value = uiState.themes.size.toString(),
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Active",
                        value = uiState.activeTheme.name,
                    )
                }
                NavigationTile(
                    title = "Import and export",
                    subtitle = "Move themes between devices or keep a backup library of your favorites.",
                    icon = Icons.Rounded.FileUpload,
                    onClick = onOpenImportExport,
                    actionLabel = "Open",
                )
            }
        }

        items(uiState.themes, key = { it.id }) { theme ->
            SectionCard(
                title = theme.name,
                subtitle = when {
                    theme.id == uiState.activeTheme.id -> "Currently active"
                    theme.isPreset -> "Preset theme"
                    else -> "Saved custom theme"
                },
            ) {
                KeyboardThemePreview(theme)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TagChip(label = if (theme.isPreset) "Preset" else "Custom")
                    TagChip(label = theme.animationStyle.keyPressPreset.displayName())
                    TagChip(label = theme.animationStyle.keyboardTransitionPreset.displayName())
                    if (theme.animationStyle.themeMotionPreset != ThemeMotionPreset.NONE) {
                        TagChip(label = theme.animationStyle.themeMotionPreset.displayName())
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onApplyTheme(theme.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (theme.id == uiState.activeTheme.id) "Theme is active" else "Apply theme")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onEditTheme(theme) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (theme.isPreset) "Customize" else "Edit")
                        }
                        OutlinedButton(
                            onClick = { onDuplicateTheme(theme) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Duplicate")
                        }
                    }
                    if (!theme.isPreset) {
                        OutlinedButton(
                            onClick = { onDeleteTheme(theme.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Delete custom theme")
                        }
                    }
                }
            }
        }
    }
}
