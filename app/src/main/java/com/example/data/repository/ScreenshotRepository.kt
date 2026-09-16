package com.example.data.repository

import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val localResult = ScreenshotClassifier.classify(recognizedText, item.fileName)

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
}
