package com.paletteboard.engine.theme

import com.paletteboard.data.repository.SettingsRepository
import com.paletteboard.data.repository.ThemeRepository
import com.paletteboard.domain.model.KeyStyle
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.SpecialKeyStyleTarget
import com.paletteboard.domain.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ThemeManager(
    private val themeRepository: ThemeRepository,
    private val settingsRepository: SettingsRepository,
) {
    val activeTheme: Flow<Theme> = combine(
        settingsRepository.settings,
        themeRepository.observeThemes(),
    ) { settings, themes ->
        themes.firstOrNull { it.id == settings.activeThemeId }
            ?: themes.firstOrNull()
            ?: DefaultThemes.midnightPulse
    }

    suspend fun setActiveTheme(themeId: String) {
        settingsRepository.update { settings ->
            settings.copy(activeThemeId = themeId)
        }
    }

    fun resolveKeyStyle(theme: Theme, key: KeyboardKeySpec, rowIndex: Int): KeyStyle {
        theme.perKeyStyles.firstOrNull { it.keyId == key.id }?.let { return it.keyStyle }
        theme.rowStyles.firstOrNull { it.rowIndex == rowIndex }?.let { return it.keyStyle }

        return when (key.specialStyleTarget ?: SpecialKeyStyleTarget.DEFAULT) {
            SpecialKeyStyleTarget.SHIFT -> theme.shiftKeyStyle
            SpecialKeyStyleTarget.ENTER -> theme.enterKeyStyle
            SpecialKeyStyleTarget.BACKSPACE -> theme.backspaceKeyStyle
            SpecialKeyStyleTarget.SPACEBAR -> theme.spacebarStyle
            SpecialKeyStyleTarget.FUNCTION -> theme.functionalKeyStyle
            SpecialKeyStyleTarget.DEFAULT -> theme.defaultKeyStyle
        }
    }
}
