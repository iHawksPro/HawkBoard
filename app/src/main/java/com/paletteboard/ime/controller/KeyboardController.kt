package com.paletteboard.ime.controller

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.paletteboard.domain.model.GestureSample
import com.paletteboard.domain.model.KeyCodes
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.KeyboardLayout
import com.paletteboard.domain.model.KeyboardMode
import com.paletteboard.domain.model.ToolbarAction
import com.paletteboard.domain.model.UserSettings
import com.paletteboard.engine.gesture.GlideTypingEngine
import com.paletteboard.engine.layout.KeyboardLayoutFactory
import com.paletteboard.engine.suggestion.SuggestionEngine

class KeyboardController(
    private val suggestionEngine: SuggestionEngine,
    private val glideTypingEngine: GlideTypingEngine,
) {
    private var keyboardMode: KeyboardMode = KeyboardMode.LETTERS
    private var shiftState: ShiftState = ShiftState.OFF
    private var pendingSuggestionReplacement: ReplacementContext? = null

    fun onStartInput(editorInfo: EditorInfo?, settings: UserSettings): KeyboardRenderState {
        keyboardMode = if (isNumericField(editorInfo)) KeyboardMode.SYMBOLS else KeyboardMode.LETTERS
        shiftState = ShiftState.OFF
        pendingSuggestionReplacement = null
        return buildRenderState(settings, inputConnection = null)
    }

    fun buildRenderState(
        settings: UserSettings,
        inputConnection: InputConnection?,
    ): KeyboardRenderState {
        val layout = KeyboardLayoutFactory.createLayout(keyboardMode, settings)
        val suggestions = if (settings.suggestionsEnabled) {
            suggestionEngine.predict(currentToken(inputConnection), layout.localeTag)
        } else {
            emptyList()
        }
        return KeyboardRenderState(
            layout = layout,
            suggestions = suggestions,
            shiftState = shiftState,
        )
    }

    fun handleTap(
        key: KeyboardKeySpec,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        settings: UserSettings,
    ): KeyboardRenderState {
        pendingSuggestionReplacement = null
        when (key.code) {
            KeyCodes.SHIFT -> shiftState = if (shiftState == ShiftState.ON) ShiftState.LOCKED else ShiftState.ON
            KeyCodes.BACKSPACE -> inputConnection?.deleteSurroundingText(1, 0)
            KeyCodes.SPACE -> commitSpace(inputConnection)
            KeyCodes.ENTER -> commitEnter(inputConnection, editorInfo)
            KeyCodes.MODE_SYMBOLS -> keyboardMode = KeyboardMode.SYMBOLS
            KeyCodes.MODE_LETTERS -> keyboardMode = KeyboardMode.LETTERS
            KeyCodes.MODE_EMOJI -> keyboardMode = KeyboardMode.EMOJI
            KeyCodes.CLIPBOARD -> {
                // Placeholder for future clipboard panel state.
            }

            else -> commitKeyText(key, inputConnection, editorInfo, settings)
        }

        if (key.commitText?.length == 1 && shiftState == ShiftState.ON) {
            shiftState = ShiftState.OFF
        }

        return buildRenderState(settings, inputConnection)
    }

    fun handleGlide(
        sample: GestureSample,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        settings: UserSettings,
    ): KeyboardRenderState {
        if (!settings.gestureSettings.enabled) return buildRenderState(settings, inputConnection)

        val locale = settings.multilingualLocales.firstOrNull() ?: "en-US"
        val candidates = glideTypingEngine.decode(
            sample = sample,
            lexicon = suggestionEngine.lexicon(locale),
            sensitivity = settings.gestureSettings.sensitivity,
        )
        val bestCandidate = candidates.firstOrNull()?.word
        if (bestCandidate != null) {
            val committedWord = applyCapitalization(bestCandidate, editorInfo, inputConnection, settings)
            val appendSpace = settings.autoSpacing
            inputConnection?.commitText(committedWord + if (appendSpace) " " else "", 1)
            pendingSuggestionReplacement = ReplacementContext(
                wordLength = committedWord.length,
                trailingSpace = appendSpace,
            )
            if (shiftState == ShiftState.ON) {
                shiftState = ShiftState.OFF
            }
        }
        return KeyboardRenderState(
            layout = KeyboardLayoutFactory.createLayout(keyboardMode, settings),
            suggestions = candidates.map { applyCapitalization(it.word, editorInfo, inputConnection, settings) },
            shiftState = shiftState,
        )
    }

    fun handleSuggestionSelection(
        suggestion: String,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        settings: UserSettings,
    ): KeyboardRenderState {
        val committed = applyCapitalization(suggestion, editorInfo, inputConnection, settings)
        val replacement = pendingSuggestionReplacement
        if (replacement != null) {
            inputConnection?.deleteSurroundingText(
                replacement.wordLength + if (replacement.trailingSpace) 1 else 0,
                0,
            )
            inputConnection?.commitText(committed + if (replacement.trailingSpace) " " else "", 1)
            pendingSuggestionReplacement = replacement.copy(wordLength = committed.length)
        } else {
            replaceCurrentTokenWith(committed, inputConnection, appendSpace = true)
        }
        if (shiftState == ShiftState.ON) {
            shiftState = ShiftState.OFF
        }
        return buildRenderState(settings, inputConnection)
    }

    fun handleToolbarAction(
        action: ToolbarAction,
        settings: UserSettings,
        inputConnection: InputConnection?,
    ): KeyboardToolbarResult {
        pendingSuggestionReplacement = null
        when (action) {
            ToolbarAction.EMOJI -> keyboardMode = KeyboardMode.EMOJI
            ToolbarAction.ONE_HANDED -> {
                // Layout mirroring belongs in the view/layout layer in a future pass.
            }

            ToolbarAction.CLIPBOARD,
            ToolbarAction.THEMES,
            ToolbarAction.SETTINGS,
            ToolbarAction.TRANSLATE,
            ToolbarAction.CALCULATOR,
            -> Unit
        }

        return KeyboardToolbarResult(
            renderState = buildRenderState(settings, inputConnection),
            sideEffect = when (action) {
                ToolbarAction.SETTINGS -> KeyboardSideEffect.OPEN_SETTINGS
                ToolbarAction.THEMES -> KeyboardSideEffect.OPEN_THEME_LIBRARY
                ToolbarAction.CLIPBOARD -> KeyboardSideEffect.OPEN_CLIPBOARD
                else -> null
            },
        )
    }

    private fun commitKeyText(
        key: KeyboardKeySpec,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        settings: UserSettings,
    ) {
        val text = key.commitText ?: return
        val commitText = if (text.length == 1 && text[0].isLetter()) {
            applyCapitalization(text, editorInfo, inputConnection, settings)
        } else {
            text
        }
        inputConnection?.commitText(commitText, 1)
    }

    private fun applyCapitalization(
        text: String,
        editorInfo: EditorInfo?,
        inputConnection: InputConnection?,
        settings: UserSettings,
    ): String {
        if (text.isEmpty()) return text
        return if (text[0].isLetter() && shouldUppercase(editorInfo, inputConnection, settings)) {
            text.replaceFirstChar { it.uppercase() }
        } else {
            text
        }
    }

    private fun shouldUppercase(
        editorInfo: EditorInfo?,
        inputConnection: InputConnection?,
        settings: UserSettings,
    ): Boolean {
        if (shiftState == ShiftState.LOCKED || shiftState == ShiftState.ON) return true
        if (!settings.autoCapitalization) return false
        val inputType = editorInfo?.inputType ?: 0
        return (inputConnection?.getCursorCapsMode(inputType) ?: 0) != 0
    }

    private fun replaceCurrentTokenWith(
        suggestion: String,
        inputConnection: InputConnection?,
        appendSpace: Boolean,
    ) {
        val token = currentToken(inputConnection)
        if (token.isNotEmpty()) {
            inputConnection?.deleteSurroundingText(token.length, 0)
        }
        inputConnection?.commitText(suggestion + if (appendSpace) " " else "", 1)
        pendingSuggestionReplacement = ReplacementContext(
            wordLength = suggestion.length,
            trailingSpace = appendSpace,
        )
    }

    private fun commitSpace(inputConnection: InputConnection?) {
        val trailingText = inputConnection?.getTextBeforeCursor(1, 0)?.toString().orEmpty()
        if (!trailingText.endsWith(" ")) {
            inputConnection?.commitText(" ", 1)
        }
    }

    private fun commitEnter(inputConnection: InputConnection?, editorInfo: EditorInfo?) {
        pendingSuggestionReplacement = null
        val action = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE) {
            inputConnection?.performEditorAction(action)
        } else {
            inputConnection?.commitText("\n", 1)
        }
    }

    private fun currentToken(inputConnection: InputConnection?): String {
        val beforeCursor = inputConnection?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        return beforeCursor
            .takeLastWhile { !it.isWhitespace() && it !in ".,!?;:" }
            .lowercase()
    }

    private fun isNumericField(editorInfo: EditorInfo?): Boolean {
        val inputType = editorInfo?.inputType ?: return false
        val clazz = inputType and InputType.TYPE_MASK_CLASS
        return clazz == InputType.TYPE_CLASS_NUMBER || clazz == InputType.TYPE_CLASS_PHONE
    }
}

data class KeyboardRenderState(
    val layout: KeyboardLayout,
    val suggestions: List<String>,
    val shiftState: ShiftState,
)

data class KeyboardToolbarResult(
    val renderState: KeyboardRenderState,
    val sideEffect: KeyboardSideEffect?,
)

enum class ShiftState {
    OFF,
    ON,
    LOCKED,
}

enum class KeyboardSideEffect {
    OPEN_SETTINGS,
    OPEN_THEME_LIBRARY,
    OPEN_CLIPBOARD,
}

private data class ReplacementContext(
    val wordLength: Int,
    val trailingSpace: Boolean,
)
