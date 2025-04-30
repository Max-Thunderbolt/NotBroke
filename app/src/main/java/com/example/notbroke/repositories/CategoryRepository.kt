package com.example.notbroke.repositories

import android.util.Log
import com.example.notbroke.DAO.CategoryDao
import com.example.notbroke.DAO.CategoryEntity
import com.example.notbroke.models.Category
import com.example.notbroke.services.FirestoreService
import com.example.notbroke.DAO.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first


class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val firestoreService: FirestoreService
) {
    private val TAG = "CategoryRepository"

    fun getAllCategories(userId: String): Flow<List<Category>> {
        return categoryDao.getAllCategories(userId).map { entities ->
            entities.map { it.toCategory() }
        }
    }

    fun getCategoryById(userId: String, categoryId: String): Flow<Category?> {
        return categoryDao.getCategoryById(categoryId).map { entity ->
            entity?.toCategory()
        }
    }

    fun getCategoriesByType(userId: String, type: Category.Type): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type.name, userId).map { entities ->
            entities.map { it.toCategory() }
        }
    }

    suspend fun saveCategory(category: Category) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving category: ${category.categoryName}")

            // Save to local database first with PENDING_UPLOAD status
            val categoryEntity = CategoryEntity.fromCategory(category)
                .copy(syncStatus = SyncStatus.PENDING_UPLOAD)
            categoryDao.insertCategory(categoryEntity)

            // Save to Firestore
            val firestoreResult = firestoreService.saveCategoryToFirestore(category, category.userId)

            firestoreResult.onSuccess { firestoreId ->
                Log.d(TAG, "Category uploaded to Firestore with ID: $firestoreId")
                // Update local database with Firestore ID and SYNCED status
                val updatedEntity = categoryEntity.copy(id = firestoreId, syncStatus = SyncStatus.SYNCED)
                categoryDao.updateCategory(updatedEntity)
            }.onFailure { e ->
                Log.e(TAG, "Failed to upload category to Firestore: ${e.message}", e)
                // Keep status as PENDING_UPLOAD if Firestore sync failed
            }

            Log.d(TAG, "Category save process initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving category", e)
            throw e
        }
    }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating category: ${category.categoryName}")

            // Update in local database with PENDING_UPDATE status
            val categoryEntity = CategoryEntity.fromCategory(category)
                .copy(syncStatus = SyncStatus.PENDING_UPDATE)
            categoryDao.updateCategory(categoryEntity)

            // Update in Firestore
            val firestoreResult = firestoreService.updateCategoryInFirestore(category)

            firestoreResult.onSuccess {
                Log.d(TAG, "Category updated in Firestore: ${category.firestoreId}")
                // Update sync status to SYNCED
                categoryDao.updateCategory(categoryEntity.copy(syncStatus = SyncStatus.SYNCED))
            }.onFailure { e ->
                Log.e(TAG, "Failed to update category in Firestore: ${e.message}", e)
                // Keep status as PENDING_UPDATE if update failed
            }

            Log.d(TAG, "Category update process initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating category", e)
            throw e
        }
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting category: ${category.categoryName}")

            // Get the category entity first
            val categoryEntity = categoryDao.getCategoryById(category.firestoreId ?: return@withContext).first()
            if (categoryEntity == null) {
                Log.w(TAG, "Category not found: ${category.categoryName}")
                return@withContext
            }

            // Mark for deletion in local database
            categoryDao.updateCategory(categoryEntity.copy(syncStatus = SyncStatus.PENDING_DELETE))

            // Try to delete from Firestore
            category.firestoreId?.let { firestoreId ->
                val firestoreResult = firestoreService.deleteCategoryFromFirestore(firestoreId)

                firestoreResult.onSuccess {
                    Log.d(TAG, "Category deleted from Firestore: ${category.categoryName}")
                    // Remove from local database after successful deletion from Firestore
                    categoryDao.deleteCategoryById(firestoreId)
                }.onFailure { e ->
                    Log.e(TAG, "Failed to delete category from Firestore: ${e.message}", e)
                    // Keep status as PENDING_DELETE if deletion failed
                }
            } ?: run {
                // If firestoreId is null, it means it was likely a local-only category
                categoryDao.deleteCategory(categoryEntity)
                Log.d(TAG, "Deleted local-only category: ${category.categoryName}")
            }

            Log.d(TAG, "Category deletion process initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting category", e)
            throw e
        }
    }

    suspend fun syncPendingCategories() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync of pending categories")

            // Get all pending categories
            val pendingCategories = categoryDao.getPendingSyncCategories(SyncStatus.SYNCED).first()

            for (categoryEntity in pendingCategories) {
                val category = categoryEntity.toCategory()

                when (categoryEntity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> {
                        Log.d(TAG, "Sync: Creating category: ${category.categoryName}")
                        val firestoreResult = firestoreService.saveCategoryToFirestore(category, category.userId)

                        firestoreResult.onSuccess { firestoreId ->
                            Log.d(TAG, "Sync: Created category ${category.categoryName}. Firestore ID: $firestoreId")
                            val updatedEntity = categoryEntity.copy(id = firestoreId, syncStatus = SyncStatus.SYNCED)
                            categoryDao.updateCategory(updatedEntity)
                        }.onFailure { e ->
                            Log.e(TAG, "Sync: Failed to create category ${category.categoryName}: ${e.message}", e)
                        }
                    }
                    SyncStatus.PENDING_UPLOAD -> {
                        Log.d(TAG, "Sync: Uploading category: ${category.categoryName}")
                        val firestoreResult = firestoreService.saveCategoryToFirestore(category, category.userId)

                        firestoreResult.onSuccess { firestoreId ->
                            Log.d(TAG, "Sync: Uploaded category ${category.categoryName}. Firestore ID: $firestoreId")
                            val updatedEntity = categoryEntity.copy(id = firestoreId, syncStatus = SyncStatus.SYNCED)
                            categoryDao.updateCategory(updatedEntity)
                        }.onFailure { e ->
                            Log.e(TAG, "Sync: Failed to upload category ${category.categoryName}: ${e.message}", e)
                        }
                    }
                    SyncStatus.PENDING_UPDATE -> {
                        Log.d(TAG, "Sync: Updating category: ${category.categoryName}")
                        category.firestoreId?.let { firestoreId ->
                            val firestoreResult = firestoreService.updateCategoryInFirestore(category)

                            firestoreResult.onSuccess {
                                Log.d(TAG, "Sync: Updated category ${category.categoryName}")
                                categoryDao.updateCategory(categoryEntity.copy(syncStatus = SyncStatus.SYNCED))
                            }.onFailure { e ->
                                Log.e(TAG, "Sync: Failed to update category ${category.categoryName}: ${e.message}", e)
                            }
                        }
                    }
                    SyncStatus.PENDING_DELETE -> {
                        Log.d(TAG, "Sync: Deleting category: ${category.categoryName}")
                        category.firestoreId?.let { firestoreId ->
                            val firestoreResult = firestoreService.deleteCategoryFromFirestore(firestoreId)

                            firestoreResult.onSuccess {
                                Log.d(TAG, "Sync: Deleted category ${category.categoryName}")
                                categoryDao.deleteCategoryById(firestoreId)
                            }.onFailure { e ->
                                Log.e(TAG, "Sync: Failed to delete category ${category.categoryName}: ${e.message}", e)
                            }
                        }
                    }
                    else -> {
                        Log.w(TAG, "Sync: Unknown sync status for category ${category.categoryName}: ${categoryEntity.syncStatus}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending categories", e)
            throw e
        }
    }

    suspend fun syncCategories(userId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting category sync for user: $userId")

            // First sync any pending local changes
            syncPendingCategories()

            // Then fetch all categories from Firestore
            firestoreService.observeCategories(userId).first().forEach { category ->
                try {
                    val categoryEntity = CategoryEntity.fromCategory(category).copy(syncStatus = SyncStatus.SYNCED)
                    categoryDao.insertCategory(categoryEntity)
                    Log.d(TAG, "Synced category from Firestore: ${category.categoryName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing category ${category.categoryName}: ${e.message}", e)
                }
            }

            Log.d(TAG, "Category sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during category sync", e)
            throw e
        }
    }
}