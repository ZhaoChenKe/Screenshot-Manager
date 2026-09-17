package com.example.update

import android.net.Uri

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val fileSizeMb: Double = 0.0,
    val isForceUpdate: Boolean = false,
    val publishDate: String = ""
)

sealed interface UpdateCheckResult {
    data object Idle : UpdateCheckResult
    data object Checking : UpdateCheckResult
    data class HasUpdate(val updateInfo: UpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersionName: String, val currentVersionCode: Int) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState
    data class Completed(val apkUri: Uri, val localPath: String) : DownloadState
    data class Failed(val error: String) : DownloadState
}
