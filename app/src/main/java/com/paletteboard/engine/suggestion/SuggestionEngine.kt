package com.paletteboard.engine.suggestion

import android.content.Context

class SuggestionEngine(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val lexiconCache = mutableMapOf<String, List<String>>()

    fun predict(prefix: String, localeTag: String): List<String> {
        val normalized = prefix.lowercase().trim()
        return lexicon(localeTag)
            .asSequence()
            .filter { normalized.isBlank() || it.startsWith(normalized) }
            .take(4)
            .toList()
    }

    fun lexicon(localeTag: String): List<String> {
        val key = normalizeLocale(localeTag)
        return synchronized(lexiconCache) {
            lexiconCache.getOrPut(key) {
                loadLexicon(key)
            }
        }
    }

    private fun loadLexicon(localeTag: String): List<String> {
        val assetPath = when (localeTag) {
            "en_us" -> "lexicon/en_us_common_words.txt"
            else -> "lexicon/en_us_common_words.txt"
        }

        val assetWords = runCatching {
            applicationContext.assets.open(assetPath).bufferedReader().useLines { lines ->
                lines
                    .map { it.trim().lowercase() }
                    .filter(::isUsableWord)
                    .take(8_000)
                    .toList()
            }
        }.getOrDefault(emptyList())

        return (assetWords + domainBoostWords)
            .distinct()
    }

    private fun normalizeLocale(localeTag: String): String = localeTag
        .replace('-', '_')
        .lowercase()

    private fun isUsableWord(word: String): Boolean {
        if (word.length !in 2..18) return false
        return word.all { it in 'a'..'z' || it == '\'' }
    }

    private companion object {
        val domainBoostWords = listOf(
            "android",
            "emoji",
            "gboard",
            "gesture",
            "glide",
            "hawk",
            "keyboard",
            "keyboards",
            "palette",
            "privacy",
            "settings",
            "spacebar",
            "swipe",
            "swiping",
            "theme",
            "themes",
            "toolbar",
            "typing",
        )
    }
}
