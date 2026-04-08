package com.paletteboard.engine.gesture

import com.paletteboard.domain.model.GestureCandidate
import com.paletteboard.domain.model.GestureSample
import com.paletteboard.domain.model.KeyKind
import kotlin.math.abs
import kotlin.math.hypot

class GlideTypingEngine {
    fun decode(
        sample: GestureSample,
        lexicon: List<String>,
        sensitivity: Float = 0.5f,
    ): List<GestureCandidate> {
        val observedSequence = sample.traversedKeys
            .filter { it.spec.kind == KeyKind.CHARACTER && it.spec.label.firstOrNull()?.isLetter() == true }
            .mapNotNull { it.spec.label.firstOrNull()?.lowercaseChar() }
            .collapseAdjacent()

        if (observedSequence.size < 2) return emptyList()

        val characterKeys = sample.allKeys.filter {
            it.spec.kind == KeyKind.CHARACTER && it.spec.label.firstOrNull()?.isLetter() == true
        }
        val centroids = characterKeys.mapNotNull { geometry ->
            geometry.spec.label.firstOrNull()?.lowercaseChar()?.let { char ->
                char to Point(geometry.centerX(), geometry.centerY())
            }
        }.toMap()
        if (centroids.isEmpty()) return emptyList()

        val averageKeyWidth = characterKeys
            .map { it.right - it.left }
            .average()
            .toFloat()
            .coerceAtLeast(1f)
        val minSampleDistance = (averageKeyWidth * 0.14f).coerceAtLeast(4f)
        val simplifiedInput = simplifyPath(
            sample.points.map { Point(it.x, it.y) },
            minDistance = minSampleDistance,
        )
        val inputPath = resample(simplifiedInput, targetCount = 18)
        if (inputPath.size < 2) return emptyList()

        val strictMatches = scoreCandidates(
            observedSequence = observedSequence,
            inputPath = inputPath,
            averageKeyWidth = averageKeyWidth,
            centroids = centroids,
            lexicon = lexicon,
            sensitivity = sensitivity,
            requireEndMatch = true,
        )

        return if (strictMatches.size >= 4 || strictMatches.isNotEmpty()) {
            strictMatches.take(4)
        } else {
            scoreCandidates(
                observedSequence = observedSequence,
                inputPath = inputPath,
                averageKeyWidth = averageKeyWidth,
                centroids = centroids,
                lexicon = lexicon,
                sensitivity = sensitivity,
                requireEndMatch = false,
            ).take(4)
        }
    }

    private fun scoreCandidates(
        observedSequence: List<Char>,
        inputPath: List<Point>,
        averageKeyWidth: Float,
        centroids: Map<Char, Point>,
        lexicon: List<String>,
        sensitivity: Float,
        requireEndMatch: Boolean,
    ): List<GestureCandidate> {
        val firstObserved = observedSequence.first()
        val lastObserved = observedSequence.last()
        val maxSignatureDrift = if (sensitivity >= 0.65f) 4 else 3
        val pathLength = pathLength(inputPath)

        return lexicon.asSequence()
            .map { it.lowercase() }
            .mapNotNull { word ->
                val cleanedWord = word.filter { it in 'a'..'z' || it == '\'' }
                if (cleanedWord.length < 2) return@mapNotNull null
                val signature = cleanedWord.toList()
                    .filter { it in 'a'..'z' }
                    .collapseAdjacent()

                if (signature.size < 2) return@mapNotNull null
                if (signature.first() != firstObserved) return@mapNotNull null
                if (requireEndMatch && signature.last() != lastObserved) return@mapNotNull null
                if (abs(signature.size - observedSequence.size) > maxSignatureDrift) return@mapNotNull null

                val targetPath = signature.mapNotNull(centroids::get)
                if (targetPath.size < 2) return@mapNotNull null

                val sampledTarget = resample(targetPath, inputPath.size)
                val averagePathDistance = inputPath.zip(sampledTarget)
                    .sumOf { (left, right) -> distance(left, right).toDouble() }
                    .toFloat() / inputPath.size

                val pathPenalty = (averagePathDistance / averageKeyWidth) * 28f
                val sequencePenalty = levenshtein(signature, observedSequence).toFloat() *
                    (17f - sensitivity * 5f)
                val startPenalty = (distance(inputPath.first(), targetPath.first()) / averageKeyWidth) * 11f
                val endPenalty = (distance(inputPath.last(), targetPath.last()) / averageKeyWidth) *
                    if (signature.last() == lastObserved) 9f else 18f
                val observedCoveragePenalty = missingCoveragePenalty(observedSequence, signature) * 15f
                val targetCoveragePenalty = missingCoveragePenalty(signature, observedSequence) * 7f
                val travelPenalty = (abs(pathLength(sampledTarget) - pathLength) / averageKeyWidth) * 3.2f
                val exactPathBonus = if (signature == observedSequence) -12f else 0f

                GestureCandidate(
                    word = cleanedWord,
                    score = pathPenalty +
                        sequencePenalty +
                        startPenalty +
                        endPenalty +
                        observedCoveragePenalty +
                        targetCoveragePenalty +
                        travelPenalty +
                        exactPathBonus,
                )
            }
            .sortedBy { it.score }
            .take(24)
            .toList()
    }

