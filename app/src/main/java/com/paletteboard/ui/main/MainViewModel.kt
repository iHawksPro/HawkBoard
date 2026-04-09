package com.paletteboard.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.paletteboard.app.AppContainer
import com.paletteboard.data.repository.SettingsRepository
import com.paletteboard.data.repository.ThemeRepository
import com.paletteboard.domain.model.ColorToken
import com.paletteboard.domain.model.FillStyle
import com.paletteboard.domain.model.KeyboardTransitionPreset
import com.paletteboard.domain.model.KeyPressAnimationPreset
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.ThemeExportFormat
import com.paletteboard.domain.model.ThemeMotionPreset
import com.paletteboard.domain.model.ToolbarAction
import com.paletteboard.engine.theme.DefaultThemes
import com.paletteboard.engine.theme.ThemeManager
import com.paletteboard.ui.state.MainUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class MainViewModel(
    private val themeRepository: ThemeRepository,
    private val settingsRepository: SettingsRepository,
    private val themeManager: ThemeManager,
) : ViewModel() {
    private val draftTheme = MutableStateFlow(newThemeDraft(DefaultThemes.midnightPulse))
    private val importBuffer = MutableStateFlow("")

    val uiState: StateFlow<MainUiState> = combine(
        themeRepository.observeThemes(),
        themeManager.activeTheme,
        settingsRepository.settings,
        draftTheme,
        importBuffer,
    ) { themes, activeTheme, settings, draft, buffer ->
        MainUiState(
            themes = themes,
            activeTheme = activeTheme,
            draftTheme = draft,
            settings = settings,
            importBuffer = buffer,
            isReady = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        viewModelScope.launch {
            val activeTheme = themeManager.activeTheme.first()
            draftTheme.value = newThemeDraft(activeTheme)
        }
    }

    fun applyTheme(themeId: String) {
        viewModelScope.launch {
            themeManager.setActiveTheme(themeId)
        }
    }

    fun editTheme(theme: Theme) {
        draftTheme.value = if (theme.isPreset) {
            newThemeDraft(theme)
        } else {
            theme.copy(updatedAt = System.currentTimeMillis())
        }
    }

    fun duplicateTheme(theme: Theme) {
        draftTheme.value = newThemeDraft(theme)
    }

    fun resetDraftFromActiveTheme() {
        draftTheme.value = newThemeDraft(uiState.value.activeTheme)
    }

    fun saveDraftTheme() {
        viewModelScope.launch {
            val theme = draftTheme.value.copy(
                isPreset = false,
                updatedAt = System.currentTimeMillis(),
            )
            themeRepository.saveTheme(theme)
            themeManager.setActiveTheme(theme.id)
        }
    }

    fun deleteTheme(themeId: String) {
        viewModelScope.launch {
            if (themeId == uiState.value.activeTheme.id) {
                themeManager.setActiveTheme(DefaultThemes.DEFAULT_THEME_ID)
            }
            themeRepository.deleteTheme(themeId)
        }
    }

    fun updateDraftName(name: String) = updateDraft { it.copy(name = name) }

    fun updateDraftCornerRadius(value: Float) = updateDraft { theme ->
        theme.copy(
            defaultKeyStyle = theme.defaultKeyStyle.copy(cornerRadiusDp = value),
            functionalKeyStyle = theme.functionalKeyStyle.copy(cornerRadiusDp = value),
            shiftKeyStyle = theme.shiftKeyStyle.copy(cornerRadiusDp = value),
            enterKeyStyle = theme.enterKeyStyle.copy(cornerRadiusDp = value),
            backspaceKeyStyle = theme.backspaceKeyStyle.copy(cornerRadiusDp = value),
            spacebarStyle = theme.spacebarStyle.copy(cornerRadiusDp = value + 6f),
        )
    }

    fun updateDraftKeySpacing(value: Float) = updateDraft { theme ->
        theme.copy(
            layoutMetrics = theme.layoutMetrics.copy(
                keyGapDp = value,
                rowGapDp = value,
            ),
        )
    }

    fun updateDraftLabelSize(value: Float) = updateDraft { theme ->
        theme.copy(
            defaultKeyStyle = theme.defaultKeyStyle.copy(labelSizeSp = value),
            functionalKeyStyle = theme.functionalKeyStyle.copy(labelSizeSp = value - 1f),
            shiftKeyStyle = theme.shiftKeyStyle.copy(labelSizeSp = value - 1f),
            enterKeyStyle = theme.enterKeyStyle.copy(labelSizeSp = value - 1f),
            backspaceKeyStyle = theme.backspaceKeyStyle.copy(labelSizeSp = value - 1f),
            spacebarStyle = theme.spacebarStyle.copy(labelSizeSp = value - 2f),
        )
    }

    fun updateDraftPrimaryColor(argb: Long) = updateDraft { theme ->
        theme.copy(
            defaultKeyStyle = theme.defaultKeyStyle.copy(
                fill = theme.defaultKeyStyle.fill.copy(solidColor = ColorToken(argb)),
            ),
        )
    }

    fun updateDraftFunctionColor(argb: Long) = updateDraft { theme ->
        theme.copy(
            functionalKeyStyle = theme.functionalKeyStyle.copy(
                fill = theme.functionalKeyStyle.fill.copy(solidColor = ColorToken(argb)),
            ),
            shiftKeyStyle = theme.shiftKeyStyle.copy(
                fill = theme.shiftKeyStyle.fill.copy(solidColor = ColorToken(argb)),
            ),
            backspaceKeyStyle = theme.backspaceKeyStyle.copy(
                fill = theme.backspaceKeyStyle.fill.copy(solidColor = ColorToken(argb)),
            ),
        )
    }

    fun updateDraftBackgroundColor(argb: Long) = updateDraft { theme ->
        theme.copy(
            background = theme.background.copy(
                fill = FillStyle(solidColor = ColorToken(argb)),
            ),
        )
    }

    fun updateDraftKeyPressAnimation(preset: KeyPressAnimationPreset) = updateDraft { theme ->
        theme.copy(
            animationStyle = theme.animationStyle.copy(keyPressPreset = preset),
        )
    }

    fun updateDraftKeyboardTransitionAnimation(preset: KeyboardTransitionPreset) = updateDraft { theme ->
        theme.copy(
            animationStyle = theme.animationStyle.copy(keyboardTransitionPreset = preset),
        )
    }

    fun updateDraftThemeMotion(preset: ThemeMotionPreset) = updateDraft { theme ->
        theme.copy(
            animationStyle = theme.animationStyle.copy(themeMotionPreset = preset),
        )
    }

    fun updateDraftAnimationDuration(durationMs: Int) = updateDraft { theme ->
        theme.copy(
            animationStyle = theme.animationStyle.copy(durationMs = durationMs),
        )
    }

    fun randomizeDraft() {
        val background = randomColor()
        val primary = randomColor()
        val function = randomColor()
        updateDraft { theme ->
            theme.copy(
                background = theme.background.copy(fill = FillStyle(solidColor = ColorToken(background))),
                defaultKeyStyle = theme.defaultKeyStyle.copy(
                    fill = theme.defaultKeyStyle.fill.copy(solidColor = ColorToken(primary)),
                ),
                functionalKeyStyle = theme.functionalKeyStyle.copy(
                    fill = theme.functionalKeyStyle.fill.copy(solidColor = ColorToken(function)),
                ),
                shiftKeyStyle = theme.shiftKeyStyle.copy(
                    fill = theme.shiftKeyStyle.fill.copy(solidColor = ColorToken(function)),
                ),
                backspaceKeyStyle = theme.backspaceKeyStyle.copy(
                    fill = theme.backspaceKeyStyle.fill.copy(solidColor = ColorToken(function)),
                ),
            )
        }
    }

    fun updateKeyboardHeightScale(value: Float) {
        updateSettings { settings -> settings.copy(keyboardHeightScale = value) }
    }

    fun setNumberRowEnabled(enabled: Boolean) {
        updateSettings { settings -> settings.copy(showNumberRow = enabled) }
    }

    fun setAutoCapitalizationEnabled(enabled: Boolean) {
        updateSettings { settings -> settings.copy(autoCapitalization = enabled) }
    }

    fun setSuggestionsEnabled(enabled: Boolean) {
        updateSettings { settings -> settings.copy(suggestionsEnabled = enabled) }
    }

    fun setAutoCorrectionEnabled(enabled: Boolean) {
        updateSettings { settings -> settings.copy(autoCorrectionEnabled = enabled) }
    }

    fun setPopupPreviewEnabled(enabled: Boolean) {
        updateSettings { settings -> settings.copy(popupPreviewEnabled = enabled) }
    }

    fun setToolbarActionEnabled(action: ToolbarAction, enabled: Boolean) {
        updateSettings { settings ->
            settings.copy(
                toolbarItems = settings.toolbarItems.map { item ->
                    if (item.action == action) {
                        item.copy(enabled = enabled)
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun setSoundPackId(soundPackId: String) {
        updateSettings { settings -> settings.copy(soundPackId = soundPackId) }
    }

    fun setHapticProfileId(hapticProfileId: String) {
        updateSettings { settings -> settings.copy(hapticProfileId = hapticProfileId) }
    }

    fun updateImportBuffer(text: String) {
        importBuffer.value = text
    }

    fun exportTheme(theme: Theme = uiState.value.activeTheme) {
        importBuffer.value = themeRepository.exportTheme(theme)
    }

    fun importThemeFromBuffer() {
        viewModelScope.launch {
            val imported = runCatching {
                themeRepository.importTheme(importBuffer.value)
            }.getOrNull() ?: return@launch

            val theme = normalizeImportedTheme(imported)
            themeRepository.saveTheme(theme)
            themeManager.setActiveTheme(theme.id)
            draftTheme.value = theme
        }
    }

    private fun updateSettings(transform: (com.paletteboard.domain.model.UserSettings) -> com.paletteboard.domain.model.UserSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
        }
    }

    private fun updateDraft(transform: (Theme) -> Theme) {
        draftTheme.update { theme ->
            transform(theme).copy(updatedAt = System.currentTimeMillis())
        }
    }

    private fun normalizeImportedTheme(imported: ThemeExportFormat): Theme {
        val incoming = imported.theme
        return if (incoming.id.isBlank()) {
            incoming.copy(
                id = UUID.randomUUID().toString(),
                isPreset = false,
                updatedAt = System.currentTimeMillis(),
            )
        } else {
            incoming.copy(
                isPreset = false,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    private fun newThemeDraft(base: Theme): Theme = base.copy(
        id = UUID.randomUUID().toString(),
        name = "${base.name} Variant",
        isPreset = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    private fun randomColor(): Long {
        val rgb = Random.nextInt(0x00202020, 0x00FFFFFF)
        return 0xFF000000L or rgb.toLong()
    }
}

class MainViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(
            themeRepository = container.themeRepository,
            settingsRepository = container.settingsRepository,
            themeManager = container.themeManager,
        ) as T
    }
}
