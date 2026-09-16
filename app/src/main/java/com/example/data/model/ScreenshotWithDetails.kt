package com.example.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ScreenshotWithDetails(
    @Embedded
    val screenshot: ScreenshotEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ScreenshotTagCrossRef::class,
            parentColumn = "screenshotId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
