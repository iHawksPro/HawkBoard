package com.paletteboard.ime.controller

import android.os.SystemClock
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.paletteboard.domain.model.KeyCodes
import com.paletteboard.domain.model.KeyKind
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
    private var lastShiftTapAtMs: Long = 0L
    private var emojiSearchQuery: String = ""
    private var emojiSearchActive: Boolean = false
    private var emojiSearchUsesSymbols: Boolean = false
    private var emojiSelectedGroup: String = emojiCatalog.groups().firstOrNull().orEmpty()
    private var emojiPageIndex: Int = 0
    private val recentEmojis = mutableListOf<String>()

    fun onStartInput(editorInfo: EditorInfo?, settings: UserSettings): KeyboardRenderState {
        keyboardMode = if (isNumericField(editorInfo)) KeyboardMode.SYMBOLS else KeyboardMode.LETTERS
        shiftState = ShiftState.OFF
        autoCapitalizationArmed = !isNumericField(editorInfo) && settings.autoCapitalization
        lastShiftTapAtMs = 0L
        emojiSearchQuery = ""
        emojiSearchActive = false
        emojiSearchUsesSymbols = false
        emojiPageIndex = 0
        emojiSelectedGroup = if (recentEmojis.isNotEmpty()) RECENT_GROUP else emojiCatalog.groups().firstOrNull().orEmpty()
        return buildRenderState(settings, inputConnection = null, editorInfo = editorInfo)
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
        editorInfo: EditorInfo? = null,
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
        }.withEnterKeyLabel(resolveEnterLabel(editorInfo))

        val token = currentToken(inputConnection)
        val suggestions = when {
            keyboardMode == KeyboardMode.EMOJI && emojiSearchActive ->
                emojiUiState?.searchEntries ?: emptyList()

            settings.suggestionsEnabled && token.isNotBlank() ->
                suggestionEngine.predict(token, layout.localeTag)

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
            return buildRenderState(settings, inputConnection, editorInfo)
        }

        when (key.code) {
            KeyCodes.SHIFT -> handleShiftTap()
            KeyCodes.BACKSPACE -> {
                inputConnection?.deleteSurroundingText(1, 0)
                synchronizeTextContext(editorInfo, inputConnection, settings)
                lastShiftTapAtMs = 0L
            }

            KeyCodes.SPACE -> {
                commitSpace(inputConnection, settings)
                lastShiftTapAtMs = 0L
            }

            KeyCodes.ENTER -> {
                commitEnter(inputConnection, editorInfo)
                autoCapitalizationArmed = settings.autoCapitalization
                lastShiftTapAtMs = 0L
            }

            KeyCodes.MODE_SYMBOLS -> {
                keyboardMode = KeyboardMode.SYMBOLS
                lastShiftTapAtMs = 0L
            }

            KeyCodes.MODE_LETTERS -> {
                keyboardMode = KeyboardMode.LETTERS
                lastShiftTapAtMs = 0L
            }

            KeyCodes.MODE_EMOJI -> {
                keyboardMode = KeyboardMode.EMOJI
                emojiSearchActive = false
                emojiSearchQuery = ""
                emojiSearchUsesSymbols = false
                emojiPageIndex = 0
                lastShiftTapAtMs = 0L
            }

            KeyCodes.CLIPBOARD -> Unit

            else -> {
                commitKeyText(key, inputConnection, editorInfo, settings)
                if (key.kind == KeyKind.EMOJI) {
                    rememberRecentEmoji(key.commitText.orEmpty())
                }
                lastShiftTapAtMs = 0L
            }
        }

        if (key.commitText?.length == 1 && shiftState == ShiftState.ON) {
            shiftState = ShiftState.OFF
        }

        return buildRenderState(settings, inputConnection, editorInfo)
    }

    fun handleSuggestionSelection(
        suggestion: String,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
        settings: UserSettings,
    ): KeyboardRenderState {
        if (keyboardMode == KeyboardMode.EMOJI) {
            inputConnection?.commitText(suggestion, 1)
            rememberRecentEmoji(suggestion)
            autoCapitalizationArmed = false
            return buildRenderState(settings, inputConnection, editorInfo)
        }

        val committed = applyCapitalization(suggestion, editorInfo, inputConnection, settings)
        replaceCurrentTokenWith(committed, inputConnection, appendSpace = true)
        if (shiftState == ShiftState.ON) {
            shiftState = ShiftState.OFF
        }
        autoCapitalizationArmed = false
        return buildRenderState(settings, inputConnection, editorInfo)
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

            ToolbarAction.ONE_HANDED,
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
            KeyCodes.SHIFT -> handleShiftTap()

            KeyCodes.BACKSPACE -> {
                if (emojiSearchQuery.isNotEmpty()) {
                    emojiSearchQuery = emojiSearchQuery.dropLast(1)
                } else {
                    emojiSearchActive = false
                    emojiSearchUsesSymbols = false
                }
                lastShiftTapAtMs = 0L
            }

            KeyCodes.SPACE -> {
                if (emojiSearchQuery.isNotBlank() && !emojiSearchQuery.endsWith(' ')) {
                    emojiSearchQuery += " "
                }
                lastShiftTapAtMs = 0L
            }

            KeyCodes.MODE_SYMBOLS -> {
                emojiSearchUsesSymbols = true
                lastShiftTapAtMs = 0L
            }

            KeyCodes.MODE_LETTERS -> {
                emojiSearchUsesSymbols = false
                lastShiftTapAtMs = 0L
            }

            KeyCodes.ENTER -> Unit

            else -> {
                val text = key.commitText ?: return
                emojiSearchQuery += text.lowercase()
                lastShiftTapAtMs = 0L
            }
        }
        if (!settings.autoCapitalization) {
            autoCapitalizationArmed = false
        }
    }

    private fun buildEmojiUiState(): EmojiUiState {
        val catalogGroups = emojiCatalog.groups()
        val groups = if (recentEmojis.isEmpty()) catalogGroups else listOf(RECENT_GROUP) + catalogGroups
        if (emojiSelectedGroup.isBlank()) {
            emojiSelectedGroup = groups.firstOrNull().orEmpty()
        }
        if (emojiSelectedGroup == RECENT_GROUP && recentEmojis.isEmpty()) {
            emojiSelectedGroup = catalogGroups.firstOrNull().orEmpty()
        }

        val page = if (emojiSelectedGroup == RECENT_GROUP) {
            buildRecentEmojiPage(emojiPageIndex)
        } else {
            emojiCatalog.pageForGroup(
                group = emojiSelectedGroup,
                pageIndex = emojiPageIndex,
                pageSize = EMOJI_PAGE_SIZE,
            )
        }
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

    private fun buildRecentEmojiPage(pageIndex: Int): EmojiCatalog.EmojiPage {
        val recentEntries = recentEmojis
            .take(EMOJI_RECENT_LIMIT)
            .map { emoji -> EmojiCatalog.EmojiEntry(emoji = emoji, name = emoji, group = RECENT_GROUP) }

        val totalPages = ((recentEntries.size + EMOJI_PAGE_SIZE - 1) / EMOJI_PAGE_SIZE).coerceAtLeast(1)
        val safePageIndex = pageIndex.coerceIn(0, totalPages - 1)
        val start = safePageIndex * EMOJI_PAGE_SIZE
        val end = (start + EMOJI_PAGE_SIZE).coerceAtMost(recentEntries.size)

        return EmojiCatalog.EmojiPage(
            entries = if (recentEntries.isEmpty()) emptyList() else recentEntries.subList(start, end),
            pageIndex = safePageIndex,
            totalPages = totalPages,
            canGoPrevious = safePageIndex > 0,
            canGoNext = safePageIndex < totalPages - 1,
        )
    }

    private fun rememberRecentEmoji(emoji: String) {
        if (emoji.isBlank()) return
        recentEmojis.remove(emoji)
        recentEmojis.add(0, emoji)
        if (recentEmojis.size > EMOJI_RECENT_LIMIT) {
            recentEmojis.removeAt(recentEmojis.lastIndex)
        }
    }

    private fun handleShiftTap() {
        val now = SystemClock.uptimeMillis()
        shiftState = when (shiftState) {
            ShiftState.OFF -> ShiftState.ON
            ShiftState.ON -> if (now - lastShiftTapAtMs <= SHIFT_DOUBLE_TAP_WINDOW_MS) ShiftState.LOCKED else ShiftState.OFF
            ShiftState.LOCKED -> ShiftState.OFF
        }
        lastShiftTapAtMs = now
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
        const val EMOJI_RECENT_LIMIT = 48
        const val SHIFT_DOUBLE_TAP_WINDOW_MS = 380L
        const val RECENT_GROUP = "Recent"
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

private fun KeyboardLayout.withEnterKeyLabel(label: String): KeyboardLayout {
    return copy(
        rows = rows.map { row ->
            row.copy(
                keys = row.keys.map { key ->
                    if (key.code == KeyCodes.ENTER) key.copy(label = label) else key
                },
            )
        },
    )
}

private fun resolveEnterLabel(editorInfo: EditorInfo?): String {
    return when (editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
        EditorInfo.IME_ACTION_GO -> "go"
        EditorInfo.IME_ACTION_SEARCH -> "search"
        EditorInfo.IME_ACTION_SEND -> "send"
        EditorInfo.IME_ACTION_NEXT -> "next"
        EditorInfo.IME_ACTION_DONE -> "done"
        else -> "return"
    }
}
