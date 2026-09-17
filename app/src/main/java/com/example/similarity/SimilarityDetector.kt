package com.example.similarity

import com.example.data.model.ScreenshotEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class SimilarityCategory {
    EXACT_DUPLICATE,   // 100% 完全重复
    HIGH_SIMILARITY    // 80%~99% 高度相似
}

data class SimilarGroup(
    val groupId: String,
    val category: SimilarityCategory,
    val similarityScore: Int,
    val reason: String,
    val items: List<ScreenshotEntity>,
    val recommendedKeepId: Long
)

object SimilarityDetector {

    /**
     * Analyzes a list of screenshots and groups exact duplicates and highly similar images.
     */
    fun findSimilarGroups(screenshots: List<ScreenshotEntity>): List<SimilarGroup> {
        if (screenshots.size < 2) return emptyList()

        val visited = mutableSetOf<Long>()
        val groups = mutableListOf<SimilarGroup>()

        // Sort by create time descending
        val sorted = screenshots.sortedByDescending { it.createTime }

        for (i in sorted.indices) {
            val base = sorted[i]
            if (base.id in visited) continue

            val matchedItems = mutableListOf(base)
            var highestScore = 0
            var highestCategory = SimilarityCategory.HIGH_SIMILARITY
            var bestReason = ""

            for (j in (i + 1) until sorted.size) {
                val candidate = sorted[j]
                if (candidate.id in visited) continue

                val (isMatch, category, score, reason) = compareScreenshots(base, candidate)
                if (isMatch) {
                    matchedItems.add(candidate)
                    visited.add(candidate.id)
                    if (score > highestScore) {
                        highestScore = score
                        highestCategory = category
                        bestReason = reason
                    }
                }
            }

            if (matchedItems.size > 1) {
                visited.add(base.id)

                // Pick the best screenshot to keep:
                // 1. Has non-empty title/summary
                // 2. Longer OCR text (more readable)
                // 3. Earliest or clearest
                val recommended = matchedItems.maxWithOrNull(
                    compareBy<ScreenshotEntity> { it.ocrText.length }
                        .thenBy { it.summary.length }
                        .thenBy { it.fileSize }
                ) ?: base

                val defaultReason = if (highestCategory == SimilarityCategory.EXACT_DUPLICATE) {
                    "相同文件指纹或完全一致的内容"
                } else {
                    "连续短时间截屏，画面与排版高度重叠"
                }

                groups.add(
                    SimilarGroup(
                        groupId = "group_${base.id}",
                        category = highestCategory,
                        similarityScore = if (highestScore > 0) highestScore else if (highestCategory == SimilarityCategory.EXACT_DUPLICATE) 100 else 88,
                        reason = if (bestReason.isNotBlank()) bestReason else defaultReason,
                        items = matchedItems,
                        recommendedKeepId = recommended.id
                    )
                )
            }
        }

        return groups.sortedWith(
            compareByDescending<SimilarGroup> { it.similarityScore }
                .thenByDescending { it.items.size }
        )
    }

    private data class CompareResult(
        val isMatch: Boolean,
        val category: SimilarityCategory,
        val score: Int,
        val reason: String
    )

    private fun compareScreenshots(a: ScreenshotEntity, b: ScreenshotEntity): CompareResult {
        // 1. Exact Duplicate by Hash Signature
        if (a.hash.isNotBlank() && a.hash == b.hash) {
            return CompareResult(
                isMatch = true,
                category = SimilarityCategory.EXACT_DUPLICATE,
                score = 100,
                reason = "尺寸与数字指纹 100% 相同"
            )
        }

        // 2. Exact Duplicate by File Size & Dimensions
        if (a.fileSize > 0 && a.fileSize == b.fileSize &&
            a.width > 0 && a.width == b.width &&
            a.height > 0 && a.height == b.height
        ) {
            return CompareResult(
                isMatch = true,
                category = SimilarityCategory.EXACT_DUPLICATE,
                score = 100,
                reason = "分辨率与文件大小完全一致"
            )
        }

        // 3. Exact Duplicate by OCR Text (Length >= 20 characters)
        val cleanA = a.ocrText.replace("\\s+".toRegex(), "")
        val cleanB = b.ocrText.replace("\\s+".toRegex(), "")
        if (cleanA.length >= 20 && cleanA == cleanB) {
            return CompareResult(
                isMatch = true,
                category = SimilarityCategory.EXACT_DUPLICATE,
                score = 100,
                reason = "文字内容 100% 相同"
            )
        }

        // 4. Burst / Rapid Screenshots within 20 seconds
        val timeDiff = abs(a.createTime - b.createTime)
        val isRapid = timeDiff <= 20_000L
        val sameDimensions = (a.width > 0 && a.width == b.width && a.height > 0 && a.height == b.height)

        if (isRapid && sameDimensions) {
            val minSize = min(a.fileSize, b.fileSize).toDouble()
            val maxSize = max(a.fileSize, b.fileSize).toDouble()
            val sizeRatio = if (maxSize > 0) minSize / maxSize else 1.0

            if (sizeRatio >= 0.85) {
                val score = (85 + (sizeRatio * 13)).toInt().coerceIn(88, 98)
                return CompareResult(
                    isMatch = true,
                    category = SimilarityCategory.HIGH_SIMILARITY,
                    score = score,
                    reason = "20秒内连续截屏，画面高度相似 (${score}%)"
                )
            }
        }

        // 5. OCR Text Overlap & Similarity (for non-rapid but visually same app screens)
        if (cleanA.length >= 25 && cleanB.length >= 25) {
            val similarity = calculateTextSimilarity(cleanA, cleanB)
            if (similarity >= 0.75) {
                val score = (similarity * 100).toInt().coerceIn(80, 99)
                val cat = if (score >= 98) SimilarityCategory.EXACT_DUPLICATE else SimilarityCategory.HIGH_SIMILARITY
                return CompareResult(
                    isMatch = true,
                    category = cat,
                    score = score,
                    reason = "界面文字相似度达 ${score}%"
                )
            }
        }

        return CompareResult(isMatch = false, category = SimilarityCategory.HIGH_SIMILARITY, score = 0, reason = "")
    }

    private fun calculateTextSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0

        // Use 2-gram overlap
        val bigrams1 = mutableMapOf<String, Int>()
        for (i in 0 until s1.length - 1) {
            val bg = s1.substring(i, i + 2)
            bigrams1[bg] = (bigrams1[bg] ?: 0) + 1
        }

        var intersection = 0
        var total = 0
        for (i in 0 until s2.length - 1) {
            val bg = s2.substring(i, i + 2)
            val count = bigrams1[bg] ?: 0
            if (count > 0) {
                intersection++
                bigrams1[bg] = count - 1
            }
            total++
        }

        val denom = (s1.length - 1) + total - intersection
        return if (denom > 0) intersection.toDouble() / denom else 0.0
    }
}
