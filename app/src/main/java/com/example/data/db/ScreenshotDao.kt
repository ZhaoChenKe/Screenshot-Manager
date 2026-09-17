package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ScreenshotEntity
import com.example.data.model.ScreenshotWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    @Transaction
    @Query("SELECT * FROM screenshots ORDER BY createTime DESC")
    fun getAllScreenshotsFlow(): Flow<List<ScreenshotWithDetails>>

    @Query("SELECT * FROM screenshots ORDER BY createTime DESC")
    suspend fun getAllScreenshots(): List<ScreenshotEntity>

    @Transaction
    @Query("SELECT * FROM screenshots ORDER BY createTime DESC LIMIT :limit")
    fun getRecentScreenshotsFlow(limit: Int = 30): Flow<List<ScreenshotWithDetails>>

    @Transaction
    @Query("SELECT * FROM screenshots WHERE id = :id")
    suspend fun getScreenshotWithDetailsById(id: Long): ScreenshotWithDetails?

    @Query("SELECT * FROM screenshots WHERE id = :id")
    suspend fun getScreenshotById(id: Long): ScreenshotEntity?

    @Transaction
    @Query("SELECT * FROM screenshots WHERE categoryId = :categoryId ORDER BY createTime DESC")
    fun getScreenshotsByCategoryFlow(categoryId: String): Flow<List<ScreenshotWithDetails>>

    @Transaction
    @Query("SELECT * FROM screenshots WHERE isProcessed = 0 ORDER BY createTime DESC")
    suspend fun getUnprocessedScreenshots(): List<ScreenshotEntity>

    @Query("SELECT COUNT(*) FROM screenshots")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE isProcessed = 0")
    fun getUnprocessedCountFlow(): Flow<Int>

    @Query("SELECT uri FROM screenshots")
    suspend fun getAllExistingUris(): List<String>

    @Query("SELECT * FROM screenshots WHERE uri = :uri LIMIT 1")
    suspend fun getScreenshotByUri(uri: String): ScreenshotEntity?

    @Transaction
    @Query("""
        SELECT * FROM screenshots 
        WHERE ocrText LIKE '%' || :keyword || '%'
           OR title LIKE '%' || :keyword || '%'
           OR summary LIKE '%' || :keyword || '%'
           OR fileName LIKE '%' || :keyword || '%'
        ORDER BY createTime DESC
    """)
    fun searchScreenshotsFlow(keyword: String): Flow<List<ScreenshotWithDetails>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScreenshot(screenshot: ScreenshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScreenshots(screenshots: List<ScreenshotEntity>): List<Long>

    @Update
    suspend fun updateScreenshot(screenshot: ScreenshotEntity)

    @Query("DELETE FROM screenshots WHERE id = :id")
    suspend fun deleteScreenshotById(id: Long)

    @Query("DELETE FROM screenshots WHERE id IN (:ids)")
    suspend fun deleteScreenshotsByIds(ids: List<Long>)

    // Duplicate detection based on non-empty hash
    @Query("""
        SELECT * FROM screenshots 
        WHERE hash != '' AND hash IN (
            SELECT hash FROM screenshots 
            WHERE hash != '' 
            GROUP BY hash 
            HAVING COUNT(*) > 1
        )
        ORDER BY hash ASC, createTime DESC
    """)
    suspend fun getPotentialDuplicates(): List<ScreenshotEntity>
}
