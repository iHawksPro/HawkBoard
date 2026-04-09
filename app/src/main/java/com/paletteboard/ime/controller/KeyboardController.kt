package com.paletteboard.ime.controller

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.paletteboard.domain.model.KeyCodes
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.KeyboardLayout
import com.paletteboard.domain.model.KeyboardMode
import com.paletteboard.domain.model.ToolbarAction
import com.paletteboard.domain.model.UserSettings
import com.paletteboard.engine.emoji.EmojiCatalog
import com.paletteboard.engine.layout.KeyboardLayoutFactory
import com.paletteboard.engine.suggestion.SuggestionEngine

class KeyboardController(
    private val emojiCatalog: EmojiCatalog,
    private val suggestionEngine: SuggestionEngine,
) {
    private var keyboardMode: KeyboardMode = KeyboardMode.LETTERS
    private var shiftState: ShiftState = ShiftState.OFF
    private var autoCapitalizationArmed: Boolean = true
    private var emojiSearchQuery: String = ""
    private var emojiSearchActive: Boolean = false
    private var emojiSearchUsesSymbols: Boolean = false
    private var emojiSelectedGroup: String = emojiCatalog.groups().firstOrNull().orEmpty()
    private var emojiPageIndex: Int = 0

    fun onStartInput(editorInfo: EditorInfo?, settings: UserSettings): KeyboardRenderState {
        keyboardMode = if (isNumericField(editorInfo)) KeyboardMode.SYMBOLS else KeyboardMode.LETTERS
        shiftState = ShiftState.OFF
        autoCapitalizationArmed = !isNumericField(editorInfo) && settings.autoCapitalization
        emojiSearchQuery = ""
        emojiSearchActive = false
        emojiSearchUsesSymbols = false
        emojiPageIndex = 0
        emojiSelectedGroup = emojiCatalog.groups().firstOrNull().orEmpty()
        return buildRenderState(settings, inputConnection = null)
    }

    fun synchronizeTextContext(
        editorInfo: EditorInfo?,
        inputConnection: InputConnection?,
        settings: UserSettings,
    ) {
        val inputType = editorInfo?.inputType ?: 0
        autoCapitalizationArmed = when {
            !settings.autoCapitalization -> false
            isNumericField(editorInfo) -> false
            (inputType and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0 -> true
            else -> deriveAutoCapitalizationFromContext(inputConnection)
        }
    }

    fun buildRenderState(
        settings: UserSettings,
        inputConnection: InputConnection?,
    ): KeyboardRenderState {
        val emojiUiState = if (keyboardMode == KeyboardMode.EMOJI) buildEmojiUiState() else null
        val layout = when {
            keyboardMode == KeyboardMode.EMOJI && emojiSearchActive && emojiSearchUsesSymbols ->
                KeyboardLayoutFactory.createEmojiSearchSymbolsLayout(settings)

            keyboardMode == KeyboardMode.EMOJI && emojiSearchActive ->
                KeyboardLayoutFactory.createEmojiSearchLettersLayout(settings)

            keyboardMode == KeyboardMode.EMOJI ->
                KeyboardLayoutFactory.createEmojiLayout(
                    settings = settings,
                    emojis = emojiUiState?.pageEntries ?: emptyList(),
                )

            else -> KeyboardLayoutFactory.createLayout(keyboardMode, settings)
        }
        val suggestions = when {
            keyboardMode == KeyboardMode.EMOJI && emojiSearchActive ->
                emojiUiState?.searchEntries ?: emptyList()

            settings.suggestionsEnabled ->
                suggestionEngine.predict(currentToken(inputConnection), layout.localeTag)

            else -> emptyList()
        }
        return KeyboardRenderState(
            layout = layout,
            suggestions = suggestions,
            shiftState = shiftState,
            emojiUiState = emojiUiState,
        )
    }

    fun handleTap(
        key: KeyboardKeySpec,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        settings: UserSettings,
    ): KeyboardRenderState {
        if (keyboardMode == KeyboardMode.EMOJI && emojiSearchActive) {
            handleEmojiSearchTap(key, settings)
            if (key.commitText?.length == 1 && shiftState == ShiftState.ON) {
                shiftState = ShiftState.OFF
            }
            return buildRenderState(settings, inputConnection)
        }

        when (key.code) {
            KeyCodes.SHIFT -> {
                shiftState = when (shiftState) {
                    ShiftState.OFF -> ShiftState.ON
                    ShiftState.ON -> ShiftState.LOCKED
                    ShiftState.LOCKED -> ShiftState.OFF
                }
            }

            KeyCodes.BACKSPACE -> {
                inputConnection?.deleteSurroundingText(1, 0)
                synchronizeTextContext(editorInfo, inputConnection, settings)
            }

            KeyCodes.SPACE -> commitSpace(inputConnection, settings)
            KeyCodes.ENTER -> {
                commitEnter(inputConnection, editorInfo)
                autoCapitalizationArmed = settings.autoCapitalization
            }
            KeyCodes.MODE_SYMBOLS -> keyboardMode = KeyboardMode.SYMBOLS
            KeyCodes.MODE_LETTERS -> keyboardMode = KeyboardMode.LETTERS
            KeyCodes.MODE_EMOJI -> {
                keyboardMode = KeyboardMode.EMOJI
                emojiSearchActive = false
                emojiSearchQuery = ""
                emojiSearchUsesSymbols = false
                emojiPageIndex = 0
            }
            KeyCodes.CLIPBOARD -> {
                // Clipboard surface is still handled from the toolbar/app shell.
            }

            else -> commitKeyText(key, inputConnection, editorInfo, settings)
        }

        if (key.commitText?.length == 1 && shiftState == ShiftState.ON) {
            shiftState = ShiftState.OFF
        }

        return buildRenderState(settings, inputConnection)
    }

    fun handleSuggestionSelection(
        suggestion: String,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        settings: UserSettings,
    ): KeyboardRenderState {
        if (keyboardMode == KeyboardMode.EMOJI) {
            inputConnection?.commitText(suggestion, 1)
            autoCapitalizationArmed = false
            return buildRenderState(settings, inputConnection)
        }
        val committed = applyCapitalization(suggestion, editorInfo, inputConnection, settings)
        replaceCurrentTokenWith(committed, inputConnection, appendSpace = true)
        if (shiftState == ShiftState.ON) {
            shiftState = ShiftState.OFF
        }
        autoCapitalizationArmed = false
        return buildRenderState(settings, inputConnection)
    }

    fun activateEmojiSearch(settings: UserSettings): KeyboardRenderState {
        keyboardMode = KeyboardMode.EMOJI
        emojiSearchActive = true
        emojiSearchQuery = ""
        emojiSearchUsesSymbols = false
        shiftState = ShiftState.OFF
        return buildRenderState(settings, inputConnection = null)
    }

    fun clearEmojiSearch(settings: UserSettings): KeyboardRenderState {
        emojiSearchQuery = ""
        emojiSearchActive = false
        emojiSearchUsesSymbols = false
        shiftState = ShiftState.OFF
        return buildRenderState(settings, inputConnection = null)
    }

    fun selectEmojiGroup(
        group: String,
        settings: UserSettings,
    ): KeyboardRenderState {
        emojiSelectedGroup = group
        emojiPageIndex = 0
        emojiSearchActive = false
        emojiSearchQuery = ""
        emojiSearchUsesSymbols = false
        return buildRenderState(settings, inputConnection = null)
    }

    fun goToNextEmojiPage(settings: UserSettings): KeyboardRenderState {
        emojiPageIndex += 1
        emojiSearchActive = false
        emojiSearchQuery = ""
        emojiSearchUsesSymbols = false
        return buildRenderState(settings, inputConnection = null)
    }

    fun goToPreviousEmojiPage(settings: UserSettings): KeyboardRenderState {
        emojiPageIndex = (emojiPageIndex - 1).coerceAtLeast(0)
        emojiSearchActive = false
        emojiSearchQuery = ""
        emojiSearchUsesSymbols = false
        return buildRenderState(settings, inputConnection = null)
    }

    fun handleToolbarAction(
        action: ToolbarAction,
        settings: UserSettings,
        inputConnection: InputConnection?,
    ): KeyboardToolbarResult {
        when (action) {
            ToolbarAction.EMOJI -> {
                keyboardMode = KeyboardMode.EMOJI
                emojiSearchActive = false
                emojiSearchQuery = ""
                emojiSearchUsesSymbols = false
                emojiPageIndex = 0
                shiftState = ShiftState.OFF
            }
            ToolbarAction.ONE_HANDED -> Unit
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
        autoCapitalizationArmed = shouldArmAfterCommittedText(commitText, settings)
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
        if ((inputType and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) return true
        return autoCapitalizationArmed
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
    }

    private fun commitSpace(
        inputConnection: InputConnection?,
        settings: UserSettings,
    ) {
        val trailingText = inputConnection?.getTextBeforeCursor(24, 0)?.toString().orEmpty()
        if (settings.autoSpacing && trailingText.length == 2 && trailingText[1] == ' ' && trailingText[0].isLetterOrDigit()) {
            inputConnection?.deleteSurroundingText(1, 0)
            inputConnection?.commitText(". ", 1)
            autoCapitalizationArmed = settings.autoCapitalization
            return
        }

        val lastCharacter = trailingText.takeLast(1)
        if (lastCharacter != " ") {
            inputConnection?.commitText(" ", 1)
        }
        val trimmed = trailingText.trimEnd()
        autoCapitalizationArmed = settings.autoCapitalization &&
            trimmed.isNotEmpty() &&
            sentenceTerminatorBeforeCursor(trimmed.lastIndex, trimmed)
    }

    private fun commitEnter(inputConnection: InputConnection?, editorInfo: EditorInfo?) {
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

    private fun handleEmojiSearchTap(
        key: KeyboardKeySpec,
        settings: UserSettings,
    ) {
        when (key.code) {
            KeyCodes.SHIFT -> {
                shiftState = when (shiftState) {
                    ShiftState.OFF -> ShiftState.ON
                    ShiftState.ON -> ShiftState.LOCKED
                    ShiftState.LOCKED -> ShiftState.OFF
                }
            }

            KeyCodes.BACKSPACE -> {
                if (emojiSearchQuery.isNotEmpty()) {
                    emojiSearchQuery = emojiSearchQuery.dropLast(1)
                } else {
                    emojiSearchActive = false
                    emojiSearchUsesSymbols = false
                }
            }

            KeyCodes.SPACE -> {
                if (emojiSearchQuery.isNotBlank() && !emojiSearchQuery.endsWith(' ')) {
                    emojiSearchQuery += " "
                }
            }

            KeyCodes.MODE_SYMBOLS -> emojiSearchUsesSymbols = true
            KeyCodes.MODE_LETTERS -> emojiSearchUsesSymbols = false
            KeyCodes.ENTER -> Unit
            else -> {
                val text = key.commitText ?: return
                emojiSearchQuery += text.lowercase()
            }
        }
        if (!settings.autoCapitalization) {
            autoCapitalizationArmed = false
        }
    }

    private fun buildEmojiUiState(): EmojiUiState {
        val groups = emojiCatalog.groups()
        if (emojiSelectedGroup.isBlank()) {
            emojiSelectedGroup = groups.firstOrNull().orEmpty()
        }
        val page = emojiCatalog.pageForGroup(
            group = emojiSelectedGroup,
            pageIndex = emojiPageIndex,
            pageSize = EMOJI_PAGE_SIZE,
        )
        emojiPageIndex = page.pageIndex
        return EmojiUiState(
            isVisible = keyboardMode == KeyboardMode.EMOJI,
            searchActive = emojiSearchActive,
            searchQuery = emojiSearchQuery,
            groups = groups,
            selectedGroup = emojiSelectedGroup,
            pageIndex = page.pageIndex,
            totalPages = page.totalPages,
            canGoPrevious = page.canGoPrevious,
            canGoNext = page.canGoNext,
            pageEntries = page.entries.map { it.emoji },
            searchEntries = emojiCatalog.search(emojiSearchQuery, EMOJI_SEARCH_LIMIT).map { it.emoji },
        )
    }

    private fun shouldArmAfterCommittedText(
        committedText: String,
        settings: UserSettings,
    ): Boolean {
        if (!settings.autoCapitalization) return false
        val trimmed = committedText.trimEnd()
        if (trimmed.isEmpty()) return autoCapitalizationArmed
        return sentenceTerminatorBeforeCursor(trimmed.lastIndex, trimmed) || trimmed.last() == '\n'
    }

    private fun deriveAutoCapitalizationFromContext(inputConnection: InputConnection?): Boolean {
        val beforeCursor = inputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        if (beforeCursor.isEmpty() || beforeCursor.isBlank()) return true
        if (beforeCursor.lastOrNull() == '\n') return true

        val trimmedWhitespace = beforeCursor.trimEnd(' ', '\t')
        if (trimmedWhitespace.isEmpty()) return true
        return sentenceTerminatorBeforeCursor(trimmedWhitespace.lastIndex, trimmedWhitespace)
    }

    private fun sentenceTerminatorBeforeCursor(
        startIndex: Int,
        text: String,
    ): Boolean {
        var index = startIndex
        while (index >= 0 && text[index] in SENTENCE_CLOSERS) {
            index -= 1
        }
        if (index < 0) return true
        return text[index] in SENTENCE_ENDINGS || text[index] == '\n'
    }

    private companion object {
        val SENTENCE_ENDINGS = setOf('.', '!', '?')
        val SENTENCE_CLOSERS = setOf('"', '\'', ')', ']', '}', '»', '”', '’')
        const val EMOJI_PAGE_SIZE = 24
        const val EMOJI_SEARCH_LIMIT = 12
    }
}

data class KeyboardRenderState(
    val layout: KeyboardLayout,
    val suggestions: List<String>,
    val shiftState: ShiftState,
    val emojiUiState: EmojiUiState? = null,
)

data class EmojiUiState(
    val isVisible: Boolean,
    val searchActive: Boolean,
    val searchQuery: String,
    val groups: List<String>,
    val selectedGroup: String,
    val pageIndex: Int,
    val totalPages: Int,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
    val pageEntries: List<String>,
    val searchEntries: List<String>,
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
