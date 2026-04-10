package com.paletteboard.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import com.paletteboard.domain.model.KeyCodes
import com.paletteboard.MainActivity
import com.paletteboard.app.PaletteBoardApp
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.OneHandedMode
import com.paletteboard.domain.model.Theme
import com.paletteboard.domain.model.ToolbarAction
import com.paletteboard.domain.model.UserSettings
import com.paletteboard.engine.theme.DefaultThemes
import com.paletteboard.ime.controller.KeyboardSideEffect
import com.paletteboard.ime.view.KeyboardRootView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PaletteInputMethodService : InputMethodService(), KeyboardRootView.Callback {
    private val appContainer by lazy { (application as PaletteBoardApp).container }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var keyboardRootView: KeyboardRootView? = null
    private var currentTheme: Theme = DefaultThemes.midnightPulse
    private var currentSettings: UserSettings = UserSettings()
    private var currentEditor: EditorInfo? = null

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            appContainer.settingsRepository.settings.collectLatest { settings ->
                currentSettings = settings
                render()
            }
        }
        serviceScope.launch {
            appContainer.themeManager.activeTheme.collectLatest { theme ->
                currentTheme = theme
                render()
            }
        }
    }

    override fun onCreateInputView(): View {
        return KeyboardRootView(this).also { rootView ->
            keyboardRootView = rootView
            rootView.setCallback(this)
            render()
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentEditor = attribute
        appContainer.keyboardController.onStartInput(attribute, currentSettings)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentEditor = info
        render()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onKeyTapped(key: KeyboardKeySpec) {
        if (key.code == KeyCodes.LANGUAGE_SWITCH) {
            getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
            return
        }
        appContainer.typingFeedbackController.performKeyFeedback(
            key = key,
            settings = currentSettings,
        )
        appContainer.keyboardController.handleTap(
            key = key,
            inputConnection = currentInputConnection,
            editorInfo = currentEditor,
            settings = currentSettings,
        )
        render()
    }

    override fun onSuggestionSelected(text: String) {
        appContainer.keyboardController.handleSuggestionSelection(
            suggestion = text,
            inputConnection = currentInputConnection,
            editorInfo = currentEditor,
            settings = currentSettings,
        )
        render()
    }

    override fun onCursorMoved(steps: Int) {
        val inputConnection = currentInputConnection ?: return
        val extracted = inputConnection.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted != null) {
            val textLength = extracted.text?.length ?: 0
            val currentSelection = extracted.selectionEnd.coerceAtLeast(0)
            val nextSelection = (currentSelection + steps).coerceIn(0, textLength)
            if (nextSelection != currentSelection) {
                inputConnection.setSelection(nextSelection, nextSelection)
            }
            return
        }

        val before = inputConnection.getTextBeforeCursor(128, 0)?.length ?: 0
        val after = inputConnection.getTextAfterCursor(128, 0)?.length ?: 0
        val nextSelection = (before + steps).coerceIn(0, before + after)
        inputConnection.setSelection(nextSelection, nextSelection)
    }

    override fun onToolbarAction(action: ToolbarAction) {
        if (action == ToolbarAction.ONE_HANDED) {
            serviceScope.launch {
                appContainer.settingsRepository.update { settings ->
                    settings.copy(oneHandedMode = settings.oneHandedMode.nextMode())
                }
            }
            return
        }
        val result = appContainer.keyboardController.handleToolbarAction(
            action = action,
            settings = currentSettings,
            inputConnection = currentInputConnection,
        )
        when (result.sideEffect) {
            KeyboardSideEffect.OPEN_SETTINGS,
            KeyboardSideEffect.OPEN_THEME_LIBRARY,
            -> {
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }

            KeyboardSideEffect.OPEN_CLIPBOARD,
            null,
            -> Unit
        }
        render()
    }

    override fun onEmojiSearchTapped() {
        appContainer.keyboardController.activateEmojiSearch(currentSettings)
        render()
    }

    override fun onEmojiSearchCleared() {
        appContainer.keyboardController.clearEmojiSearch(currentSettings)
        render()
    }

    override fun onEmojiGroupSelected(group: String) {
        appContainer.keyboardController.selectEmojiGroup(group, currentSettings)
        render()
    }

    override fun onEmojiPreviousPage() {
        appContainer.keyboardController.goToPreviousEmojiPage(currentSettings)
        render()
    }

    override fun onEmojiNextPage() {
        appContainer.keyboardController.goToNextEmojiPage(currentSettings)
        render()
    }

    private fun render() {
        val rootView = keyboardRootView ?: return
        appContainer.keyboardController.synchronizeTextContext(
            editorInfo = currentEditor,
            inputConnection = currentInputConnection,
            settings = currentSettings,
        )
        val renderState = appContainer.keyboardController.buildRenderState(
            settings = currentSettings,
            inputConnection = currentInputConnection,
            editorInfo = currentEditor,
        )
        rootView.render(
            layout = renderState.layout,
            theme = currentTheme,
            suggestions = renderState.suggestions,
            toolbarItems = currentSettings.toolbarItems,
            themeManager = appContainer.themeManager,
            shiftState = renderState.shiftState,
            emojiUiState = renderState.emojiUiState,
            popupPreviewEnabled = currentSettings.popupPreviewEnabled,
            keyboardHeightScale = currentSettings.keyboardHeightScale,
            oneHandedMode = currentSettings.oneHandedMode,
        )
    }
}

private fun OneHandedMode.nextMode(): OneHandedMode = when (this) {
    OneHandedMode.OFF -> OneHandedMode.RIGHT
    OneHandedMode.RIGHT -> OneHandedMode.LEFT
    OneHandedMode.LEFT -> OneHandedMode.OFF
}
