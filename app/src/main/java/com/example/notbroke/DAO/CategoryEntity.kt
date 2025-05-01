package com.example.notbroke.DAO

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import com.example.notbroke.models.Category
import com.example.notbroke.DAO.SyncStatus

/*** Entity class representing a category in the local database
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
            // Use the existing firestoreId or generate a new UUID
            val entityId = category.firestoreId ?: UUID.randomUUID().toString()
            return CategoryEntity(
                id = entityId,
                userId = category.userId,
                categoryName = category.categoryName,
                categoryType = category.categoryType.name,
                monthLimit = category.monthLimit,
                keyword = category.keyword,
                // Set appropriate sync status based on whether it's a new or existing category
                syncStatus = when {
                    category.firestoreId == null -> SyncStatus.PENDING_CREATE
                    else -> SyncStatus.SYNCED // Default for existing categories
                }
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