    private fun simplifyPath(
        points: List<Point>,
        minDistance: Float,
    ): List<Point> {
        if (points.isEmpty()) return emptyList()
        val simplified = mutableListOf(points.first())
        points.drop(1).forEach { point ->
            if (distance(simplified.last(), point) >= minDistance) {
                simplified += point
            }
        }
        if (simplified.last() != points.last()) {
            simplified += points.last()
        }
        return simplified
    }

    private fun missingCoveragePenalty(
        expected: List<Char>,
        observed: List<Char>,
    ): Float = expected.count { it !in observed }.toFloat()

    private fun pathLength(path: List<Point>): Float = path
        .zipWithNext()
        .sumOf { (a, b) -> distance(a, b).toDouble() }
        .toFloat()

    private fun resample(path: List<Point>, targetCount: Int): List<Point> {
        if (path.isEmpty()) return emptyList()
        if (path.size == 1 || targetCount <= 1) return List(targetCount.coerceAtLeast(1)) { path.first() }

        val totalLength = pathLength(path)
        if (totalLength <= 0.001f) return List(targetCount) { path.first() }

        val step = totalLength / (targetCount - 1)
        val result = mutableListOf(path.first())
        var segmentStart = path.first()
        var segmentIndex = 1
        var carried = 0f

        while (segmentIndex < path.size && result.size < targetCount - 1) {
            val segmentEnd = path[segmentIndex]
            val segmentLength = distance(segmentStart, segmentEnd)
            if (carried + segmentLength >= step) {
                val ratio = (step - carried) / segmentLength
                val newPoint = Point(
                    x = segmentStart.x + (segmentEnd.x - segmentStart.x) * ratio,
                    y = segmentStart.y + (segmentEnd.y - segmentStart.y) * ratio,
                )
                result += newPoint
                segmentStart = newPoint
                carried = 0f
            } else {
                carried += segmentLength
                segmentStart = segmentEnd
                segmentIndex++
            }
        }

        while (result.size < targetCount) {
            result += path.last()
        }
        return result
    }

    private fun distance(left: Point, right: Point): Float = hypot(
        left.x - right.x,
        left.y - right.y,
    )

    private fun levenshtein(left: List<Char>, right: List<Char>): Int {
        val rows = Array(left.size + 1) { IntArray(right.size + 1) }
        for (i in left.indices) rows[i + 1][0] = i + 1
        for (j in right.indices) rows[0][j + 1] = j + 1

        for (i in 1..left.size) {
            for (j in 1..right.size) {
                val cost = if (left[i - 1] == right[j - 1]) 0 else 1
                rows[i][j] = minOf(
                    rows[i - 1][j] + 1,
                    rows[i][j - 1] + 1,
                    rows[i - 1][j - 1] + cost,
                )
            }
        }
        return rows[left.size][right.size]
    }

    private fun List<Char>.collapseAdjacent(): List<Char> = buildList {
        this@collapseAdjacent.forEach { value ->
            if (lastOrNull() != value) {
                add(value)
            }
        }
    }

    private data class Point(
        val x: Float,
        val y: Float,
    )
}
