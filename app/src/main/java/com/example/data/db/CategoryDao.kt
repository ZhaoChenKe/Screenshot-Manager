package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

data class CategoryWithCount(
    val id: String,
    val name: String,
    val icon: String,
    val sort: Int,
    val count: Int
)

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sort ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("""
        SELECT c.id, c.name, c.icon, c.sort, COUNT(s.id) as count
        FROM categories c
        LEFT JOIN screenshots s ON c.id = s.categoryId
        GROUP BY c.id
        ORDER BY c.sort ASC
    """)
    fun getCategoriesWithCountFlow(): Flow<List<CategoryWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)
}
