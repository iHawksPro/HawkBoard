package com.paletteboard.engine.suggestion

import android.content.Context
import kotlin.math.abs

class SuggestionEngine(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val lexiconCache = mutableMapOf<String, LexiconData>()

    fun predict(prefix: String, localeTag: String): List<String> {
        val normalized = prefix.lowercase().trim()
        val lexicon = lexiconData(localeTag)
        if (normalized.isBlank()) {
            return lexicon.words.take(4).map { it.text }
        }

        return lexicon.words.asSequence()
            .filter { candidate ->
                candidate.text.startsWith(normalized) ||
                    candidate.text.contains(normalized) ||
                    (normalized.length >= 3 && abs(candidate.text.length - normalized.length) <= 2)
            }
            .map { candidate ->
                candidate to scoreCandidate(normalized, candidate)
            }
            .filter { (_, score) -> score < 80f }
            .sortedBy { it.second }
            .map { it.first.text }
            .distinct()
            .take(4)
            .toList()
            .ifEmpty {
                lexicon.words
                    .take(4)
                    .map { it.text }
            }
    }

    fun lexicon(localeTag: String): List<String> = lexiconData(localeTag).words.map { it.text }

    private fun lexiconData(localeTag: String): LexiconData {
        val key = normalizeLocale(localeTag)
        return synchronized(lexiconCache) {
            lexiconCache.getOrPut(key) {
                loadLexicon(key)
            }
        }
    }

    private fun scoreCandidate(
        input: String,
        candidate: WeightedWord,
    ): Float {
        val word = candidate.text
        val frequencyPenalty = candidate.rank * 0.0022f
        val prefixBonus = when {
            word == input -> -30f
            word.startsWith(input) -> -20f
            word.firstOrNull() == input.firstOrNull() -> -6f
            else -> 0f
        }
        val editPenalty = levenshtein(input, word).toFloat() * 8f
        val lengthPenalty = abs(word.length - input.length) * 1.7f
        val containPenalty = if (word.contains(input)) -4f else 0f
        return frequencyPenalty + prefixBonus + editPenalty + lengthPenalty + containPenalty
    }

    private fun loadLexicon(localeTag: String): LexiconData {
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

        val distinctWords = (assetWords + domainBoostWords)
            .distinct()
            .mapIndexed { index, word -> WeightedWord(text = word, rank = index) }

        return LexiconData(distinctWords)
    }

    private fun normalizeLocale(localeTag: String): String = localeTag
        .replace('-', '_')
        .lowercase()

    private fun isUsableWord(word: String): Boolean {
        if (word.length !in 2..18) return false
        return word.all { it in 'a'..'z' || it == '\'' }
    }

    private fun levenshtein(left: String, right: String): Int {
        val rows = Array(left.length + 1) { IntArray(right.length + 1) }
        for (i in left.indices) rows[i + 1][0] = i + 1
        for (j in right.indices) rows[0][j + 1] = j + 1

        for (i in 1..left.length) {
            for (j in 1..right.length) {
                val cost = if (left[i - 1] == right[j - 1]) 0 else 1
                rows[i][j] = minOf(
                    rows[i - 1][j] + 1,
                    rows[i][j - 1] + 1,
                    rows[i - 1][j - 1] + cost,
                )
            }
        }
        return rows[left.length][right.length]
    }

    private data class LexiconData(
        val words: List<WeightedWord>,
    )

    private data class WeightedWord(
        val text: String,
        val rank: Int,
    )

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
