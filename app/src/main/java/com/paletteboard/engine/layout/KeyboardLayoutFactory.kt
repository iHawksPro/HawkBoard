package com.paletteboard.engine.layout

import com.paletteboard.domain.model.KeyCodes
import com.paletteboard.domain.model.KeyKind
import com.paletteboard.domain.model.KeyRow
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.KeyboardLayout
import com.paletteboard.domain.model.KeyboardMode
import com.paletteboard.domain.model.SpecialKeyStyleTarget
import com.paletteboard.domain.model.UserSettings
import java.util.Locale

object KeyboardLayoutFactory {
    fun createLayout(mode: KeyboardMode, settings: UserSettings): KeyboardLayout = when (mode) {
        KeyboardMode.LETTERS -> alphabetLayout(settings)
        KeyboardMode.SYMBOLS -> symbolsLayout(settings)
        KeyboardMode.EMOJI -> createEmojiLayout(settings, defaultEmojiPage)
    }

    fun createEmojiLayout(
        settings: UserSettings,
        emojis: List<String>,
    ): KeyboardLayout {
        val localeTag = settings.multilingualLocales.firstOrNull() ?: "en-US"
        val page = emojis.ifEmpty { defaultEmojiPage }.take(24)
        val rows = page.chunked(8).map { chunk ->
            val missing = (8 - chunk.size).coerceAtLeast(0)
            val inset = missing * 0.52f
            KeyRow(
                keys = chunk.map(::emojiKey),
                heightWeight = 0.94f,
                leadingInsetWeight = inset / 2f,
                trailingInsetWeight = inset / 2f,
            )
        }.toMutableList()
        while (rows.size < 3) {
            rows += KeyRow(
                keys = emptyList(),
                heightWeight = 0.94f,
                leadingInsetWeight = 4.16f,
                trailingInsetWeight = 4.16f,
            )
        }
        rows += KeyRow(
            keys = listOf(
                lettersSwitchKey(),
                spaceKey(localeTag),
                backspaceKey(widthWeight = 1.72f),
            ),
            heightWeight = 0.98f,
            leadingInsetWeight = 0.44f,
            trailingInsetWeight = 0.44f,
        )

        return KeyboardLayout(
            id = "emoji",
            mode = KeyboardMode.EMOJI,
            localeTag = localeTag,
            rows = rows,
            supportsGlideTyping = false,
        )
    }

    fun createEmojiSearchLettersLayout(settings: UserSettings): KeyboardLayout =
        alphabetLayout(settings).copy(id = "emoji_search_letters")

    fun createEmojiSearchSymbolsLayout(settings: UserSettings): KeyboardLayout =
        symbolsLayout(settings).copy(id = "emoji_search_symbols")

    private fun alphabetLayout(settings: UserSettings): KeyboardLayout {
        val localeTag = settings.multilingualLocales.firstOrNull() ?: "en-US"
        val rows = buildList {
            if (settings.showNumberRow) {
                add(KeyRow("1234567890".map(::numberKey), heightWeight = 0.7f))
            }
            add(KeyRow("qwertyuiop".map(::characterKey), heightWeight = 1.1f))
            add(
                KeyRow(
                    keys = "asdfghjkl".map(::characterKey),
                    heightWeight = 1.1f,
                    leadingInsetWeight = 0.46f,
                    trailingInsetWeight = 0.46f,
                ),
            )
            add(
                KeyRow(
                    keys = listOf(shiftKey()) + "zxcvbnm".map(::characterKey) + listOf(backspaceKey()),
                    heightWeight = 1.18f,
                    leadingInsetWeight = 0.03f,
                    trailingInsetWeight = 0.03f,
                ),
            )
            add(
                KeyRow(
                    keys = listOf(
                        symbolsSwitchKey(label = "123", widthWeight = 1.04f),
                        emojiSwitchKey(widthWeight = 0.9f),
                        commaKey(),
                        spaceKey(localeTag),
                        periodKey(widthWeight = 0.78f),
                        enterKey(widthWeight = 1.42f),
                    ),
                    heightWeight = 1.02f,
                    leadingInsetWeight = 0.04f,
                    trailingInsetWeight = 0.04f,
                ),
            )
        }

        return KeyboardLayout(
            id = "letters",
            mode = KeyboardMode.LETTERS,
            localeTag = localeTag,
            rows = rows,
            supportsGlideTyping = false,
            supportsSplit = true,
        )
    }

