package com.paletteboard.ui.state

import com.paletteboard.BuildConfig
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.UserSettings
import com.paletteboard.engine.theme.DefaultThemes
import com.paletteboard.updater.AppUpdateUiState

data class MainUiState(
    val themes: List<Theme> = emptyList(),
    val activeTheme: Theme = DefaultThemes.midnightPulse,
    val draftTheme: Theme = DefaultThemes.midnightPulse.copy(
        id = "draft",
        name = "Draft Theme",
        isPreset = false,
    ),
    val settings: UserSettings = UserSettings(),
    val importBuffer: String = "",
    val appUpdate: AppUpdateUiState = AppUpdateUiState(currentVersionLabel = BuildConfig.VERSION_NAME),
    val isReady: Boolean = false,
)
