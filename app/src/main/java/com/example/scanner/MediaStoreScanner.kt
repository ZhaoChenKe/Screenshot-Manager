package com.example.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.ScreenshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MediaStoreScanner(private val context: Context) {

    companion object {
        private const val TAG = "MediaStoreScanner"

        // Keywords in path or filename commonly used by Xiaomi, Huawei, OPPO, vivo, Honor, Samsung, Pixel
        private val SCREENSHOT_PATH_KEYWORDS = listOf(
            "screenshot",
            "screenshots",
            "screencapture",
            "screen_shot",
            "screen_capture",
            "截屏",
            "屏幕截图"
        )

        private val SCREENSHOT_FILE_PREFIXES = listOf(
            "screenshot",
            "screen_shot",
            "screencapture",
            "截屏",
            "svid_",
            "sec_screencapture",
            "capture_"
        )
    }

    suspend fun scanScreenshots(): List<ScreenshotEntity> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<ScreenshotEntity>()
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Images.Media.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            projection.add(MediaStore.Images.Media.DATA)
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                null,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateModCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val widthCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else -1

                @Suppress("DEPRECATION")
                val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    c.getColumnIndex(MediaStore.Images.Media.DATA)
                } else -1

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol) ?: "Screenshot_$id.jpg"
                    val dateAdded = c.getLong(dateAddedCol) * 1000L
                    val dateMod = c.getLong(dateModCol) * 1000L
                    val width = c.getInt(widthCol)
                    val height = c.getInt(heightCol)
                    val size = c.getLong(sizeCol)

                    val relativePath = if (relativePathCol != -1) c.getString(relativePathCol) else null
                    val dataPath = if (dataCol != -1) c.getString(dataCol) else null

                    if (isScreenshot(name, relativePath, dataPath)) {
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        // Perceptual / duplicate detection signature
                        val hashSignature = if (size > 0 && width > 0 && height > 0) {
                            "${width}x${height}_${size}"
                        } else {
                            ""
                        }

                        val createTime = if (dateAdded > 0) dateAdded else dateMod

                        resultList.add(
                            ScreenshotEntity(
                                id = 0,
                                uri = contentUri.toString(),
                                fileName = name,
                                createTime = createTime,
                                modifyTime = dateMod,
                                ocrText = "",
                                title = "",
                                summary = "",
                                categoryId = "other",
                                isProcessed = false,
                                width = width,
                                height = height,
                                fileSize = size,
                                hash = hashSignature
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning screenshots from MediaStore", e)
        }

        Log.d(TAG, "Scan completed, found ${resultList.size} candidate screenshots")
        return@withContext resultList
    }

    private fun isScreenshot(name: String, relativePath: String?, dataPath: String?): Boolean {
        val lowerName = name.lowercase(Locale.getDefault())

        // 1. Check filename prefixes/patterns
        for (prefix in SCREENSHOT_FILE_PREFIXES) {
            if (lowerName.contains(prefix)) return true
        }

        // 2. Check Android 10+ Relative Path (e.g., Pictures/Screenshots/, DCIM/Screenshots/, etc.)
        if (!relativePath.isNullOrBlank()) {
            val lowerPath = relativePath.lowercase(Locale.getDefault())
            for (keyword in SCREENSHOT_PATH_KEYWORDS) {
                if (lowerPath.contains(keyword)) return true
            }
        }

        // 3. Check legacy full data path (e.g. /storage/emulated/0/Pictures/Screenshots/...)
        if (!dataPath.isNullOrBlank()) {
            val lowerData = dataPath.lowercase(Locale.getDefault())
            for (keyword in SCREENSHOT_PATH_KEYWORDS) {
                if (lowerData.contains("/$keyword") || lowerData.contains("$keyword/")) return true
            }
        }

        return false
    }
}