    private fun symbolsLayout(settings: UserSettings): KeyboardLayout {
        val localeTag = settings.multilingualLocales.firstOrNull() ?: "en-US"
        return KeyboardLayout(
            id = "symbols",
            mode = KeyboardMode.SYMBOLS,
            localeTag = localeTag,
            rows = listOf(
                KeyRow("1234567890".map { symbolKey(it.toString()) }, heightWeight = 0.78f),
                KeyRow(
                    keys = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/").map(::symbolKey),
                    heightWeight = 1.02f,
                ),
                KeyRow(
                    keys = listOf(lettersSwitchKey()) +
                        listOf("\"", "'", ":", ";", "!", "?", "[", "]").map(::symbolKey) +
                        listOf(backspaceKey()),
                    heightWeight = 1.08f,
                    leadingInsetWeight = 0.12f,
                    trailingInsetWeight = 0.12f,
                ),
                KeyRow(
                    keys = listOf(
                        lettersSwitchKey(widthWeight = 1.04f),
                        emojiSwitchKey(widthWeight = 0.9f),
                        commaKey(),
                        spaceKey(localeTag),
                        periodKey(widthWeight = 0.78f),
                        enterKey(widthWeight = 1.42f),
                    ),
                    heightWeight = 1.02f,
                    leadingInsetWeight = 0.04f,
                    trailingInsetWeight = 0.04f,
                ),
            ),
            supportsGlideTyping = false,
        )
    }

    private fun characterKey(letter: Char): KeyboardKeySpec {
        val popupChars = letterPopupChars[letter].orEmpty()
        return KeyboardKeySpec(
            id = letter.toString(),
            label = letter.toString(),
            commitText = letter.toString(),
            code = letter.code,
            kind = KeyKind.CHARACTER,
            hintLabel = letterHintLabels[letter],
            popupChars = popupChars,
        )
    }

    private fun numberKey(number: Char): KeyboardKeySpec = KeyboardKeySpec(
        id = "number_$number",
        label = number.toString(),
        commitText = number.toString(),
        code = number.code,
        kind = KeyKind.CHARACTER,
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
        widthWeight = 1.04f,
    )

    private fun shiftKey(widthWeight: Float = 1.46f): KeyboardKeySpec = KeyboardKeySpec(
        id = "shift",
        label = "\u21E7",
        code = KeyCodes.SHIFT,
        kind = KeyKind.FUNCTION,
        widthWeight = widthWeight,
        specialStyleTarget = SpecialKeyStyleTarget.SHIFT,
    )

    private fun backspaceKey(widthWeight: Float = 1.46f): KeyboardKeySpec = KeyboardKeySpec(
        id = "backspace",
        label = "\u232B",
        code = KeyCodes.BACKSPACE,
        kind = KeyKind.FUNCTION,
        widthWeight = widthWeight,
        specialStyleTarget = SpecialKeyStyleTarget.BACKSPACE,
    )

    private fun commaKey(): KeyboardKeySpec = KeyboardKeySpec(
        id = "comma",
        label = ",",
        commitText = ",",
        code = ','.code,
        kind = KeyKind.CHARACTER,
        widthWeight = 0.78f,
        popupChars = listOf(";", ":", "@"),
    )

    private fun periodKey(widthWeight: Float = 0.96f): KeyboardKeySpec = KeyboardKeySpec(
        id = "period",
        label = ".",
        commitText = ".",
        code = '.'.code,
        kind = KeyKind.CHARACTER,
        widthWeight = widthWeight,
        popupChars = listOf("?", "!", ","),
    )

    private fun spaceKey(localeTag: String): KeyboardKeySpec = KeyboardKeySpec(
        id = "space",
        label = spaceLabel(localeTag),
        commitText = " ",
        code = KeyCodes.SPACE,
        kind = KeyKind.FUNCTION,
        widthWeight = 4.18f,
        specialStyleTarget = SpecialKeyStyleTarget.SPACEBAR,
    )

    private fun enterKey(widthWeight: Float = 1.78f): KeyboardKeySpec = KeyboardKeySpec(
        id = "enter",
        label = "return",
        commitText = "\n",
        code = KeyCodes.ENTER,
        kind = KeyKind.ACTION,
        widthWeight = widthWeight,
        specialStyleTarget = SpecialKeyStyleTarget.ENTER,
    )

    private fun symbolsSwitchKey(
        label: String = "123",
        code: Int = KeyCodes.MODE_SYMBOLS,
        widthWeight: Float = 1.44f,
    ): KeyboardKeySpec = KeyboardKeySpec(
        id = "mode_symbols",
        label = label,
        code = code,
        kind = KeyKind.MODE_SWITCH,
        widthWeight = widthWeight,
        specialStyleTarget = SpecialKeyStyleTarget.FUNCTION,
    )

