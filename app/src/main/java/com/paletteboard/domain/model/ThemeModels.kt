package com.paletteboard.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Theme(
    val id: String,
    val name: String,
    val author: String = "Local",
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPreset: Boolean = false,
    val background: KeyboardBackground = KeyboardBackground(),
    val layoutMetrics: LayoutMetrics = LayoutMetrics(),
    val defaultKeyStyle: KeyStyle = KeyStyle(),
    val functionalKeyStyle: KeyStyle = KeyStyle(
        fill = FillStyle(solidColor = ColorToken(0xFF3A4455)),
        labelColor = ColorToken(0xFFF3F6FB),
    ),
    val spacebarStyle: KeyStyle = KeyStyle(
        fill = FillStyle(solidColor = ColorToken(0xFF233042)),
        labelColor = ColorToken(0xFFF3F6FB),
        cornerRadiusDp = 20f,
    ),
    val enterKeyStyle: KeyStyle = KeyStyle(
        fill = FillStyle(solidColor = ColorToken(0xFF0D9488)),
        labelColor = ColorToken(0xFFFFFFFF),
    ),
    val shiftKeyStyle: KeyStyle = KeyStyle(
        fill = FillStyle(solidColor = ColorToken(0xFF334155)),
        labelColor = ColorToken(0xFFFFFFFF),
    ),
    val backspaceKeyStyle: KeyStyle = KeyStyle(
        fill = FillStyle(solidColor = ColorToken(0xFF334155)),
        labelColor = ColorToken(0xFFFFFFFF),
    ),
    val popupStyle: PopupStyle = PopupStyle(),
    val gestureTrailStyle: GestureTrailStyle = GestureTrailStyle(),
    val animationStyle: AnimationStyle = AnimationStyle(),
    val toolbarStyle: ToolbarStyle = ToolbarStyle(),
    val rowStyles: List<RowStyleOverride> = emptyList(),
    val perKeyStyles: List<KeyStyleOverride> = emptyList(),
)

@Serializable
data class KeyStyle(
    val fill: FillStyle = FillStyle(solidColor = ColorToken(0xFF1F2937)),
    val labelColor: ColorToken = ColorToken(0xFFFFFFFF),
    val iconColor: ColorToken = ColorToken(0xFFFFFFFF),
    val border: BorderSpec = BorderSpec(),
    val shadow: ShadowSpec = ShadowSpec(),
    val shape: KeyShapeStyle = KeyShapeStyle.ROUNDED,
    val cornerRadiusDp: Float = 16f,
    val labelSizeSp: Float = 20f,
    val fontFamily: FontFamilyToken = FontFamilyToken.SYSTEM,
    val fontWeight: Int = 500,
    val horizontalPaddingDp: Float = 4f,
    val verticalPaddingDp: Float = 6f,
    val pressHighlightColor: ColorToken = ColorToken(0x66FFFFFF),
    val pressScale: Float = 0.98f,
    val elevationDp: Float = 0f,
)

@Serializable
data class KeyboardBackground(
    val fill: FillStyle = FillStyle(solidColor = ColorToken(0xFF0F172A)),
    val imageUri: String? = null,
    val alpha: Float = 1f,
    val blurRadiusDp: Float = 0f,
    val translucency: Float = 0f,
)

@Serializable
data class FillStyle(
    val solidColor: ColorToken? = null,
    val gradient: GradientSpec? = null,
    val alpha: Float = 1f,
)

@Serializable
data class GradientSpec(
    val colors: List<ColorToken> = emptyList(),
    val angleDegrees: Float = 90f,
)

@Serializable
data class BorderSpec(
    val color: ColorToken = ColorToken(0x00000000),
    val widthDp: Float = 0f,
)

@Serializable
data class ShadowSpec(
    val color: ColorToken = ColorToken(0x55000000),
    val blurRadiusDp: Float = 0f,
    val offsetXDp: Float = 0f,
    val offsetYDp: Float = 0f,
)

@Serializable
data class PopupStyle(
    val fill: FillStyle = FillStyle(solidColor = ColorToken(0xFFE2E8F0)),
    val labelColor: ColorToken = ColorToken(0xFF0F172A),
    val border: BorderSpec = BorderSpec(ColorToken(0x1A0F172A), 1f),
)

@Serializable
data class GestureTrailStyle(
    val color: ColorToken = ColorToken(0xFF38BDF8),
    val thicknessDp: Float = 5f,
    val alpha: Float = 0.95f,
)

@Serializable
data class AnimationStyle(
    val keyPressPreset: KeyPressAnimationPreset = KeyPressAnimationPreset.SCALE,
    val keyboardTransitionPreset: KeyboardTransitionPreset = KeyboardTransitionPreset.FADE_SLIDE,
    val durationMs: Int = 140,
)

@Serializable
data class ToolbarStyle(
    val fill: FillStyle = FillStyle(solidColor = ColorToken(0xFF111827)),
    val labelColor: ColorToken = ColorToken(0xFFE5E7EB),
    val chipCornerRadiusDp: Float = 14f,
)

@Serializable
data class LayoutMetrics(
    val keyboardPaddingDp: Float = 6f,
    val keyGapDp: Float = 6f,
    val rowGapDp: Float = 6f,
    val rowHeightMultiplier: Float = 1f,
)

@Serializable
data class RowStyleOverride(
    val rowIndex: Int,
    val keyStyle: KeyStyle,
)

@Serializable
data class KeyStyleOverride(
    val keyId: String,
    val keyStyle: KeyStyle,
)

@Serializable
data class ThemePreset(
    val id: String,
    val themeId: String,
    val category: String,
    val featured: Boolean = false,
)

@Serializable
data class ThemeExportFormat(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val exportedBy: String = "Hawk Board",
    val theme: Theme,
)

@Serializable
data class SoundPack(
    val id: String,
    val name: String,
    val keyPressAsset: String? = null,
    val spaceAsset: String? = null,
    val deleteAsset: String? = null,
    val volume: Float = 0.45f,
    val pitchVariance: Float = 0.06f,
)

@Serializable
data class HapticProfile(
    val id: String,
    val name: String,
    val durationMs: Int = 12,
    val amplitude: Int = 90,
    val waveform: List<Long> = emptyList(),
)

@Serializable
data class ColorToken(
    val argb: Long,
)

@Serializable
enum class KeyShapeStyle {
    ROUNDED,
    PILL,
    SQUARED,
    BUBBLE,
    GLASS,
}

@Serializable
enum class FontFamilyToken {
    SYSTEM,
    SANS,
    SERIF,
    MONO,
    DYSLEXIA_FRIENDLY,
}

@Serializable
enum class KeyPressAnimationPreset {
    NONE,
    SCALE,
    POP,
    LIFT,
    GLOW,
    SLIDE,
}

@Serializable
enum class KeyboardTransitionPreset {
    NONE,
    FADE_SLIDE,
    RISE,
    ZOOM,
    WAVE,
}
