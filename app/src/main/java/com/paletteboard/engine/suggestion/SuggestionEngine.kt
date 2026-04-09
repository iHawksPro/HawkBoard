package com.paletteboard.engine.suggestion

import android.content.Context
import kotlin.math.abs

class SuggestionEngine(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val lexiconCache = mutableMapOf<String, LexiconData>()
    private val predictionCache = linkedMapOf<String, List<String>>()

    fun predict(prefix: String, localeTag: String): List<String> {
        val normalized = prefix.lowercase().trim()
        val localeKey = normalizeLocale(localeTag)
        val cacheKey = "$localeKey|$normalized"
        synchronized(predictionCache) {
            predictionCache[cacheKey]?.let { return it }
        }

        val lexicon = lexiconData(localeTag)
        if (normalized.isBlank()) {
            val fallback = lexicon.words.take(3).map { it.text }
            rememberPrediction(cacheKey, fallback)
            return fallback
        }

        val results = candidatePool(normalized, lexicon)
            .asSequence()
            .map { candidate ->
                candidate to scoreCandidate(normalized, candidate)
            }
            .filter { (_, score) -> score < 58f }
            .sortedBy { it.second }
            .map { it.first.text }
            .distinct()
            .take(3)
            .toList()
            .ifEmpty {
                fallbackCandidates(normalized, lexicon)
                    .take(3)
                    .map { it.text }
            }
        rememberPrediction(cacheKey, results)
        return results
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
        val frequencyPenalty = candidate.rank * 0.0018f
        val prefixBonus = when {
            word == input -> -34f
            word.startsWith(input) -> -24f
            word.startsWith(input.take(2.coerceAtMost(input.length))) -> -14f
            word.firstOrNull() == input.firstOrNull() -> -7f
            else -> 0f
        }
        val editPenalty = levenshtein(input, word).toFloat() * 6.2f
        val lengthPenalty = abs(word.length - input.length) * 2.3f
        val containPenalty = if (word.contains(input)) -3f else 0f
        return frequencyPenalty + prefixBonus + editPenalty + lengthPenalty + containPenalty
    }

    private fun candidatePool(
        normalized: String,
        lexicon: LexiconData,
    ): List<WeightedWord> {
        val pool = LinkedHashSet<WeightedWord>()
        val firstLetter = normalized.firstOrNull()

        if (normalized.length >= 3) {
            pool += lexicon.prefix3Index[normalized.take(3)].orEmpty().take(180)
        }
        if (normalized.length >= 2) {
            pool += lexicon.prefix2Index[normalized.take(2)].orEmpty().take(220)
        }
        if (firstLetter != null) {
            pool += lexicon.firstLetterIndex[firstLetter].orEmpty().take(240)
        }

        if (pool.size < 90) {
            lexicon.words.asSequence()
                .filter { candidate ->
                    candidate.text.startsWith(normalized) ||
                        candidate.text.contains(normalized) ||
                        (firstLetter != null && candidate.text.firstOrNull() == firstLetter && abs(candidate.text.length - normalized.length) <= 2)
                }
                .take(220)
                .forEach(pool::add)
        }

        return pool.toList()
    }

    private fun fallbackCandidates(
        normalized: String,
        lexicon: LexiconData,
    ): List<WeightedWord> {
        val firstLetter = normalized.firstOrNull()
        val firstLetterMatches = firstLetter?.let { lexicon.firstLetterIndex[it] }.orEmpty()
        return firstLetterMatches.ifEmpty { lexicon.words.take(24) }
    }

    private fun rememberPrediction(
        cacheKey: String,
        predictions: List<String>,
    ) {
        synchronized(predictionCache) {
            predictionCache[cacheKey] = predictions
            while (predictionCache.size > MAX_PREDICTION_CACHE_SIZE) {
                val oldestKey = predictionCache.entries.firstOrNull()?.key ?: break
                predictionCache.remove(oldestKey)
            }
        }
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

        val firstLetterIndex = linkedMapOf<Char, MutableList<WeightedWord>>()
        val prefix2Index = linkedMapOf<String, MutableList<WeightedWord>>()
        val prefix3Index = linkedMapOf<String, MutableList<WeightedWord>>()
        distinctWords.forEach { word ->
            firstLetterIndex.getOrPut(word.text.first()) { mutableListOf() }.add(word)
            if (word.text.length >= 2) {
                prefix2Index.getOrPut(word.text.take(2)) { mutableListOf() }.add(word)
            }
            if (word.text.length >= 3) {
                prefix3Index.getOrPut(word.text.take(3)) { mutableListOf() }.add(word)
            }
        }

        return LexiconData(
            words = distinctWords,
            firstLetterIndex = firstLetterIndex,
            prefix2Index = prefix2Index,
            prefix3Index = prefix3Index,
        )
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
        val firstLetterIndex: Map<Char, List<WeightedWord>>,
        val prefix2Index: Map<String, List<WeightedWord>>,
        val prefix3Index: Map<String, List<WeightedWord>>,
    )

    private data class WeightedWord(
        val text: String,
        val rank: Int,
    )

    private companion object {
        const val MAX_PREDICTION_CACHE_SIZE = 180
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
