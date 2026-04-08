package com.paletteboard.engine.layout

import com.paletteboard.domain.model.KeyCodes
import com.paletteboard.domain.model.KeyKind
import com.paletteboard.domain.model.KeyRow
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.KeyboardLayout
import com.paletteboard.domain.model.KeyboardMode
import com.paletteboard.domain.model.SpecialKeyStyleTarget
import com.paletteboard.domain.model.UserSettings

object KeyboardLayoutFactory {
    fun createLayout(mode: KeyboardMode, settings: UserSettings): KeyboardLayout = when (mode) {
        KeyboardMode.LETTERS -> alphabetLayout(settings)
        KeyboardMode.SYMBOLS -> symbolsLayout(settings)
        KeyboardMode.EMOJI -> emojiLayout(settings)
    }

    private fun alphabetLayout(settings: UserSettings): KeyboardLayout {
        val rows = buildList {
            if (settings.showNumberRow) {
                add(KeyRow("1234567890".map(::characterKey), heightWeight = 0.9f))
            }

            add(KeyRow("qwertyuiop".map(::characterKey)))
            add(KeyRow("asdfghjkl".map(::characterKey)))
            add(
                KeyRow(
                    listOf(shiftKey()) +
                        "zxcvbnm".map(::characterKey) +
                        listOf(backspaceKey()),
                ),
            )
            add(
                KeyRow(
                    listOf(symbolsSwitchKey(), commaKey()) +
                        listOf(spaceKey()) +
                        listOf(periodKey(), enterKey()),
                    heightWeight = 0.98f,
                ),
            )
        }

        return KeyboardLayout(
            id = "letters",
            mode = KeyboardMode.LETTERS,
            localeTag = settings.multilingualLocales.firstOrNull() ?: "en-US",
            rows = rows,
            supportsGlideTyping = settings.gestureSettings.enabled,
            supportsSplit = true,
        )
    }

    private fun symbolsLayout(settings: UserSettings): KeyboardLayout = KeyboardLayout(
        id = "symbols",
        mode = KeyboardMode.SYMBOLS,
        localeTag = settings.multilingualLocales.firstOrNull() ?: "en-US",
        rows = listOf(
            KeyRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map(::symbolKey)),
            KeyRow(listOf("@", "#", "$", "&", "-", "+", "(", ")", "/", "*").map(::symbolKey)),
            KeyRow(
                listOf(lettersSwitchKey()) +
                    listOf("%", "\"", "'", ":", ";", "!", "?").map(::symbolKey) +
                    listOf(backspaceKey()),
            ),
            KeyRow(
                listOf(symbolsSwitchKey(label = "ABC", code = KeyCodes.MODE_LETTERS)) +
                    listOf(commaKey()) +
                    listOf(spaceKey()) +
                    listOf(periodKey(), enterKey()),
                heightWeight = 0.98f,
            ),
        ),
        supportsGlideTyping = false,
    )

    private fun emojiLayout(settings: UserSettings): KeyboardLayout = KeyboardLayout(
        id = "emoji",
        mode = KeyboardMode.EMOJI,
        localeTag = settings.multilingualLocales.firstOrNull() ?: "en-US",
        rows = listOf(
            KeyRow(listOf("😀", "😁", "😂", "🥹", "😍", "😎", "🤯", "😴").map(::emojiKey)),
            KeyRow(listOf("❤️", "🔥", "✨", "🎉", "👍", "🙌", "👏", "🤝").map(::emojiKey)),
            KeyRow(listOf("😊", "🤔", "😭", "🙏", "💯", "✅", "🌈", "🎵").map(::emojiKey)),
            KeyRow(
                listOf(lettersSwitchKey()) +
                    listOf(clipboardKey()) +
                    listOf(spaceKey()) +
                    listOf(backspaceKey()),
            ),
        ),
        supportsGlideTyping = false,
    )

    private fun characterKey(letter: Char): KeyboardKeySpec = KeyboardKeySpec(
        id = letter.toString(),
        label = letter.toString(),
        commitText = letter.toString(),
        code = letter.code,
        kind = KeyKind.CHARACTER,
        popupChars = emptyList(),
    )

    private fun symbolKey(symbol: String): KeyboardKeySpec = KeyboardKeySpec(
        id = "symbol_$symbol",
        label = symbol,
        commitText = symbol,
        code = symbol.first().code,
        kind = KeyKind.CHARACTER,
    )

    private fun emojiKey(emoji: String): KeyboardKeySpec = KeyboardKeySpec(
        id = "emoji_$emoji",
        label = emoji,
        commitText = emoji,
        code = emoji.first().code,
        kind = KeyKind.EMOJI,
    )

    private fun shiftKey(): KeyboardKeySpec = KeyboardKeySpec(
        id = "shift",
        label = "Shift",
        code = KeyCodes.SHIFT,
        kind = KeyKind.FUNCTION,
        widthWeight = 1.25f,
        specialStyleTarget = SpecialKeyStyleTarget.SHIFT,
    )

    private fun backspaceKey(): KeyboardKeySpec = KeyboardKeySpec(
        id = "backspace",
        label = "Del",
        code = KeyCodes.BACKSPACE,
        kind = KeyKind.FUNCTION,
        widthWeight = 1.25f,
        specialStyleTarget = SpecialKeyStyleTarget.BACKSPACE,
    )

    private fun commaKey(): KeyboardKeySpec = KeyboardKeySpec(
        id = "comma",
        label = ",",
        commitText = ",",
        code = ','.code,
        kind = KeyKind.CHARACTER,
        widthWeight = 1f,
    )

    private fun periodKey(): KeyboardKeySpec = KeyboardKeySpec(
        id = "period",
        label = ".",
        commitText = ".",
        code = '.'.code,
        kind = KeyKind.CHARACTER,
        widthWeight = 1f,
    )

    private fun spaceKey(): KeyboardKeySpec = KeyboardKeySpec(
        id = "space",
        label = "space",
        commitText = " ",
        code = KeyCodes.SPACE,
        kind = KeyKind.FUNCTION,
        widthWeight = 4.4f,
        specialStyleTarget = SpecialKeyStyleTarget.SPACEBAR,
    )

    private fun enterKey(): KeyboardKeySpec = KeyboardKeySpec(
        id = "enter",
        label = "Enter",
        commitText = "\n",
        code = KeyCodes.ENTER,
        kind = KeyKind.ACTION,
        widthWeight = 1.35f,
        specialStyleTarget = SpecialKeyStyleTarget.ENTER,
    )

    private fun symbolsSwitchKey(
        label: String = "?123",
        code: Int = KeyCodes.MODE_SYMBOLS,
    ): KeyboardKeySpec = KeyboardKeySpec(
        id = "mode_symbols",
        label = label,
        code = code,
        kind = KeyKind.MODE_SWITCH,
        widthWeight = 1.35f,
        specialStyleTarget = SpecialKeyStyleTarget.FUNCTION,
    )

    private fun lettersSwitchKey(): KeyboardKeySpec = KeyboardKeySpec(
        id = "mode_letters",
        label = "ABC",
        code = KeyCodes.MODE_LETTERS,
        kind = KeyKind.MODE_SWITCH,
        widthWeight = 1.35f,
        specialStyleTarget = SpecialKeyStyleTarget.FUNCTION,
    )

    private fun clipboardKey(): KeyboardKeySpec = KeyboardKeySpec(
        id = "clipboard",
        label = "Clip",
        code = KeyCodes.CLIPBOARD,
        kind = KeyKind.CLIPBOARD,
        widthWeight = 1.55f,
        specialStyleTarget = SpecialKeyStyleTarget.FUNCTION,
    )
}
