package com.example.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "screenshot_tag_cross_ref",
    primaryKeys = ["screenshotId", "tagId"],
    indices = [Index(value = ["tagId"])]
)
data class ScreenshotTagCrossRef(
    val screenshotId: Long,
    val tagId: Long
)
