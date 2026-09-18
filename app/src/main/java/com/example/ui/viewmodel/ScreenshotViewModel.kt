package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScreenshotApplication
import com.example.data.db.CategoryWithCount
import com.example.data.model.ScreenshotEntity
import com.example.data.model.ScreenshotWithDetails
import com.example.data.preferences.UserPreferences
import com.example.data.repository.ProcessingProgress
import com.example.similarity.SimilarGroup
import com.example.update.AppUpdateManager
import com.example.update.DownloadState
import com.example.update.UpdateCheckResult
import com.example.update.UpdateInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScreenshotViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ScreenshotApplication
    private val repository = app.repository
    private val preferencesRepo = app.preferencesRepository

    val processingProgress: StateFlow<ProcessingProgress> = repository.processingProgress

    val allScreenshots: StateFlow<List<ScreenshotWithDetails>> = repository.allScreenshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentScreenshots: StateFlow<List<ScreenshotWithDetails>> = repository.recentScreenshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unprocessedCount: StateFlow<Int> = repository.unprocessedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categoriesWithCount: StateFlow<List<CategoryWithCount>> = repository.categoriesWithCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences: StateFlow<UserPreferences> = preferencesRepo.userPreferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPreferences(
                autoScanNew = true,
                scanFrequencyMinutes = 60,
                enableAiAnalysis = false,
                customApiKey = "",
                aiProvider = "gemini",
                themeMode = "system",
                hasCompletedFirstScan = false,
                lastUpdateCheckTime = 0L,
                autoCheckUpdateFrequencyDays = 7,
                customUpdateUrl = ""
            )
        )

    val appUpdateManager = AppUpdateManager(application)

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val updateCheckResult: StateFlow<UpdateCheckResult> = _updateCheckResult.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    init {
        checkUpdateOnStartup()
    }

    val searchQuery = MutableStateFlow("")
    val selectedSearchCategory = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<ScreenshotWithDetails>> = searchQuery
        .combine(selectedSearchCategory) { query, category -> Pair(query, category) }
        .flatMapLatest { (query, category) ->
            if (query.isBlank() && category == null) {
                flowOf(emptyList())
            } else if (query.isBlank() && category != null) {
                repository.getByCategory(category)
            } else {
                repository.search(query).combine(selectedSearchCategory) { results, cat ->
                    if (cat == null) results else results.filter { it.screenshot.categoryId == cat }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _duplicateGroups = MutableStateFlow<List<List<ScreenshotEntity>>>(emptyList())
    val duplicateGroups: StateFlow<List<List<ScreenshotEntity>>> = _duplicateGroups.asStateFlow()

    private val _similarGroups = MutableStateFlow<List<SimilarGroup>>(emptyList())
    val similarGroups: StateFlow<List<SimilarGroup>> = _similarGroups.asStateFlow()

    private val _isCheckingDuplicates = MutableStateFlow(false)
    val isCheckingDuplicates: StateFlow<Boolean> = _isCheckingDuplicates.asStateFlow()

    init {
        // Automatically check duplicates in background
        checkDuplicates()
    }

    fun startScan() {
        // Use applicationScope so backgrounding the app or switching screens will continue scanning and processing
        app.applicationScope.launch {
            repository.scanAndSyncScreenshots(app.applicationScope)
            preferencesRepo.setHasCompletedFirstScan(true)
            checkDuplicates()
        }
    }

    fun loadSampleScreenshots(onCompleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.loadSampleScreenshots()
            checkDuplicates()
            onCompleted?.invoke()
        }
    }

    fun startProcessPending() {
        viewModelScope.launch {
            repository.processPendingScreenshots()
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectSearchCategory(categoryId: String?) {
        selectedSearchCategory.value = categoryId
    }

    suspend fun getScreenshotById(id: Long): ScreenshotWithDetails? {
        return repository.getScreenshotWithDetailsById(id)
    }

    fun updateCategory(screenshotId: Long, categoryId: String) {
        viewModelScope.launch {
            repository.updateCategory(screenshotId, categoryId)
        }
    }

    fun updateTitle(screenshotId: Long, title: String) {
        viewModelScope.launch {
            repository.updateTitle(screenshotId, title)
        }
    }

    fun updateTags(screenshotId: Long, tags: List<String>) {
        viewModelScope.launch {
            repository.updateTags(screenshotId, tags)
        }
    }

    suspend fun executeCheckDuplicates() {
        _isCheckingDuplicates.value = true
        try {
            // 1. Perceptual and content similarity analysis
            val similarResult = repository.getSimilarAndDuplicateGroups()
            _similarGroups.value = similarResult

            // 2. Legacy fallback grouping
            val legacy = similarResult.map { it.items }
            _duplicateGroups.value = legacy
        } catch (e: Exception) {
            // Fallback
            val potential = repository.getPotentialDuplicates()
            val groups = potential.groupBy { it.hash }.values.filter { it.size > 1 }.toList()
            _duplicateGroups.value = groups
        } finally {
            _isCheckingDuplicates.value = false
        }
    }

    fun checkDuplicates() {
        viewModelScope.launch {
            executeCheckDuplicates()
        }
    }

    fun deleteScreenshot(screenshotId: Long, onDeleted: (() -> Unit)? = null) {
        // Optimistic UI updates for immediate responsiveness
        _similarGroups.value = _similarGroups.value.mapNotNull { group ->
            val remainingItems = group.items.filter { it.id != screenshotId }
            if (remainingItems.size > 1) {
                group.copy(
                    items = remainingItems,
                    recommendedKeepId = if (group.recommendedKeepId == screenshotId) remainingItems.first().id else group.recommendedKeepId
                )
            } else null
        }
        _duplicateGroups.value = _duplicateGroups.value.mapNotNull { list ->
            val remaining = list.filter { it.id != screenshotId }
            if (remaining.size > 1) remaining else null
        }

        viewModelScope.launch {
            repository.deleteScreenshot(screenshotId)
            // Re-verify similarity groupings in background to ensure strict accuracy
            executeCheckDuplicates()
            onDeleted?.invoke()
        }
    }

    fun deleteScreenshots(ids: List<Long>, onDeleted: (() -> Unit)? = null) {
        val idSet = ids.toSet()
        // Optimistic UI updates for immediate responsiveness
        _similarGroups.value = _similarGroups.value.mapNotNull { group ->
            val remainingItems = group.items.filter { it.id !in idSet }
            if (remainingItems.size > 1) {
                group.copy(
                    items = remainingItems,
                    recommendedKeepId = if (group.recommendedKeepId in idSet) remainingItems.first().id else group.recommendedKeepId
                )
            } else null
        }
        _duplicateGroups.value = _duplicateGroups.value.mapNotNull { list ->
            val remaining = list.filter { it.id !in idSet }
            if (remaining.size > 1) remaining else null
        }

        viewModelScope.launch {
            repository.deleteScreenshots(ids)
            // Re-verify similarity groupings in background to ensure strict accuracy
            executeCheckDuplicates()
            onDeleted?.invoke()
        }
    }

    fun reprocessScreenshot(screenshotId: Long) {
        viewModelScope.launch {
            repository.reprocessScreenshot(screenshotId)
        }
    }

    fun batchUpdateCategory(ids: List<Long>, categoryId: String, onCompleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            for (id in ids) {
                repository.updateCategory(id, categoryId)
            }
            onCompleted?.invoke()
        }
    }

    fun importFromUri(uri: Uri, fileName: String?, onImported: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.importFromUri(uri, fileName)
            onImported?.invoke(id)
        }
    }

    fun setAutoScanNew(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setAutoScanNew(enabled)
        }
    }

    fun setEnableAiAnalysis(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setEnableAiAnalysis(enabled)
        }
    }

    fun setCustomApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepo.setCustomApiKey(apiKey)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepo.setThemeMode(mode)
        }
    }

    fun checkForUpdate(isManual: Boolean = false, simulateIfNoUrl: Boolean = false) {
        viewModelScope.launch {
            _updateCheckResult.value = UpdateCheckResult.Checking
            val prefs = userPreferences.value
            val result = appUpdateManager.checkForUpdate(prefs.customUpdateUrl, simulateIfNoUrl = simulateIfNoUrl)
            _updateCheckResult.value = result
            preferencesRepo.setLastUpdateCheckTime(System.currentTimeMillis())

            if (result is UpdateCheckResult.HasUpdate) {
                _showUpdateDialog.value = true
            }
        }
    }

    fun checkUpdateOnStartup() {
        viewModelScope.launch {
            val prefs = userPreferences.value
            if (appUpdateManager.shouldAutoCheck(prefs.lastUpdateCheckTime, prefs.autoCheckUpdateFrequencyDays)) {
                val result = appUpdateManager.checkForUpdate(prefs.customUpdateUrl, simulateIfNoUrl = false)
                _updateCheckResult.value = result
                preferencesRepo.setLastUpdateCheckTime(System.currentTimeMillis())
                if (result is UpdateCheckResult.HasUpdate) {
                    _showUpdateDialog.value = true
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
        _downloadState.value = DownloadState.Idle
    }

    fun startDownloadAndInstall(updateInfo: UpdateInfo) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(0, 0, (updateInfo.fileSizeMb * 1024 * 1024).toLong())
            val result = appUpdateManager.downloadAndInstallApk(updateInfo) { percent, downloaded, total ->
                _downloadState.value = DownloadState.Downloading(percent, downloaded, total)
            }
            if (result.isSuccess) {
                val file = result.getOrNull()
                _downloadState.value = DownloadState.Completed(
                    Uri.fromFile(file),
                    file?.absolutePath ?: ""
                )
            } else {
                _downloadState.value = DownloadState.Failed(result.exceptionOrNull()?.localizedMessage ?: "下载失败")
            }
        }
    }

    fun setAutoCheckUpdateFrequencyDays(days: Int) {
        viewModelScope.launch {
            preferencesRepo.setAutoCheckUpdateFrequencyDays(days)
        }
    }

    fun setCustomUpdateUrl(url: String) {
        viewModelScope.launch {
            preferencesRepo.setCustomUpdateUrl(url)
        }
    }
}
