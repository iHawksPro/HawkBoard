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
import com.paletteboard.ui.state.MainUiState

@Composable
fun ImportExportScreen(
    uiState: MainUiState,
    onImportBufferChanged: (String) -> Unit,
    onImportTheme: () -> Unit,
    onExportActiveTheme: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroCard(
                title = "Import and Export",
                subtitle = "Themes serialize to JSON so you can inspect them, back them up, share them, and move them between devices without a cloud dependency.",
                overline = "Theme Portability",
                fill = uiState.activeTheme.background.fill,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Active theme",
                        value = uiState.activeTheme.name,
                    )
                    StatPill(
                        modifier = Modifier.weight(1f),
                        label = "Payload",
                        value = if (uiState.importBuffer.isBlank()) "Empty" else "Loaded",
                    )
                }
            }
        }
        item {
            SectionCard(
                title = "Transfer Actions",
                subtitle = "Export the current theme into JSON or paste a payload below and import it as a local theme.",
            ) {
                Button(
                    onClick = onExportActiveTheme,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Export active theme to JSON")
                }
                OutlinedButton(
                    onClick = onImportTheme,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Import JSON payload")
                }
            }
        }
        item {
            SectionCard(
                title = "Theme JSON",
                subtitle = "This text area is intentionally transparent and editable so advanced users can inspect or tweak payloads directly.",
            ) {
                OutlinedTextField(
                    value = uiState.importBuffer,
                    onValueChange = onImportBufferChanged,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 16,
                    label = { Text("Theme export payload") },
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