    private fun lettersSwitchKey(
        label: String = "ABC",
        widthWeight: Float = 1.44f,
    ): KeyboardKeySpec = KeyboardKeySpec(
        id = "mode_letters",
        label = label,
        code = KeyCodes.MODE_LETTERS,
        kind = KeyKind.MODE_SWITCH,
        widthWeight = widthWeight,
        specialStyleTarget = SpecialKeyStyleTarget.FUNCTION,
    )

    private fun emojiSwitchKey(widthWeight: Float = 0.96f): KeyboardKeySpec = KeyboardKeySpec(
        id = "mode_emoji",
        label = "\uD83D\uDE03",
        code = KeyCodes.MODE_EMOJI,
        kind = KeyKind.MODE_SWITCH,
        widthWeight = widthWeight,
        specialStyleTarget = SpecialKeyStyleTarget.FUNCTION,
    )

    private fun spaceLabel(localeTag: String): String {
        val locale = Locale.forLanguageTag(localeTag.ifBlank { "en-US" })
        if (locale.language == "en") return "space"
        val display = locale.getDisplayLanguage(locale).takeIf { it.isNotBlank() }
            ?: locale.getDisplayLanguage(Locale.ENGLISH)
        val normalized = display.ifBlank { "English" }
        return normalized.replaceFirstChar { it.uppercase(locale) }
    }

    private val letterHintLabels = mapOf(
        'q' to "%",
        'w' to "'",
        'e' to "\"",
        'r' to "_",
        't' to "[",
        'y' to "]",
        'u' to "<",
        'i' to ">",
        'o' to "{",
        'p' to "}",
        'a' to "@",
        's' to "#",
        'd' to "$",
        'f' to "&",
        'g' to "-",
        'h' to "+",
        'j' to "(",
        'k' to ")",
        'l' to "/",
        'z' to "*",
        'x' to "\"",
        'c' to "'",
        'v' to ":",
        'b' to ";",
        'n' to "!",
        'm' to "?",
    )

    private val letterPopupChars = mapOf(
        'q' to listOf("%", "1"),
        'w' to listOf("'", "2"),
        'e' to listOf("\"", "3", "\u00E9", "\u00E8", "\u00EA", "\u00EB"),
        'r' to listOf("_", "4"),
        't' to listOf("[", "]", "5"),
        'y' to listOf("]", "6"),
        'u' to listOf("<", ">", "7", "\u00FA", "\u00F9", "\u00FB", "\u00FC"),
        'i' to listOf(">", "8", "\u00ED", "\u00EC", "\u00EE", "\u00EF"),
        'o' to listOf("{", "}", "9", "\u00F3", "\u00F2", "\u00F4", "\u00F6"),
        'p' to listOf("}", "0"),
        'a' to listOf("@", "\u00E1", "\u00E0", "\u00E2", "\u00E4"),
        's' to listOf("#", "\u00DF"),
        'd' to listOf("$"),
        'f' to listOf("&"),
        'g' to listOf("-"),
        'h' to listOf("+"),
        'j' to listOf("("),
        'k' to listOf(")"),
        'l' to listOf("/"),
        'z' to listOf("*"),
        'x' to listOf("\""),
        'c' to listOf("'", "\u00E7"),
        'v' to listOf(":"),
        'b' to listOf(";"),
        'n' to listOf("!", "\u00F1"),
        'm' to listOf("?"),
    )

    private val defaultEmojiPage = listOf(
        "\uD83D\uDE00",
        "\uD83D\uDE03",
        "\uD83D\uDE04",
        "\uD83D\uDE01",
        "\uD83D\uDE06",
        "\uD83D\uDE02",
        "\uD83E\uDD23",
        "\u263A\uFE0F",
        "\uD83D\uDE0A",
        "\uD83D\uDE07",
        "\uD83D\uDE0D",
        "\uD83E\uDD70",
        "\uD83D\uDE18",
        "\uD83D\uDE17",
        "\uD83E\uDD29",
        "\uD83E\uDD73",
        "\uD83D\uDE0E",
        "\uD83E\uDD13",
        "\uD83E\uDD14",
        "\uD83D\uDE2D",
        "\uD83D\uDE21",
        "\uD83D\uDC4D",
        "\u2764\uFE0F",
        "\uD83D\uDD25",
    )
}
