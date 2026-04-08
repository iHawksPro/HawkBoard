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
import com.paletteboard.domain.model.ToolbarAction
import com.paletteboard.ui.state.MainUiState

@Composable
fun ToolbarCustomizationScreen(
    uiState: MainUiState,
    onActionEnabledChanged: (ToolbarAction, Boolean) -> Unit,
) {
    val enabledCount = uiState.settings.toolbarItems.count { it.enabled }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroCard(
                title = "Toolbar Customization",
                subtitle = "The top bar is modeled as configurable actions now, which keeps the door open for richer widgets and contextual tools later.",
                overline = "Top Bar",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Enabled",
                        value = enabledCount.toString(),
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Total slots",
                        value = uiState.settings.toolbarItems.size.toString(),
                    )
                }
            }
        }
        item {
            SectionCard(
                title = "Toolbar Actions",
                subtitle = "Leave the essentials visible, then add power tools only if they make the keyboard faster instead of busier.",
            ) {
                uiState.settings.toolbarItems.forEach { item ->
                    SettingSwitchRow(
                        title = item.action.displayName(),
                        subtitle = item.action.description(),
                        checked = item.enabled,
                        onCheckedChange = { enabled ->
                            onActionEnabledChanged(item.action, enabled)
                        },
                    )
                }
            }
        }
        item {
            SectionCard(
                title = "Design Guidance",
                subtitle = "The toolbar should feel curated rather than stuffed with every possible utility.",
            ) {
                TagChip(label = "Fast access")
                TagChip(label = "Contextual tools")
                TagChip(label = "Less clutter")
                Text(
                    text = "A tighter toolbar usually feels more premium on phone screens because it keeps the keyboard visually lighter and easier to scan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun ToolbarAction.displayName(): String = when (this) {
    ToolbarAction.CLIPBOARD -> "Clipboard"
    ToolbarAction.EMOJI -> "Emoji"
    ToolbarAction.THEMES -> "Themes"
    ToolbarAction.SETTINGS -> "Settings"
    ToolbarAction.ONE_HANDED -> "One-handed mode"
    ToolbarAction.TRANSLATE -> "Translate"
    ToolbarAction.CALCULATOR -> "Calculator"
}

private fun ToolbarAction.description(): String = when (this) {
    ToolbarAction.CLIPBOARD -> "Jump into copied text, pinned items, and clipboard history."
    ToolbarAction.EMOJI -> "Open the emoji surface quickly without changing the core layout."
    ToolbarAction.THEMES -> "Switch themes or open the visual customization flow."
    ToolbarAction.SETTINGS -> "Expose keyboard controls without leaving the typing flow for long."
    ToolbarAction.ONE_HANDED -> "Collapse the layout to the left or right for thumb typing."
    ToolbarAction.TRANSLATE -> "Reserve space for fast local translation or rewrite tools later."
    ToolbarAction.CALCULATOR -> "Keep a compact utility surface ready for quick calculations."
}
