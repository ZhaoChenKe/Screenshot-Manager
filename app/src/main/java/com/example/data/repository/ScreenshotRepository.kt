package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import com.example.ai.AiService
import com.example.ai.GeminiAiService
import com.example.classifier.ScreenshotClassifier
import com.example.data.db.AppDatabase
import com.example.data.db.CategoryWithCount
import com.example.data.model.CategoryEntity
import com.example.data.model.ScreenshotEntity
import com.example.data.model.ScreenshotWithDetails
import com.example.data.preferences.UserPreferencesRepository
import com.example.ocr.MlKitOcrService
import com.example.ocr.OcrService
import com.example.scanner.MediaStoreScanner
import com.example.similarity.SimilarGroup
import com.example.similarity.SimilarityDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ProcessingProgress(
    val isScanning: Boolean = false,
    val isProcessing: Boolean = false,
    val totalToProcess: Int = 0,
    val processedCount: Int = 0,
    val currentFileName: String = ""
)

class ScreenshotRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val scanner: MediaStoreScanner = MediaStoreScanner(context),
    private val ocrService: OcrService = MlKitOcrService(),
    private val aiService: AiService = GeminiAiService()
) {
    companion object {
        private const val TAG = "ScreenshotRepository"
    }

    private val screenshotDao = database.screenshotDao()
    private val categoryDao = database.categoryDao()
    private val tagDao = database.tagDao()

    private val _processingProgress = MutableStateFlow(ProcessingProgress())
    val processingProgress: StateFlow<ProcessingProgress> = _processingProgress.asStateFlow()

    val allScreenshots: Flow<List<ScreenshotWithDetails>> = screenshotDao.getAllScreenshotsFlow()
    val recentScreenshots: Flow<List<ScreenshotWithDetails>> = screenshotDao.getRecentScreenshotsFlow(30)
    val totalCount: Flow<Int> = screenshotDao.getTotalCountFlow()
    val unprocessedCount: Flow<Int> = screenshotDao.getUnprocessedCountFlow()
    val categoriesWithCount: Flow<List<CategoryWithCount>> = categoryDao.getCategoriesWithCountFlow()
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    fun search(query: String): Flow<List<ScreenshotWithDetails>> {
        return screenshotDao.searchScreenshotsFlow(query.trim())
    }

    fun getByCategory(categoryId: String): Flow<List<ScreenshotWithDetails>> {
        return screenshotDao.getScreenshotsByCategoryFlow(categoryId)
    }

    suspend fun getScreenshotWithDetailsById(id: Long): ScreenshotWithDetails? {
        return screenshotDao.getScreenshotWithDetailsById(id)
    }

    /**
     * Scans MediaStore for screenshots and updates local database
     */
    suspend fun scanAndSyncScreenshots(scope: CoroutineScope) = withContext(Dispatchers.IO) {
        if (_processingProgress.value.isScanning) return@withContext

        _processingProgress.value = _processingProgress.value.copy(isScanning = true)
        try {
            val scanned = scanner.scanScreenshots()
            val existingUris = screenshotDao.getAllExistingUris().toHashSet()

            val newItems = scanned.filter { it.uri !in existingUris }
            if (newItems.isNotEmpty()) {
                screenshotDao.insertScreenshots(newItems)
                Log.d(TAG, "Inserted ${newItems.size} new screenshots into Room")
            }

            // Trigger background processing of unprocessed items
            scope.launch(Dispatchers.IO) {
                processPendingScreenshots()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning screenshots", e)
        } finally {
            _processingProgress.value = _processingProgress.value.copy(isScanning = false)
        }
    }

    /**
     * Processes any screenshots that have not yet had OCR and categorization applied.
     */
    suspend fun processPendingScreenshots() = withContext(Dispatchers.IO) {
        if (_processingProgress.value.isProcessing) return@withContext

        val pending = screenshotDao.getUnprocessedScreenshots()
        if (pending.isEmpty()) return@withContext

        _processingProgress.value = _processingProgress.value.copy(
            isProcessing = true,
            totalToProcess = pending.size,
            processedCount = 0
        )

        val prefs = preferencesRepository.userPreferencesFlow.first()

        var currentProcessed = 0
        for (item in pending) {
            _processingProgress.value = _processingProgress.value.copy(
                currentFileName = item.fileName,
                processedCount = currentProcessed
            )

            processSingleScreenshot(item, prefs.enableAiAnalysis, prefs.customApiKey)

            currentProcessed++
            _processingProgress.value = _processingProgress.value.copy(
                processedCount = currentProcessed
            )
        }

        _processingProgress.value = _processingProgress.value.copy(
            isProcessing = false,
            currentFileName = ""
        )
    }

    private suspend fun processSingleScreenshot(
        item: ScreenshotEntity,
        enableAi: Boolean,
        customApiKey: String
    ) {
        try {
            val uri = Uri.parse(item.uri)

            // Step 1: Perform on-device OCR
            val recognizedText = ocrService.recognizeText(context, uri)

            // Step 2: Rule-based local classification & tag extraction
            val localResult = ScreenshotClassifier.classify(recognizedText, item.fileName, item.width, item.height)

            var finalTitle = localResult.title
            var finalCategory = localResult.categoryId
            var finalSummary = localResult.summary
            var finalTags = localResult.tags

            // Step 3: Optional Cloud AI enhancement if user enabled it
            if (enableAi && recognizedText.isNotBlank()) {
                val aiResult = aiService.analyzeScreenshot(
                    ocrText = recognizedText,
                    bitmap = null,
                    customApiKey = customApiKey
                )
                if (aiResult != null) {
                    if (aiResult.title.isNotBlank()) finalTitle = aiResult.title
                    if (aiResult.categoryId.isNotBlank()) finalCategory = aiResult.categoryId
                    if (aiResult.summary.isNotBlank()) finalSummary = aiResult.summary
                    if (aiResult.tags.isNotEmpty()) finalTags = aiResult.tags
                }
            }

            // Step 4: Update Screenshot entity
            val updated = item.copy(
                ocrText = recognizedText,
                title = finalTitle,
                summary = finalSummary,
                categoryId = finalCategory,
                isProcessed = true
            )
            screenshotDao.updateScreenshot(updated)

            // Step 5: Save tags in database
            tagDao.setTagsForScreenshot(item.id, finalTags)

            Log.d(TAG, "Processed screenshot ${item.id}: Category=$finalCategory, Title=$finalTitle")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing screenshot ${item.id}", e)
            // Mark processed even on failure to prevent infinite retry loops
            screenshotDao.updateScreenshot(item.copy(isProcessed = true))
        }
    }

    suspend fun reprocessScreenshot(screenshotId: Long) = withContext(Dispatchers.IO) {
        val item = screenshotDao.getScreenshotById(screenshotId) ?: return@withContext
        val prefs = preferencesRepository.userPreferencesFlow.first()
        processSingleScreenshot(item, prefs.enableAiAnalysis, prefs.customApiKey)
    }

    /**
     * Import a screenshot via Android Share (from WeChat, Browser, etc.)
     */
    suspend fun importFromUri(uri: Uri, fileName: String?): Long = withContext(Dispatchers.IO) {
        val name = fileName ?: "Shared_Screenshot_${System.currentTimeMillis()}.jpg"
        val entity = ScreenshotEntity(
            uri = uri.toString(),
            fileName = name,
            createTime = System.currentTimeMillis(),
            modifyTime = System.currentTimeMillis(),
            ocrText = "",
            title = "",
            summary = "",
            categoryId = "other",
            isProcessed = false
        )
        val id = screenshotDao.insertScreenshot(entity)
        if (id > 0) {
            val inserted = entity.copy(id = id)
            val prefs = preferencesRepository.userPreferencesFlow.first()
            processSingleScreenshot(inserted, prefs.enableAiAnalysis, prefs.customApiKey)
        }
        return@withContext id
    }

    suspend fun updateCategory(screenshotId: Long, categoryId: String) = withContext(Dispatchers.IO) {
        val item = screenshotDao.getScreenshotById(screenshotId) ?: return@withContext
        screenshotDao.updateScreenshot(item.copy(categoryId = categoryId))
    }

    suspend fun updateTags(screenshotId: Long, tags: List<String>) = withContext(Dispatchers.IO) {
        tagDao.setTagsForScreenshot(screenshotId, tags)
    }

    suspend fun deleteScreenshot(screenshotId: Long) = withContext(Dispatchers.IO) {
        tagDao.deleteAllCrossRefsForScreenshot(screenshotId)
        screenshotDao.deleteScreenshotById(screenshotId)
    }

    suspend fun deleteScreenshots(ids: List<Long>) = withContext(Dispatchers.IO) {
        for (id in ids) {
            tagDao.deleteAllCrossRefsForScreenshot(id)
        }
        screenshotDao.deleteScreenshotsByIds(ids)
    }

    suspend fun getPotentialDuplicates(): List<ScreenshotEntity> = withContext(Dispatchers.IO) {
        screenshotDao.getPotentialDuplicates()
    }

    suspend fun getSimilarAndDuplicateGroups(): List<SimilarGroup> = withContext(Dispatchers.IO) {
        val allScreenshots = screenshotDao.getAllScreenshots()
        SimilarityDetector.findSimilarGroups(allScreenshots)
    }

    /**
     * Generates and loads realistic sample screenshots for immediate preview
     */
    suspend fun loadSampleScreenshots() = withContext(Dispatchers.IO) {
        val sampleDir = File(context.filesDir, "sample_screenshots")
        if (!sampleDir.exists()) sampleDir.mkdirs()

        data class SampleData(
            val fileName: String,
            val title: String,
            val categoryId: String,
            val ocrText: String,
            val tags: List<String>,
            val summary: String,
            val headerColor: Int,
            val appTitle: String,
            val bodyLines: List<String>,
            val customWidth: Int = 540,
            val customHeight: Int = 960,
            val timeOffsetMs: Long = 0L
        )

        val samples = listOf(
            SampleData(
                fileName = "Screenshot_HonorOfKings_Victory.jpg",
                title = "王者荣耀·排位赛对局胜利",
                categoryId = "game",
                ocrText = "王者荣耀 胜利 VICTORY\n排位赛 · 荣耀王者 52星\n对局详情 比赛用时 18:32\nMVP 本局最佳：鲁班七号 (12/1/8)\n参团率 76% 伤害占比 38.5% 经济 13850\nKDA 20.0 评分 14.8\nS36赛季 赛季战绩结算",
                tags = listOf("游戏", "王者荣耀", "排位", "MVP", "战绩", "KDA"),
                summary = "排位赛荣耀王者52星对局胜利，MVP 鲁班七号 战绩 12/1/8，KDA 20.0 评分 14.8",
                headerColor = Color.parseColor("#7C3AED"),
                appTitle = "王者荣耀 · 对局战报",
                bodyLines = listOf("排位赛 · 荣耀王者52星 (对局胜利)", "MVP: 鲁班七号 (战绩 12/1/8)", "评分: 14.8 · KDA: 20.0 · 参团率 76%", "输出占比: 38.5% · 经济: 13,850"),
                customWidth = 960,
                customHeight = 540,
                timeOffsetMs = 0L
            ),
            SampleData(
                fileName = "Screenshot_Taobao_Keyboard_2026.jpg",
                title = "淘宝·机械键盘订单",
                categoryId = "shopping",
                ocrText = "淘宝 交易成功\n订单号：284918239102938\n商品：Keychron K3 Pro 双模矮轴机械键盘\n实付款：￥399.00\n发货快递：顺丰速运 SF13928472910\n交易时间：2026-09-14 18:20",
                tags = listOf("购物", "键盘", "数码", "顺丰"),
                summary = "Keychron 矮轴键盘订单已付款 399 元，顺丰单号 SF13928472910",
                headerColor = Color.parseColor("#FF5000"),
                appTitle = "淘宝 · 订单详情",
                bodyLines = listOf("交易状态：买家已付款", "商品：Keychron K3 Pro 机械键盘", "实付款：￥399.00", "运单号：SF13928472910"),
                timeOffsetMs = 3600000L
            ),
            SampleData(
                fileName = "Screenshot_SF_Express_2026.jpg",
                title = "顺丰速运·派件通知",
                categoryId = "express",
                ocrText = "顺丰速运\n运单号：SF13928472910\n快件正在派送中\n派件员：王师傅 13800138000\n预计今日 14:30 送达中关村南大街丰巢快递柜",
                tags = listOf("快递", "顺丰", "派送", "丰巢"),
                summary = "顺丰快件派送中，预计今日 14:30 投递至中关村丰巢快递柜",
                headerColor = Color.parseColor("#222222"),
                appTitle = "顺丰速运 · 运单追踪",
                bodyLines = listOf("运单号：SF13928472910", "状态：派送中", "派件员：王师傅 13800138000", "送达点：海淀区中关村丰巢柜"),
                timeOffsetMs = 7200000L
            ),
            SampleData(
                fileName = "Screenshot_SF_Express_Duplicate.jpg",
                title = "顺丰速运·派件通知(连拍重复项)",
                categoryId = "express",
                ocrText = "顺丰速运\n运单号：SF13928472910\n快件正在派送中\n派件员：王师傅 13800138000\n预计今日 14:30 送达中关村南大街丰巢快递柜",
                tags = listOf("快递", "顺丰", "派送", "丰巢"),
                summary = "顺丰快件派送中，预计今日 14:30 投递至中关村丰巢快递柜",
                headerColor = Color.parseColor("#222222"),
                appTitle = "顺丰速运 · 运单追踪",
                bodyLines = listOf("运单号：SF13928472910", "状态：派送中", "派件员：王师傅 13800138000", "送达点：海淀区中关村丰巢柜"),
                timeOffsetMs = 7205000L // 5 seconds apart: burst screenshot!
            ),
            SampleData(
                fileName = "Screenshot_Meeting_Notes_2026.jpg",
                title = "Q4产品规划会议纪要",
                categoryId = "work",
                ocrText = "飞书文档 · Q4移动端产品规划会议\n参会人：张伟、李莉、王强、陈工\n讨论要点：\n1. 截图管家本地离线 OCR 准确率达 98%\n2. Gemini 语义结构化提炼完成\n3. 下周三封版上线",
                tags = listOf("工作", "会议", "待办", "产品规划"),
                summary = "Q4移动端产品规划会议，讨论离线OCR与Gemini结构化分析，下周三封版",
                headerColor = Color.parseColor("#1B6AF4"),
                appTitle = "飞书文档 · 会议纪要",
                bodyLines = listOf("Q4移动端产品规划会议", "参会人：张伟、李莉、王强", "1. 离线OCR识别率达到98%", "2. 下周三完成封版上线"),
                timeOffsetMs = 10800000L
            ),
            SampleData(
                fileName = "Screenshot_Flight_AirChina_2026.jpg",
                title = "中国国航·北京-上海机票",
                categoryId = "travel",
                ocrText = "航旅纵横 · 行程提醒\n航班号：CA1831\n行程：北京首都 T3 -> 上海虹桥 T2\n起飞时间：09月20日 08:30\n登机口：C28 座位号：16A",
                tags = listOf("旅行", "机票", "国航", "行程"),
                summary = "09月20日国航 CA1831 航班，北京T3至上海虹桥T2，座位16A",
                headerColor = Color.parseColor("#C8102E"),
                appTitle = "航旅纵横 · 行程详情",
                bodyLines = listOf("航班：中国国际航空 CA1831", "行程：北京首都 T3 - 上海虹桥 T2", "时间：09月20日 08:30", "座位：16A (靠窗) · 登机口 C28"),
                timeOffsetMs = 14400000L
            ),
            SampleData(
                fileName = "Screenshot_Android_Kotlin_2026.jpg",
                title = "Kotlin 协程 StateFlow 笔记",
                categoryId = "doc",
                ocrText = "掘金技术专栏 · Kotlin 协程 Flow 实战\nStateFlow 是一个具备初始值的热流，适合用于 Jetpack Compose 中的 UI 状态管理\nviewModelScope.launch {\n  repository.flow.collectAsStateWithLifecycle()\n}",
                tags = listOf("学习", "Kotlin", "Android", "代码"),
                summary = "Kotlin 协程 StateFlow 原理笔记，初始值与防抖特性在 Compose 中的实战用法",
                headerColor = Color.parseColor("#7F52FF"),
                appTitle = "技术笔记 · Android 开发",
                bodyLines = listOf("Kotlin 协程 StateFlow 实战", "1. 具初始值的状态热流", "2. Compose UI 状态响应", "3. 结合 collectAsStateWithLifecycle"),
                timeOffsetMs = 18000000L
            )
        )

        for ((index, sample) in samples.withIndex()) {
            val file = File(sampleDir, sample.fileName)
            val width = sample.customWidth
            val height = sample.customHeight
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawColor(Color.parseColor("#F5F5F7"))

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Status bar
            paint.color = Color.parseColor("#1C1C1E")
            paint.textSize = 20f
            canvas.drawText("09:41", 36f, 44f, paint)
            canvas.drawText("5G · 100%", (width - 130).toFloat(), 44f, paint)

            // App Bar
            paint.color = sample.headerColor
            canvas.drawRect(0f, 64f, width.toFloat(), 150f, paint)

            paint.color = Color.WHITE
            paint.textSize = 28f
            paint.isFakeBoldText = true
            canvas.drawText(sample.appTitle, 36f, 120f, paint)

            // Main Card
            val cardRect = RectF(24f, 180f, (width - 24).toFloat(), (height - 60).toFloat())
            paint.color = Color.WHITE
            canvas.drawRoundRect(cardRect, 20f, 20f, paint)

            // Card Inner Header
            paint.color = Color.parseColor("#1C1C1E")
            paint.textSize = 26f
            paint.isFakeBoldText = true
            canvas.drawText(sample.title, 50f, 240f, paint)

            paint.color = Color.parseColor("#8E8E93")
            paint.textSize = 18f
            paint.isFakeBoldText = false
            canvas.drawText("截图时间：2026-09-15 09:41 · 截图管家已识别", 50f, 280f, paint)

            // Divider
            paint.color = Color.parseColor("#E5E5EA")
            paint.strokeWidth = 2f
            canvas.drawLine(50f, 310f, (width - 50).toFloat(), 310f, paint)

            // Body text lines
            paint.color = Color.parseColor("#2C2C2E")
            paint.textSize = 22f
            var yPos = 370f
            for (line in sample.bodyLines) {
                canvas.drawText(line, 50f, yPos, paint)
                yPos += 55f
            }

            // Tags Pill badge
            paint.color = Color.parseColor("#EFEFF4")
            val badgeRect = RectF(50f, yPos + 20f, 220f, yPos + 65f)
            canvas.drawRoundRect(badgeRect, 12f, 12f, paint)
            paint.color = sample.headerColor
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("# " + sample.tags.first(), 70f, yPos + 50f, paint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            val fileUri = Uri.fromFile(file).toString()
            val now = System.currentTimeMillis() - sample.timeOffsetMs
            val hashSig = "${width}x${height}_${file.length()}"
            val entity = ScreenshotEntity(
                uri = fileUri,
                fileName = sample.fileName,
                createTime = now,
                modifyTime = now,
                ocrText = sample.ocrText,
                title = sample.title,
                summary = sample.summary,
                categoryId = sample.categoryId,
                width = width,
                height = height,
                fileSize = file.length(),
                hash = hashSig,
                isProcessed = true
            )
            val id = screenshotDao.insertScreenshot(entity)
            if (id > 0) {
                tagDao.setTagsForScreenshot(id, sample.tags)
            }
        }
    }
}
