package com.example.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateManager"
        const val DEFAULT_CHECK_INTERVAL_DAYS = 7 // 默认一周检查一次
        const val GITEE_OWNER = "zhao-chenke"
        const val GITEE_REPO = "Screenshot-Manager"
        // 默认更新源：Gitee Releases 最新发布版本接口（国内直连、极速无网络阻碍）
        const val DEFAULT_UPDATE_URL = "https://gitee.com/api/v5/repos/zhao-chenke/Screenshot-Manager/releases/latest"
        // GitHub 备用接口
        const val GITHUB_BACKUP_UPDATE_URL = "https://api.github.com/repos/ZhaoChenKe/Screenshot-Manager/releases/latest"
        const val DEMO_UPDATE_URL = DEFAULT_UPDATE_URL
    }

    /**
     * 获取当前安装的 App 版本号与版本名
     */
    fun getCurrentVersionInfo(): Pair<String, Int> {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val versionName = pInfo.versionName ?: "1.0.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
            Pair(versionName, versionCode)
        } catch (e: Exception) {
            Pair("1.0.0", 1)
        }
    }

    /**
     * 判断是否已达到自动检查更新周期（默认 7 天）
     */
    fun shouldAutoCheck(lastCheckTime: Long, intervalDays: Int = DEFAULT_CHECK_INTERVAL_DAYS): Boolean {
        if (intervalDays <= 0) return false // 0 表示关闭自动检查
        if (lastCheckTime <= 0L) return true // 从未检查过
        val intervalMillis = intervalDays * 24L * 60 * 60 * 1000L
        return (System.currentTimeMillis() - lastCheckTime) >= intervalMillis
    }

    /**
     * 检查版本更新
     * @param customUrl 用户在设置中自定义的更新接口地址，默认为官方 GitHub 仓库 Releases 接口
     * @param simulateIfNoUrl 当没有配置服务器或网络不可达时，是否允许进入演示升级模式
     */
    suspend fun checkForUpdate(
        customUrl: String?,
        simulateIfNoUrl: Boolean = false
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val (currentVersionName, currentVersionCode) = getCurrentVersionInfo()

        val urlStr = if (customUrl.isNullOrBlank()) DEFAULT_UPDATE_URL else customUrl.trim()

        try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 12000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json, application/json")
                setRequestProperty("User-Agent", "ScreenshotManager/${currentVersionName}")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)

                val updateInfo: UpdateInfo? = if (json.has("tag_name")) {
                    // Gitee or GitHub Releases API Format
                    if (urlStr.contains("gitee.com")) {
                        parseGiteeReleaseJson(json, currentVersionCode)
                    } else {
                        parseGitHubReleaseJson(json, currentVersionCode)
                    }
                } else if (json.has("versionCode") || json.has("versionName")) {
                    // Standard Custom version.json Format
                    val remoteVersionCode = json.optInt("versionCode", 1)
                    val remoteVersionName = json.optString("versionName", "1.0.1")
                    val changelog = json.optString("changelog", "1. 稳定性优化与问题修复")
                    val downloadUrl = json.optString("downloadUrl", "")
                    val fileSizeMb = json.optDouble("fileSizeMb", 15.0)
                    val isForceUpdate = json.optBoolean("isForceUpdate", false)
                    val publishDate = json.optString("publishDate", "")
                    UpdateInfo(
                        versionCode = remoteVersionCode,
                        versionName = remoteVersionName,
                        changelog = changelog,
                        downloadUrl = downloadUrl,
                        fileSizeMb = fileSizeMb,
                        isForceUpdate = isForceUpdate,
                        publishDate = publishDate
                    )
                } else {
                    null
                }

                if (updateInfo != null && isNewerVersion(updateInfo.versionName, currentVersionName, updateInfo.versionCode, currentVersionCode)) {
                    UpdateCheckResult.HasUpdate(updateInfo)
                } else {
                    UpdateCheckResult.UpToDate(currentVersionName, currentVersionCode)
                }
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // 404 表示 GitHub 仓库尚未发布任何 Release，当前即为最新
                if (simulateIfNoUrl) {
                    UpdateCheckResult.HasUpdate(getSimulatedUpdateInfo(currentVersionCode + 1))
                } else {
                    UpdateCheckResult.UpToDate(currentVersionName, currentVersionCode)
                }
            } else {
                if (simulateIfNoUrl) {
                    UpdateCheckResult.HasUpdate(getSimulatedUpdateInfo(currentVersionCode + 1))
                } else {
                    UpdateCheckResult.Error("检查失败 (HTTP $responseCode)")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch update from $urlStr: ${e.message}")
            if (simulateIfNoUrl) {
                UpdateCheckResult.HasUpdate(getSimulatedUpdateInfo(currentVersionCode + 1))
            } else {
                UpdateCheckResult.Error("无法连接到更新服务器: ${e.localizedMessage ?: "网络错误"}")
            }
        }
    }

    /**
     * 解析 Gitee Releases 最新发布数据
     */
    private fun parseGiteeReleaseJson(json: JSONObject, currentVersionCode: Int): UpdateInfo {
        val rawTag = json.optString("tag_name", "v1.0.0")
        val cleanVersionName = rawTag.removePrefix("v").removePrefix("V")
        val releaseName = json.optString("name", "新版本 $rawTag")
        val body = json.optString("body", "优化系统体验与多项问题修复")
        val publishedAt = json.optString("created_at", "").take(10)

        var downloadUrl = ""
        var fileSizeMb = 18.0

        // Gitee 的 release 附件通常存放在 attach_files 数组中，部分版本也兼容 assets
        val attachFiles = json.optJSONArray("attach_files") ?: json.optJSONArray("assets")
        if (attachFiles != null && attachFiles.length() > 0) {
            for (i in 0 until attachFiles.length()) {
                val asset = attachFiles.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("download_url", "")
                    if (downloadUrl.isBlank()) {
                        downloadUrl = asset.optString("browser_download_url", "")
                    }
                    val sizeBytes = asset.optLong("size", 0L)
                    if (sizeBytes > 0) {
                        fileSizeMb = Math.round((sizeBytes / (1024.0 * 1024.0)) * 10.0) / 10.0
                    }
                    break
                }
            }
        }

        if (downloadUrl.isBlank()) {
            downloadUrl = json.optString("html_url", "https://gitee.com/$GITEE_OWNER/$GITEE_REPO/releases")
        }

        val remoteVersionCode = parseSemanticVersionToCode(cleanVersionName, currentVersionCode + 1)

        val fullChangelog = if (body.isNotBlank()) {
            if (releaseName.isNotBlank() && !body.contains(releaseName)) {
                "$releaseName\n\n$body"
            } else {
                body
            }
        } else {
            "修复已知问题并提升稳定性"
        }

        return UpdateInfo(
            versionCode = remoteVersionCode,
            versionName = cleanVersionName,
            changelog = fullChangelog,
            downloadUrl = downloadUrl,
            fileSizeMb = fileSizeMb,
            isForceUpdate = false,
            publishDate = publishedAt
        )
    }

    /**
     * 解析 GitHub Releases 最新发布数据
     */
    private fun parseGitHubReleaseJson(json: JSONObject, currentVersionCode: Int): UpdateInfo {
        val rawTag = json.optString("tag_name", "v1.0.0")
        val cleanVersionName = rawTag.removePrefix("v").removePrefix("V")
        val releaseName = json.optString("name", "新版本 $rawTag")
        val body = json.optString("body", "优化系统体验与多项问题修复")
        val publishedAt = json.optString("published_at", "").take(10)

        var downloadUrl = ""
        var fileSizeMb = 18.0

        val assets = json.optJSONArray("assets")
        if (assets != null && assets.length() > 0) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url", "")
                    val sizeBytes = asset.optLong("size", 0L)
                    if (sizeBytes > 0) {
                        fileSizeMb = Math.round((sizeBytes / (1024.0 * 1024.0)) * 10.0) / 10.0
                    }
                    break
                }
            }
        }

        if (downloadUrl.isBlank()) {
            downloadUrl = json.optString("html_url", "https://github.com/ZhaoChenKe/Screenshot-Manager/releases")
        }

        val remoteVersionCode = parseSemanticVersionToCode(cleanVersionName, currentVersionCode + 1)

        val fullChangelog = if (body.isNotBlank()) {
            if (releaseName.isNotBlank() && !body.contains(releaseName)) {
                "$releaseName\n\n$body"
            } else {
                body
            }
        } else {
            "修复已知问题并提升稳定性"
        }

        return UpdateInfo(
            versionCode = remoteVersionCode,
            versionName = cleanVersionName,
            changelog = fullChangelog,
            downloadUrl = downloadUrl,
            fileSizeMb = fileSizeMb,
            isForceUpdate = false,
            publishDate = publishedAt
        )
    }

    /**
     * 判断远程版本是否高于本地版本（支持语义化版本对比，如 1.0.3 > 1.0.2）
     */
    private fun isNewerVersion(remoteTag: String, currentVerName: String, remoteCode: Int, currentCode: Int): Boolean {
        val cleanRemoteTag = remoteTag.removePrefix("v").removePrefix("V").trim()
        val cleanCurrentName = currentVerName.removePrefix("v").removePrefix("V").trim()

        // 1. 如果版本号完全相同，直接判定无更新
        if (cleanRemoteTag.equals(cleanCurrentName, ignoreCase = true)) {
            return false
        }

        // 2. 语义化版本分段比较（优先级最高，避免自动生成的 versionCode 算法误差）
        val rParts = cleanRemoteTag.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = cleanCurrentName.split(".").mapNotNull { it.toIntOrNull() }
        if (rParts.isNotEmpty() && cParts.isNotEmpty()) {
            val maxLen = maxOf(rParts.size, cParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            // 语义化完全一致
            return false
        }

        // 3. Fallback: 如果无法进行语义化比较，再比较 versionCode
        if (remoteCode > currentCode && remoteCode > 0 && currentCode > 0) {
            return true
        }

        return false
    }

    private fun parseSemanticVersionToCode(versionName: String, fallback: Int): Int {
        return try {
            val parts = versionName.split(".").mapNotNull { it.toIntOrNull() }
            if (parts.isNotEmpty()) {
                val major = parts.getOrElse(0) { 1 }
                val minor = parts.getOrElse(1) { 0 }
                val patch = parts.getOrElse(2) { 0 }
                major * 10000 + minor * 100 + patch
            } else {
                fallback
            }
        } catch (e: Exception) {
            fallback
        }
    }

    /**
     * 生成示例新版本（演示手机端直接更新交互）
     */
    fun getSimulatedUpdateInfo(targetVersionCode: Int = 2): UpdateInfo {
        return UpdateInfo(
            versionCode = targetVersionCode,
            versionName = "1.1.0",
            changelog = "1. 强化游戏战绩、段位与排位截图智能分类识别\n" +
                    "2. 新增高相似度与连拍图片聚合检测\n" +
                    "3. 新增最近截图批量多选与安全批量删除\n" +
                    "4. 全新极简双层自适应应用图标\n" +
                    "5. 手机端一键直接下载更新体验",
            downloadUrl = "https://example.com/screenshot_manager_v1.1.0.apk",
            fileSizeMb = 18.2,
            isForceUpdate = false,
            publishDate = "2026-09-17"
        )
    }

    /**
     * 下载 APK 文件并在完成后调起系统安装程序
     * 支持 GitHub 原始直链与国内镜像加速双通道，杜绝因下载失败退回到旧版本安装包
     */
     suspend fun downloadAndInstallApk(
        updateInfo: UpdateInfo,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            val apkFile = File(updatesDir, "screenshot_manager_v${updateInfo.versionName}.apk")
            if (apkFile.exists()) {
                apkFile.delete() // 确保清理旧缓存，防止安装旧文件
            }

            val rawUrl = updateInfo.downloadUrl
            val candidateUrls = mutableListOf<String>()

            if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                // 如果是 GitHub release 链接，优先尝试国内高速镜像，再尝试原链接
                if (rawUrl.contains("github.com") && rawUrl.contains("/releases/download/")) {
                    candidateUrls.add("https://ghproxy.net/$rawUrl")
                    candidateUrls.add("https://mirror.ghproxy.com/$rawUrl")
                }
                candidateUrls.add(rawUrl)
            }

            var isRealDownloadSuccess = false
            var lastError: Exception? = null

            for (urlToTry in candidateUrls) {
                try {
                    Log.d(TAG, "Trying to download APK from: $urlToTry")
                    val connection = openConnectionWithRedirects(urlToTry)

                    if (connection.responseCode in 200..299) {
                        val totalBytes = connection.contentLength.toLong().let { if (it <= 0) (updateInfo.fileSizeMb * 1024 * 1024).toLong() else it }
                        val input: InputStream = connection.inputStream
                        val output = FileOutputStream(apkFile)

                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloadedBytes: Long = 0

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 50
                            withContext(Dispatchers.Main) {
                                onProgress(percent, downloadedBytes, totalBytes)
                            }
                        }

                        output.flush()
                        output.close()
                        input.close()

                        // 验证下载文件大小（至少大于 5MB，避免下载到 404/html 错误页面）
                        if (apkFile.length() > 5 * 1024 * 1024) {
                            isRealDownloadSuccess = true
                            Log.d(TAG, "Successfully downloaded APK, size: ${apkFile.length()} bytes")
                            break
                        } else {
                            Log.w(TAG, "Downloaded file too small (${apkFile.length()} bytes), might be error page")
                            apkFile.delete()
                        }
                    } else {
                        Log.w(TAG, "HTTP error ${connection.responseCode} from $urlToTry")
                    }
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Download attempt failed for $urlToTry: ${e.message}")
                    if (apkFile.exists()) apkFile.delete()
                }
            }

            if (!isRealDownloadSuccess) {
                val errorMsg = lastError?.localizedMessage ?: "网络连接超时，请检查网络或点击在浏览器中下载"
                Log.e(TAG, "All download attempts failed: $errorMsg")
                return@withContext Result.failure(Exception("下载安装包失败: $errorMsg"))
            }

            // 完成下载，在主线程调起安装
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download and install failed", e)
            Result.failure(e)
        }
    }

    /**
     * 调用系统 PackageInstaller 安装已下载的 APK
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "安装包不存在或已损坏", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 针对 Android 8.0+ 校验未知应用安装权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hasInstallPermission = context.packageManager.canRequestPackageInstalls()
                if (!hasInstallPermission) {
                    val manageIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    Toast.makeText(context, "请在系统设置中允许此应用安装新版 APK", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Trigger install failed", e)
            Toast.makeText(context, "调起安装程序失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openConnectionWithRedirects(urlStr: String, maxRedirects: Int = 5): HttpURLConnection {
        var currentUrl = urlStr
        for (i in 0 until maxRedirects) {
            val url = URL(currentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 12000
            conn.readTimeout = 20000
            conn.setRequestProperty("User-Agent", "ScreenshotManager")
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == 307 || code == 308
            ) {
                val location = conn.getHeaderField("Location") ?: break
                conn.disconnect()
                currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                    location
                } else {
                    URL(url, location).toString()
                }
            } else {
                return conn
            }
        }
        val finalUrl = URL(currentUrl)
        val finalConn = finalUrl.openConnection() as HttpURLConnection
        finalConn.connectTimeout = 12000
        finalConn.readTimeout = 20000
        finalConn.setRequestProperty("User-Agent", "ScreenshotManager")
        return finalConn
    }
}
