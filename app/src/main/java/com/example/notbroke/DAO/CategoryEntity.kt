package com.example.notbroke.DAO

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.notbroke.models.Category
import com.example.notbroke.DAO.SyncStatus

/**
 * Entity class representing a category in the local database
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val categoryName: String,
    val categoryType: String,
    val monthLimit: Double?,
    val keyword: String?,
    val syncStatus: SyncStatus = SyncStatus.SYNCED

) {
    // Convert from Category model to CategoryEntity
    companion object {
        fun fromCategory(category: Category): CategoryEntity {
            return CategoryEntity(
                id = category.firestoreId ?: category.id.toString(),
                userId = category.userId,
                categoryName = category.categoryName,
                categoryType = category.categoryType.name,
                monthLimit = category.monthLimit,
                keyword = category.keyword
            )
        }
    }

    // Convert from CategoryEntity to Category model
    fun toCategory(): Category {
        return Category(
            id = id.toLongOrNull() ?: 0L,
            firestoreId = id,
            userId = userId,
            categoryName = categoryName,
            categoryType = Category.Type.valueOf(categoryType),
            monthLimit = monthLimit,
            keyword = keyword
        )
    }
}