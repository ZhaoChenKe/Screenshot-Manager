package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val fileName: String,
    val createTime: Long,
    val modifyTime: Long,
    val ocrText: String = "",
    val title: String = "",
    val summary: String = "",
    val categoryId: String = "other",
    val isProcessed: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val fileSize: Long = 0,
    val hash: String = ""
)
