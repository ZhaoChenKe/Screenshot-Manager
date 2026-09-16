package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.ScreenshotTagCrossRef
import com.example.data.model.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTagsFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: ScreenshotTagCrossRef)

    @Query("DELETE FROM screenshot_tag_cross_ref WHERE screenshotId = :screenshotId AND tagId = :tagId")
    suspend fun deleteCrossRef(screenshotId: Long, tagId: Long)

    @Query("DELETE FROM screenshot_tag_cross_ref WHERE screenshotId = :screenshotId")
    suspend fun deleteAllCrossRefsForScreenshot(screenshotId: Long)

    @Transaction
    suspend fun setTagsForScreenshot(screenshotId: Long, tagNames: List<String>) {
        deleteAllCrossRefsForScreenshot(screenshotId)
        for (rawName in tagNames) {
            val name = rawName.trim().removePrefix("#")
            if (name.isNotBlank()) {
                var tag = getTagByName(name)
                val tagId = if (tag != null) {
                    tag.id
                } else {
                    insertTag(TagEntity(name = name))
                }
                if (tagId > 0) {
                    insertCrossRef(ScreenshotTagCrossRef(screenshotId, tagId))
                }
            }
        }
    }
}
