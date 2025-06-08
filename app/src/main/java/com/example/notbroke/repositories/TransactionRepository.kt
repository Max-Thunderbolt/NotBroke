package com.example.notbroke.repositories

import android.util.Log
import com.example.notbroke.DAO.TransactionDao
import com.example.notbroke.DAO.TransactionEntity
import com.example.notbroke.DAO.SyncStatus
import com.example.notbroke.models.Transaction
import com.example.notbroke.services.FirestoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Repository for handling transaction data from both local database and Firestore
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val firestoreService: FirestoreService
) {
    private val TAG = "TransactionRepository"

    // Get all transactions as Flow (modified in DAO to filter PENDING_DELETE)
    fun getAllTransactions(userId: String): Flow<List<Transaction>> = 
        transactionDao.getAllTransactions(userId = userId) // Uses the updated DAO method
            .map { entities -> 
                Log.d(TAG, "Retrieved ${entities.size} entities from DAO for user $userId")
                entities.forEach { entity ->
                    Log.d(TAG, "Entity: id=${entity.id}, syncStatus=${entity.syncStatus}, userId=${entity.userId}")
                }
                entities.map { it.toTransaction() }
            }

    /**
     * Save transaction to both local database and Firestore
     */
    suspend fun saveTransaction(transaction: Transaction, userId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving transaction: ${transaction.description}")

            // Generate a unique ID for the transaction if it doesn't have one
            val transactionWithId = if (transaction.firestoreId == null) {
                transaction.copy(
                    firestoreId = UUID.randomUUID().toString(),
                    userId = userId // Ensure userId is set
                )
            } else {
                transaction.copy(userId = userId) // Ensure userId is set
            }

            // Create entity from model using the companion object method
            val transactionEntity = TransactionEntity.fromTransaction(transactionWithId, userId = userId)
            
            // Insert into local database first
            transactionDao.insertTransaction(transactionEntity)
            Log.d(TAG, "Transaction saved locally with ID: ${transactionEntity.id}")

            // Try to save to Firestore immediately
            try {
                val firestoreResult = firestoreService.saveTransactionToFirestore(transactionWithId, userId)

                firestoreResult.onSuccess { firestoreId ->
                    Log.d(TAG, "Transaction uploaded to Firestore with ID: $firestoreId")
                    // Update local database with SYNCED status
                    val updatedEntity = transactionEntity.copy(
                        id = firestoreId, // Use the Firestore ID
                        syncStatus = SyncStatus.SYNCED
                    )
                    transactionDao.updateTransaction(updatedEntity)
                    Log.d(TAG, "Local transaction updated with Firestore ID and SYNCED status")
                }.onFailure { e ->
                    // Keep the PENDING status for retry during sync
                    Log.e(TAG, "Failed to upload transaction to Firestore: ${e.message}", e)
                }
            } catch (e: Exception) {
                // Log the error but don't throw - the transaction is saved locally and will sync later
                Log.e(TAG, "Error during Firestore upload: ${e.message}", e)
            }

            Log.d(TAG, "Transaction save process completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction", e)
            throw e
        }
    }

    /**
     * Save transaction to local database only (for offline support)
     */
    suspend fun saveTransactionOffline(transaction: Transaction, userId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving transaction offline: ${transaction.description}")

            // Create entity from model. `fromTransaction` will generate a local UUID
            // since firestoreId will be null for a new offline transaction.
            val transactionEntity = TransactionEntity.fromTransaction(transaction, userId = userId)
                .copy(syncStatus = SyncStatus.PENDING_UPLOAD) // Mark as pending upload

            // Insert into local database. Will use the generated UUID as the primary key.
            transactionDao.insertTransaction(transactionEntity)

            Log.d(TAG, "Transaction saved offline successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction offline", e)
            throw e
        }
    }

    /**
     * Update transaction in both local database and Firestore
     */
    suspend fun updateTransaction(transaction: Transaction, userId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating transaction: ${transaction.description}")

            // Ensure userId is set in the transaction
            val transactionWithUserId = transaction.copy(userId = userId)

            // Create entity from model. The entity's ID will be the existing firestoreId
            val transactionEntity = TransactionEntity.fromTransaction(transactionWithUserId, userId = userId)
                .copy(syncStatus = SyncStatus.PENDING_UPDATE) // Mark as pending update

            // Update in local database. Uses the existing firestoreId as the primary key.
            transactionDao.updateTransaction(transactionEntity)

            // Update in Firestore. Assumes the transaction model has the firestoreId.
            // updateTransactionInFirestore returns Result<Unit>
            val firestoreResult = firestoreService.updateTransactionInFirestore(transactionWithUserId)

            firestoreResult.onSuccess {
                Log.d(TAG, "Transaction updated in Firestore: ${transaction.firestoreId}")
                // Status will be SYNCED during the syncPendingTransactions process after successful Firestore update
                // Or you could mark as SYNCED here if you prefer immediate status update on success:
                // transactionDao.updateTransaction(transactionEntity.copy(syncStatus = SyncStatus.SYNCED))
            }.onFailure { e ->
                Log.e(TAG, "Failed to update transaction in Firestore: ${e.message}", e)
                // Keep status as PENDING_UPDATE if update failed
            }

            Log.d(TAG, "Transaction update process initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating transaction", e)
            throw e
        }
    }

    /**
     * Delete transaction from both local database and Firestore
     */
    suspend fun deleteTransaction(transaction: Transaction, userId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting transaction: ${transaction.description}")

            // Ensure userId is set in the transaction
            val transactionWithUserId = transaction.copy(userId = userId)

            // Create entity from model. The entity's ID will be the existing unique ID (Firestore or local UUID)
            val transactionEntity = TransactionEntity.fromTransaction(transactionWithUserId, userId = userId)
                .copy(syncStatus = SyncStatus.PENDING_DELETE) // Mark for deletion

            // Update status in local database. This will cause the transaction to be filtered out
            // by the updated getAllTransactions query in the DAO.
            transactionDao.updateTransaction(transactionEntity) // Mark for deletion

            // Attempt to delete from Firestore if it has a Firestore ID
            transaction.firestoreId?.let { firestoreId ->
                val firestoreResult = firestoreService.deleteTransactionFromFirestore(transactionId = firestoreId)

                firestoreResult.onSuccess {
                    Log.d(TAG, "Transaction deleted from Firestore: $firestoreId")
                    // The record will be physically removed from local DB during syncPendingTransactions
                    // after successful Firestore deletion.
                }.onFailure { e ->
                    Log.e(TAG, "Failed to delete transaction from Firestore: ${e.message}", e)
                    // Keep status as PENDING_DELETE if deletion failed, sync will retry.
                }
            } ?: run {
                // If firestoreId is null, it was a local-only transaction never uploaded.
                // Just delete it directly from the local database.
                Log.w(TAG, "Attempted to delete local-only transaction with null firestoreId: ${transaction.description}")
                transactionDao.deleteTransaction(transactionEntity) // Delete from Room
                Log.d(TAG, "Deleted local-only transaction: ${transaction.description}")
            }

            Log.d(TAG, "Transaction deletion process initiated (marked locally, attempting remote)")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting transaction", e)
            throw e
        }
    }

    /**
     * Get transactions by date range
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long, userId: String): Flow<List<Transaction>> {
        // This matches TransactionDao.getTransactionsByDateRange signature
        // The DAO query itself doesn't need sync status filtering here unless you want
        // date-ranged queries to also exclude PENDING_DELETE items.
        return transactionDao.getTransactionsByDateRange(startDate, endDate, userId)
            .map { entities -> entities.map { it.toTransaction() } }
    }

    /**
     * Get transactions by type
     */
    fun getTransactionsByType(type: Transaction.Type, userId: String): Flow<List<Transaction>> {
        // This matches TransactionDao.getTransactionsByType signature
        // The DAO query itself doesn't need sync status filtering here unless you want
        // type queries to also exclude PENDING_DELETE items.
        return transactionDao.getTransactionsByType(type.name, userId)
            .map { entities -> entities.map { it.toTransaction() } }
    }

    /**
     * Sync pending transactions with Firestore
     */
    suspend fun syncPendingTransactions(userId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync of pending transactions")

            // Get all pending transactions that are *not* SYNCED
            // Collect the *current* list using first()
            val pendingTransactionsEntities = transactionDao.getPendingSyncTransactions(userId = userId).first()

            // Process each pending transaction entity
            for (transactionEntity in pendingTransactionsEntities) {
                val transaction = transactionEntity.toTransaction() // Convert to model
                    .copy(userId = userId) // Ensure userId is set

                when (transactionEntity.syncStatus) {
                    SyncStatus.PENDING_UPLOAD, SyncStatus.PENDING_CREATE -> {
                        Log.d(TAG, "Sync: Handling PENDING_UPLOAD/CREATE transaction: ${transaction.description}")
                        // Attempt to upload new transactions to Firestore
                        val firestoreResult = firestoreService.saveTransactionToFirestore(transaction, userId)

                        firestoreResult.onSuccess { firestoreId ->
                            Log.d(TAG, "Sync: Uploaded transaction ${transaction.description}. Firestore ID: $firestoreId")
                            // Update local entity with Firestore ID and SYNCED status
                            val updatedEntity = transactionEntity.copy(id = firestoreId, syncStatus = SyncStatus.SYNCED)
                            transactionDao.updateTransaction(updatedEntity) // Update based on the new Firestore ID
                        }.onFailure { e ->
                            Log.e(TAG, "Sync: Failed to upload transaction ${transaction.description}: ${e.message}", e)
                            // Keep status as PENDING_UPLOAD/CREATE if upload failed, it will be retried.
                        }
                    }
                    SyncStatus.PENDING_UPDATE -> {
                        Log.d(TAG, "Sync: Updating transaction: ${transaction.description}")
                        transaction.firestoreId?.let { firestoreId ->
                            val firestoreResult = firestoreService.updateTransactionInFirestore(transaction)

                            firestoreResult.onSuccess {
                                Log.d(TAG, "Sync: Updated transaction ${transaction.description}. Firestore ID: $firestoreId")
                                transactionDao.updateTransaction(transactionEntity.copy(syncStatus = SyncStatus.SYNCED))
                            }.onFailure { e ->
                                Log.e(TAG, "Sync: Failed to update transaction ${transaction.description}: ${e.message}", e)
                            }
                        } ?: run {
                            Log.e(TAG, "Sync: Cannot update transaction with null firestoreId: ${transaction.description}. Marking as error or SYNCED with warning?")
                        }
                    }
                    SyncStatus.PENDING_DELETE -> {
                        Log.d(TAG, "Sync: Deleting transaction: ${transaction.description}")
                        transaction.firestoreId?.let { firestoreId ->
                            val firestoreResult = firestoreService.deleteTransactionFromFirestore(transactionId = firestoreId)

                            firestoreResult.onSuccess {

                            }.onFailure { e ->
                                Log.e(TAG, "Sync: Failed to delete transaction from Firestore: ${e.message}", e)
                            }
                        } ?: run {
                            Log.w(TAG, "Sync: Deleting local-only transaction with null firestoreId: ${transaction.description}")
                            transactionDao.deleteTransaction(transactionEntity)
                        }
                    }
                    SyncStatus.SYNCED -> {
                        // This case should ideally not be reached due to the query filtering.
                        Log.w(TAG, "Sync: Encountered a SYNCED transaction in pending list (should be filtered): ${transaction.description}")
                    }
                }
            }

            Log.d(TAG, "Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending transactions", e)
            throw e
        }
    }

    /**
     * Load transactions from Firestore and save to local database
     */
    suspend fun loadTransactionsFromFirestore(userId: String, startDate: Long, endDate: Long) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading transactions from Firestore for period: $startDate to $endDate")

            // Get transactions from Firestore
            val firestoreTransactions = firestoreService.getTransactionsForPeriod(userId, startDate, endDate)

            // Convert to entities. The fromTransaction will use the firestoreId as the entity ID.
            val firestoreEntities = firestoreTransactions.map {
                // When loading from Firestore, they are already synced
                // Ensure the firestoreId is present and used as the entity ID
                // Also ensure userId is set
                TransactionEntity.fromTransaction(it.copy(userId = userId), userId = userId)
                    .copy(syncStatus = SyncStatus.SYNCED)
            }

            // Insert fetched transactions. OnConflictStrategy.REPLACE will update existing entities
            // with the same Firestore ID or insert new ones.
            transactionDao.insertTransactions(firestoreEntities)

            Log.d(TAG, "Loaded and inserted ${firestoreTransactions.size} transactions from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading transactions from Firestore", e)
            throw e
        }
    }
    
    /**
     * Unified sync method that follows the pattern of other repositories
     * This method handles both downloading from Firestore and uploading pending changes
     */
    suspend fun syncTransactions(userId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting unified sync for transactions")
            
            // 1. First, sync any pending transactions to Firestore
            syncPendingTransactions(userId)
            
            // 2. Then, load transactions from Firestore for the user
            // We'll use a reasonable date range (e.g., last 3 months)
            val endDate = System.currentTimeMillis()
            val startDate = endDate - (90L * 24 * 60 * 60 * 1000) // 90 days ago
            
            loadTransactionsFromFirestore(userId, startDate, endDate)
            
            Log.d(TAG, "Unified sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during unified sync", e)
            throw e
        }
    }

    /**
     * Sync transactions in batches to improve performance
     */
    suspend fun syncTransactionsInBatches(userId: String, batchSize: Int = 50) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting batch sync of transactions")
            
            // Get all pending transactions
            val pendingTransactions = transactionDao.getPendingSyncTransactions(userId = userId).first()
            
            // Split into batches
            val batches = pendingTransactions.chunked(batchSize)
            
            for (batch in batches) {
                try {
                    // Process each transaction in the batch
                    for (transactionEntity in batch) {
                        val transaction = transactionEntity.toTransaction()
                            .copy(userId = userId) // Ensure userId is set
                        
                        when (transactionEntity.syncStatus) {
                            SyncStatus.PENDING_UPLOAD, SyncStatus.PENDING_CREATE -> {
                                val firestoreResult = firestoreService.saveTransactionToFirestore(transaction, userId)
                                firestoreResult.onSuccess { firestoreId ->
                                    val updatedEntity = transactionEntity.copy(
                                        id = firestoreId,
                                        syncStatus = SyncStatus.SYNCED
                                    )
                                    transactionDao.updateTransaction(updatedEntity)
                                }.onFailure { e ->
                                    Log.e(TAG, "Failed to upload transaction in batch: ${e.message}", e)
                                    // Keep PENDING status for retry
                                }
                            }
                            SyncStatus.PENDING_UPDATE -> {
                                transaction.firestoreId?.let { firestoreId ->
                                    val firestoreResult = firestoreService.updateTransactionInFirestore(transaction)
                                    firestoreResult.onSuccess {
                                        transactionDao.updateTransaction(
                                            transactionEntity.copy(syncStatus = SyncStatus.SYNCED)
                                        )
                                    }.onFailure { e ->
                                        Log.e(TAG, "Failed to update transaction in batch: ${e.message}", e)
                                        // Keep PENDING status for retry
                                    }
                                }
                            }
                            SyncStatus.PENDING_DELETE -> {
                                transaction.firestoreId?.let { firestoreId ->
                                    val firestoreResult = firestoreService.deleteTransactionFromFirestore(firestoreId)
                                    firestoreResult.onSuccess {
                                        transactionDao.deleteTransaction(transactionEntity)
                                    }.onFailure { e ->
                                        Log.e(TAG, "Failed to delete transaction in batch: ${e.message}", e)
                                        // Keep PENDING status for retry
                                    }
                                } ?: run {
                                    // Local-only transaction, delete directly
                                    transactionDao.deleteTransaction(transactionEntity)
                                }
                            }
                            else -> {
                                Log.w(TAG, "Unexpected sync status in batch: ${transactionEntity.syncStatus}")
                            }
                        }
                    }
                    
                    // Add a small delay between batches to prevent overwhelming the network
                    delay(100)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing batch: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "Batch sync completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during batch sync", e)
            throw e
        }
    }

    /**
     * Retry failed syncs with exponential backoff
     */
    private suspend fun retryFailedSync(
        transaction: Transaction,
        userId: String,
        maxRetries: Int = 3,
        initialDelay: Long = 1000
    ) {
        var currentRetry = 0
        var delay = initialDelay
        
        val transactionWithUserId = transaction.copy(userId = userId)
        
        while (currentRetry < maxRetries) {
            try {
                when {
                    transactionWithUserId.firestoreId == null -> {
                        val result = firestoreService.saveTransactionToFirestore(transactionWithUserId, userId)
                        if (result.isSuccess) {
                            return 
                        }
                    }
                    else -> {

                        val result = firestoreService.updateTransactionInFirestore(transactionWithUserId)
                        if (result.isSuccess) {
                            return
                        }
                    }
                }
                
                // If we get here, the operation failed
                currentRetry++
                if (currentRetry < maxRetries) {
                    delay *= 2 // Exponential backoff
                    delay(delay)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Retry attempt $currentRetry failed: ${e.message}", e)
                currentRetry++
                if (currentRetry < maxRetries) {
                    delay *= 2
                    delay(delay)
                }
            }
        }
        
        Log.e(TAG, "All retry attempts failed for transaction: ${transaction.description}")
    }

    fun getTotalSpendForCategoryInDateRange(userId: String, categoryName: String, startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getTotalSpendForCategoryInDateRange(userId, categoryName, startDate, endDate)
            .map { it ?: 0.0 } // Return 0.0 if sum is null (no transactions)
    }
}