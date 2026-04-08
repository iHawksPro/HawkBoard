package com.paletteboard.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val activeThemeId: String = "",
    val keyboardHeightScale: Float = 1f,
    val keySpacingOverrideDp: Float? = null,
    val keyLabelSizeOverrideSp: Float? = null,
    val showNumberRow: Boolean = false,
    val resizeHandleEnabled: Boolean = true,
    val oneHandedMode: OneHandedMode = OneHandedMode.OFF,
    val splitKeyboardEnabled: Boolean = false,
    val autoCapitalization: Boolean = true,
    val autoSpacing: Boolean = true,
    val autoCorrectionEnabled: Boolean = false,
    val suggestionsEnabled: Boolean = true,
    val multilingualLocales: List<String> = listOf("en-US"),
    val gestureSettings: GestureSettings = GestureSettings(),
    val soundPackId: String = "classic",
    val hapticProfileId: String = "balanced",
    val toolbarItems: List<ToolbarItem> = defaultToolbarItems(),
    val privacyMode: PrivacyMode = PrivacyMode.STANDARD,
    val localOnlyTyping: Boolean = true,
    val popupPreviewEnabled: Boolean = true,
    val highContrastMode: Boolean = false,
    val dyslexiaFriendlyMode: Boolean = false,
)

@Serializable
data class ToolbarItem(
    val action: ToolbarAction,
    val enabled: Boolean = true,
)

@Serializable
enum class ToolbarAction {
    CLIPBOARD,
    EMOJI,
    THEMES,
    SETTINGS,
    ONE_HANDED,
    TRANSLATE,
    CALCULATOR,
}

@Serializable
enum class PrivacyMode {
    STANDARD,
    INCOGNITO,
}

@Serializable
enum class OneHandedMode {
    OFF,
    LEFT,
    RIGHT,
}

fun defaultToolbarItems(): List<ToolbarItem> = listOf(
    ToolbarItem(ToolbarAction.CLIPBOARD, enabled = true),
    ToolbarItem(ToolbarAction.EMOJI, enabled = true),
    ToolbarItem(ToolbarAction.THEMES, enabled = true),
    ToolbarItem(ToolbarAction.ONE_HANDED, enabled = false),
    ToolbarItem(ToolbarAction.TRANSLATE, enabled = false),
    ToolbarItem(ToolbarAction.CALCULATOR, enabled = false),
    ToolbarItem(ToolbarAction.SETTINGS, enabled = true),
)
