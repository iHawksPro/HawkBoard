package com.paletteboard.engine.emoji

import android.content.Context
import kotlin.math.abs

class EmojiCatalog(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val entries: List<EmojiEntry> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadEntries()
    }
    private val groups: List<String> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        entries.map { it.group }.distinct().ifEmpty { listOf(DEFAULT_GROUP) }
    }

    fun groups(): List<String> = groups

    fun pageForGroup(
        group: String,
        pageIndex: Int,
        pageSize: Int,
    ): EmojiPage {
        val normalizedGroup = group.takeIf { it.isNotBlank() } ?: groups.firstOrNull().orEmpty()
        val source = entries.filter { it.group == normalizedGroup }.ifEmpty { entries }
        return buildPage(source, pageIndex, pageSize)
    }

    fun search(
        query: String,
        limit: Int,
    ): List<EmojiEntry> {
        val normalized = query.lowercase().trim()
        if (normalized.isBlank()) return emptyList()
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        return entries.asSequence()
            .map { entry -> entry to scoreSearch(entry, normalized, tokens) }
            .filter { (_, score) -> score < SEARCH_REJECTION_THRESHOLD }
            .sortedBy { it.second }
            .map { it.first }
            .distinctBy { it.emoji }
            .take(limit)
            .toList()
    }

    private fun buildPage(
        source: List<EmojiEntry>,
        requestedPageIndex: Int,
        pageSize: Int,
    ): EmojiPage {
        val safePageSize = pageSize.coerceAtLeast(1)
        val totalPages = ((source.size + safePageSize - 1) / safePageSize).coerceAtLeast(1)
        val pageIndex = requestedPageIndex.coerceIn(0, totalPages - 1)
        val start = pageIndex * safePageSize
        val end = (start + safePageSize).coerceAtMost(source.size)
        return EmojiPage(
            entries = source.subList(start, end),
            pageIndex = pageIndex,
            totalPages = totalPages,
            canGoPrevious = pageIndex > 0,
            canGoNext = pageIndex < totalPages - 1,
        )
    }

    private fun scoreSearch(
        entry: EmojiEntry,
        query: String,
        tokens: List<String>,
    ): Int {
        val name = entry.name.lowercase()
        val group = entry.group.lowercase()
        val exactBonus = when {
            name == query -> -90
            name.startsWith(query) -> -50
            name.contains(query) -> -20
            else -> 0
        }
        val tokenPenalty = tokens.fold(0) { total, token ->
            total + when {
                name.startsWith(token) -> -14
                name.contains(token) -> -8
                group.contains(token) -> -3
                else -> 10
            }
        }
        val wordStartBonus = name.split(' ').count { it.startsWith(query) } * -6
        val lengthPenalty = abs(name.length - query.length)
        return exactBonus + tokenPenalty + wordStartBonus + lengthPenalty
    }

    private fun loadEntries(): List<EmojiEntry> {
        return runCatching {
            applicationContext.assets.open("emoji/emoji_catalog.tsv").bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val parts = line.split('\t')
                    if (parts.size < 3) return@mapNotNull null
                    EmojiEntry(
                        emoji = parts[0],
                        name = parts[1],
                        group = parts[2],
                    )
                }.toList()
            }
        }.getOrDefault(fallbackEntries)
    }

    data class EmojiEntry(
        val emoji: String,
        val name: String,
        val group: String,
    )

    data class EmojiPage(
        val entries: List<EmojiEntry>,
        val pageIndex: Int,
        val totalPages: Int,
        val canGoPrevious: Boolean,
        val canGoNext: Boolean,
    )

    private companion object {
        const val DEFAULT_GROUP = "Smileys & Emotion"
        const val SEARCH_REJECTION_THRESHOLD = 60
        val fallbackEntries = listOf(
            EmojiEntry("\uD83D\uDE00", "grinning face", DEFAULT_GROUP),
            EmojiEntry("\uD83D\uDE02", "face with tears of joy", DEFAULT_GROUP),
            EmojiEntry("\u2764\uFE0F", "red heart", "Symbols"),
            EmojiEntry("\uD83D\uDC4D", "thumbs up", "People & Body"),
            EmojiEntry("\uD83D\uDD25", "fire", "Travel & Places"),
        )
    }
}
