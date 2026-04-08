package com.paletteboard.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.paletteboard.MainActivity
import com.paletteboard.app.PaletteBoardApp
import com.paletteboard.domain.model.GestureSample
import com.paletteboard.domain.model.KeyboardKeySpec
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

    override fun onFinishInputView(finishingInput: Boolean) {
        keyboardRootView?.clearGestureTrail()
        super.onFinishInputView(finishingInput)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onKeyTapped(key: KeyboardKeySpec) {
        appContainer.keyboardController.handleTap(
            key = key,
            inputConnection = currentInputConnection,
            editorInfo = currentEditor,
            settings = currentSettings,
        )
        render()
    }

    override fun onGestureCompleted(sample: GestureSample) {
        appContainer.keyboardController.handleGlide(
            sample = sample,
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

    override fun onToolbarAction(action: ToolbarAction) {
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

    private fun render() {
        val rootView = keyboardRootView ?: return
        val renderState = appContainer.keyboardController.buildRenderState(
            settings = currentSettings,
            inputConnection = currentInputConnection,
        )
        rootView.render(
            layout = renderState.layout,
            theme = currentTheme,
            suggestions = renderState.suggestions,
            toolbarItems = currentSettings.toolbarItems,
            themeManager = appContainer.themeManager,
            keyboardHeightScale = currentSettings.keyboardHeightScale,
        )
    }
}
