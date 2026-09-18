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
     * Uses transitive clustering (connected components) so that groups with 3, 4 or more
     * similar/duplicate screenshots are properly grouped together into a single cluster.
     */
    fun findSimilarGroups(screenshots: List<ScreenshotEntity>): List<SimilarGroup> {
        if (screenshots.size < 2) return emptyList()

        val n = screenshots.size
        // Pre-compute pairwise matches
        val adj = Array(n) { mutableListOf<Pair<Int, CompareResult>>() }

        for (i in 0 until n) {
            val a = screenshots[i]
            for (j in (i + 1) until n) {
                val b = screenshots[j]
                val result = compareScreenshots(a, b)
                if (result.isMatch) {
                    adj[i].add(Pair(j, result))
                    adj[j].add(Pair(i, result))
                }
            }
        }

        // Find connected components using BFS/DFS
        val visited = BooleanArray(n)
        val groups = mutableListOf<SimilarGroup>()

        for (i in 0 until n) {
            if (visited[i] || adj[i].isEmpty()) continue

            val clusterIndices = mutableListOf<Int>()
            val queue = ArrayDeque<Int>()
            queue.add(i)
            visited[i] = true

            var highestScore = 0
            var hasExact = false
            var bestReason = ""

            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                clusterIndices.add(curr)

                for ((neighbor, result) in adj[curr]) {
                    if (result.category == SimilarityCategory.EXACT_DUPLICATE) {
                        hasExact = true
                    }
                    if (result.score > highestScore) {
                        highestScore = result.score
                        bestReason = result.reason
                    }
                    if (!visited[neighbor]) {
                        visited[neighbor] = true
                        queue.add(neighbor)
                    }
                }
            }

            if (clusterIndices.size > 1) {
                val clusterItems = clusterIndices.map { screenshots[it] }
                    .sortedByDescending { it.createTime }

                val recommended = clusterItems.maxWithOrNull(
                    compareBy<ScreenshotEntity> { it.ocrText.length }
                        .thenBy { it.summary.length }
                        .thenBy { it.fileSize }
                ) ?: clusterItems.first()

                val category = if (hasExact && highestScore >= 98) {
                    SimilarityCategory.EXACT_DUPLICATE
                } else {
                    SimilarityCategory.HIGH_SIMILARITY
                }

                val finalReason = if (bestReason.isNotBlank()) {
                    bestReason
                } else if (category == SimilarityCategory.EXACT_DUPLICATE) {
                    "相同文件特征或内容高度一致"
                } else {
                    "连拍截屏或界面内容高度重叠"
                }

                val baseItem = clusterItems.first()
                groups.add(
                    SimilarGroup(
                        groupId = "group_${baseItem.id}",
                        category = category,
                        similarityScore = if (highestScore > 0) highestScore else if (category == SimilarityCategory.EXACT_DUPLICATE) 100 else 88,
                        reason = finalReason,
                        items = clusterItems,
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

        // Clean OCR text
        val cleanA = a.ocrText.replace("\\s+".toRegex(), "")
        val cleanB = b.ocrText.replace("\\s+".toRegex(), "")

        // 3. Exact Duplicate by OCR Text (Length >= 10 characters)
        if (cleanA.length >= 10 && cleanA == cleanB) {
            return CompareResult(
                isMatch = true,
                category = SimilarityCategory.EXACT_DUPLICATE,
                score = 100,
                reason = "文字内容 100% 相同"
            )
        }

        val sameDimensions = (a.width > 0 && a.width == b.width && a.height > 0 && a.height == b.height)
        val timeDiff = abs(a.createTime - b.createTime)

        // 4. Consecutive / Burst screenshots with same dimensions within 3 minutes (180s)
        if (sameDimensions && timeDiff <= 180_000L) {
            val minSize = min(a.fileSize, b.fileSize).toDouble()
            val maxSize = max(a.fileSize, b.fileSize).toDouble()
            val sizeRatio = if (maxSize > 0) minSize / maxSize else 1.0

            // If file size ratio >= 0.70, it's almost certainly the same device & same app screen flow
            if (sizeRatio >= 0.70) {
                val score = (82 + (sizeRatio * 16)).toInt().coerceIn(85, 98)
                val timeDesc = if (timeDiff <= 20_000L) "连续快速截屏" else "相近时间连拍截屏"
                return CompareResult(
                    isMatch = true,
                    category = SimilarityCategory.HIGH_SIMILARITY,
                    score = score,
                    reason = "$timeDesc，画面排版高度一致 (${score}%)"
                )
            }
        }

        // 5. Same dimensions & very similar file size (within 5% difference) regardless of time
        if (sameDimensions && a.fileSize > 0 && b.fileSize > 0) {
            val minSize = min(a.fileSize, b.fileSize).toDouble()
            val maxSize = max(a.fileSize, b.fileSize).toDouble()
            val sizeRatio = minSize / maxSize
            if (sizeRatio >= 0.95) {
                // If OCR is also identical or nearly identical or empty (e.g. photo/graph screenshot)
                if (cleanA.isEmpty() && cleanB.isEmpty()) {
                    return CompareResult(
                        isMatch = true,
                        category = SimilarityCategory.HIGH_SIMILARITY,
                        score = 92,
                        reason = "同分辨率且文件大小极其接近 (92%)"
                    )
                }
            }
        }

        // 6. OCR Text Overlap & Similarity (for screens with recognizable text)
        if (cleanA.length >= 15 && cleanB.length >= 15) {
            val similarity = calculateTextSimilarity(cleanA, cleanB)
            if (similarity >= 0.65) {
                val score = (similarity * 100).toInt().coerceIn(75, 99)
                val cat = if (score >= 98) SimilarityCategory.EXACT_DUPLICATE else SimilarityCategory.HIGH_SIMILARITY
                return CompareResult(
                    isMatch = true,
                    category = cat,
                    score = score,
                    reason = "界面文本重合度达 ${score}%"
                )
            }
        }

        // 7. Same Title and Category with similar file size
        if (a.title.isNotBlank() && a.title == b.title && a.categoryId == b.categoryId && sameDimensions) {
            return CompareResult(
                isMatch = true,
                category = SimilarityCategory.HIGH_SIMILARITY,
                score = 88,
                reason = "同类别同标题截图，界面高度相近 (88%)"
            )
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
