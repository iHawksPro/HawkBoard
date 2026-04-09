package com.paletteboard.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.dp
import com.paletteboard.domain.model.FillStyle
import com.paletteboard.domain.model.KeyStyle
import com.paletteboard.domain.model.Theme
import com.paletteboard.util.primaryColor
import com.paletteboard.util.toComposeColor

@Composable
fun HeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    fill: FillStyle? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val heroBackgroundColor = fill?.primaryColor()?.toComposeColor() ?: MaterialTheme.colorScheme.primary
    val heroForegroundColor = readableContentColor(heroBackgroundColor)
    val heroBrush = if (fill == null) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.secondary,
            ),
        )
    } else {
        null
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .let { baseModifier ->
                    if (heroBrush != null) {
                        baseModifier.background(heroBrush, MaterialTheme.shapes.large)
                    } else {
                        baseModifier.fillStyleBackground(fill, MaterialTheme.shapes.large)
                    }
                }
                .padding(20.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides heroForegroundColor) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    overline?.let {
                        Text(
                            text = it.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = LocalContentColor.current.copy(alpha = 0.78f),
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = LocalContentColor.current,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = LocalContentColor.current.copy(alpha = 0.88f),
                    )
                }
                content()
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.secondary,
                            ),
                        ),
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = LocalContentColor.current.copy(alpha = 0.14f),
        contentColor = LocalContentColor.current,
        border = BorderStroke(1.dp, LocalContentColor.current.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = LocalContentColor.current.copy(alpha = 0.74f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
            )
        }
    }
}

@Composable
fun TagChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        modifier = modifier,
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = false,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        ),
    )
}

@Composable
fun SettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            TagChip(label = valueLabel)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
fun NavigationTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "Open",
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun ColorSwatchPicker(
    title: String,
    selected: Long,
    options: List<Long>,
    onSelected: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(options) { option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(option))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                            },
                            shape = CircleShape,
                        )
                        .clickable { onSelected(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.86f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> OptionChipRow(
    title: String,
    selected: T,
    options: List<T>,
    labelFor: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    label = { Text(labelFor(option)) },
                )
            }
        }
    }
}

@Composable
fun ThemeQuickPicker(
    themes: List<Theme>,
    activeThemeId: String,
    onApplyTheme: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(themes, key = { it.id }) { theme ->
            val cardShape = MaterialTheme.shapes.medium
            Card(
                modifier = Modifier.size(width = 176.dp, height = 206.dp),
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillStyleBackground(theme.background.fill, cardShape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = theme.name,
                            color = theme.defaultKeyStyle.labelColor.toComposeColor(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (theme.id == activeThemeId) {
                            TagChip(label = "Live")
                        }
                    }
                    PreviewSwatches(theme)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(4) {
                            PreviewKey(
                                style = theme.defaultKeyStyle,
                                label = "A",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PreviewKey(theme.functionalKeyStyle, "Fn", modifier = Modifier.weight(1f))
                        PreviewKey(theme.spacebarStyle, "Space", modifier = Modifier.weight(2f))
                    }
                    FilledTonalButton(
                        onClick = { onApplyTheme(theme.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (theme.id == activeThemeId) "Applied" else "Use theme")
                    }
                }
            }
        }
    }
}

@Composable
fun KeyboardThemePreview(theme: Theme) {
    val cardShape = MaterialTheme.shapes.medium
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillStyleBackground(theme.background.fill, cardShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = theme.name,
                    color = theme.defaultKeyStyle.labelColor.toComposeColor(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TagChip(label = if (theme.isPreset) "Preset" else "Custom")
                    TagChip(label = theme.animationStyle.keyPressPreset.displayName())
                }
            }
            PreviewSwatches(theme)
            PreviewRow(
                listOf(
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                ),
            )
            PreviewRow(
                listOf(
                    theme.defaultKeyStyle,
                    theme.functionalKeyStyle,
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                    theme.defaultKeyStyle,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewKey(theme.functionalKeyStyle, label = "?123", modifier = Modifier.weight(1.2f))
                PreviewKey(theme.functionalKeyStyle, label = "🌐", modifier = Modifier.weight(0.92f))
                PreviewKey(theme.spacebarStyle, label = "English", modifier = Modifier.weight(4f))
                PreviewKey(theme.defaultKeyStyle, label = ".", modifier = Modifier.weight(0.9f))
                PreviewKey(theme.enterKeyStyle, label = "↵", modifier = Modifier.weight(1.2f))
            }
        }
    }
}

@Composable
private fun PreviewSwatches(theme: Theme) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Swatch(theme.background.fill.primaryColor().toComposeColor(), label = "BG")
        Swatch(theme.defaultKeyStyle.fill.primaryColor().toComposeColor(), label = "Key")
        Swatch(theme.functionalKeyStyle.fill.primaryColor().toComposeColor(), label = "Fn")
        Swatch(theme.enterKeyStyle.fill.primaryColor().toComposeColor(), label = "Go")
    }
}

@Composable
private fun Swatch(
    color: Color,
    label: String,
) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (color.luminance() > 0.45f) Color.Black else Color.White,
            )
        }
    }
}

@Composable
private fun PreviewRow(styles: List<KeyStyle>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        styles.forEach { style ->
            PreviewKey(style = style, label = "A", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PreviewKey(
    style: KeyStyle,
    label: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(style.cornerRadiusDp.dp)
    Box(
        modifier = modifier
            .aspectRatio(1.35f)
            .clip(shape)
            .fillStyleBackground(style.fill, shape)
            .border(
                width = style.border.widthDp.dp.coerceAtLeast(0.dp),
                color = style.border.color.toComposeColor().copy(alpha = 0.7f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = style.labelColor.toComposeColor(),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun Modifier.fillStyleBackground(
    fill: FillStyle?,
    shape: Shape,
): Modifier {
    val safeFill = fill ?: FillStyle()
    val brush = safeFill.gradient?.colors
        ?.map { it.toComposeColor().copy(alpha = safeFill.alpha) }
        ?.takeIf { it.isNotEmpty() }
        ?.let(Brush::linearGradient)

    return if (brush != null) {
        background(brush = brush, shape = shape)
    } else {
        background(
            color = safeFill.primaryColor().toComposeColor().copy(alpha = safeFill.alpha),
            shape = shape,
        )
    }
}

private fun readableContentColor(background: Color): Color = if (background.luminance() > 0.6f) {
    Color(0xFF0F172A)
} else {
    Color.White
}
