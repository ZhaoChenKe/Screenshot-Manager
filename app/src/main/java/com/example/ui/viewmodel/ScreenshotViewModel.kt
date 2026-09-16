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
                hasCompletedFirstScan = false
            )
        )

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

    init {
        // Automatically check duplicates in background
        checkDuplicates()
    }

    fun startScan() {
        viewModelScope.launch {
            repository.scanAndSyncScreenshots(viewModelScope)
            preferencesRepo.setHasCompletedFirstScan(true)
            checkDuplicates()
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

    fun updateTags(screenshotId: Long, tags: List<String>) {
        viewModelScope.launch {
            repository.updateTags(screenshotId, tags)
        }
    }

    fun deleteScreenshot(screenshotId: Long, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteScreenshot(screenshotId)
            checkDuplicates()
            onDeleted?.invoke()
        }
    }

    fun deleteScreenshots(ids: List<Long>, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteScreenshots(ids)
            checkDuplicates()
            onDeleted?.invoke()
        }
    }

    fun reprocessScreenshot(screenshotId: Long) {
        viewModelScope.launch {
            repository.reprocessScreenshot(screenshotId)
        }
    }

    fun checkDuplicates() {
        viewModelScope.launch {
            val potential = repository.getPotentialDuplicates()
            val groups = potential.groupBy { it.hash }.values.filter { it.size > 1 }.toList()
            _duplicateGroups.value = groups
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
}
