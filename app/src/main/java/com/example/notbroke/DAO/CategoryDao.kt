package com.example.notbroke.DAO

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for categories
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY categoryName ASC")
    fun getAllCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getCategoryById(categoryId: String): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE categoryType = :type AND userId = :userId ORDER BY categoryName ASC")
    fun getCategoriesByType(type: String, userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE keyword LIKE '%' || :keyword || '%' AND userId = :userId")
    fun getCategoriesByKeyword(keyword: String, userId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: String)

    @Query("SELECT * FROM categories WHERE syncStatus != :status")
    fun getPendingSyncCategories(status: SyncStatus = SyncStatus.SYNCED): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("UPDATE categories SET monthLimit = :monthLimit WHERE id = :id")
    suspend fun updateCategoryMonthLimit(id: String, monthLimit: Double)
}