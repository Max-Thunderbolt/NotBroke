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
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID // Import UUID

/**
 * Repository for handling transaction data from both local database and Firestore
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val firestoreService: FirestoreService
) {
    private val TAG = "TransactionRepository"

    // Get all transactions as Flow (modified in DAO to filter PENDING_DELETE)
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions() // Uses the updated DAO method
        .map { entities -> entities.map { it.toTransaction() } }

    /**
     * Save transaction to both local database and Firestore
     */
    suspend fun saveTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving transaction: ${transaction.description}")

            // Create entity from model. `fromTransaction` in TransactionEntity
            // will generate a local UUID if firestoreId is null.
            val transactionEntity = TransactionEntity.fromTransaction(transaction)
                .copy(syncStatus = SyncStatus.PENDING_UPLOAD) // Mark as pending upload

            // Insert into local database.
            // If it's a new transaction, it has a UUID. If it's an existing one being resaved,
            // it might have a Firestore ID. REPLACE strategy works correctly with unique IDs.
            transactionDao.insertTransaction(transactionEntity)

            // Save to Firestore - Firestore ID will be generated here for new transactions
            // saveTransactionToFirestore returns Result<String> (the Firestore ID)
            val firestoreResult = firestoreService.saveTransactionToFirestore(transaction, firestoreService.getCurrentUserId())

            firestoreResult.onSuccess { firestoreId ->
                Log.d(TAG, "Transaction uploaded to Firestore with ID: $firestoreId")
                // Update local database with the *correct* Firestore ID and SYNCED status
                // Use the entity that was just inserted, but update its ID and status
                val updatedEntity = transactionEntity.copy(id = firestoreId, syncStatus = SyncStatus.SYNCED)
                // Use the ID from Firestore as the primary key in the local DB going forward
                transactionDao.updateTransaction(updatedEntity) // Update based on the new Firestore ID

            }.onFailure { e ->
                // Handle case where Firestore save failed but local save succeeded
                // The local entity already has PENDING_UPLOAD status from the initial insert
                Log.e(TAG, "Failed to upload transaction to Firestore: ${e.message}", e)
                // The local entity remains with PENDING_UPLOAD status to be retried later by syncPendingTransactions
            }

            Log.d(TAG, "Transaction save process initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction", e)
            throw e
        }
    }

    /**
     * Save transaction to local database only (for offline support)
     */
    suspend fun saveTransactionOffline(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving transaction offline: ${transaction.description}")

            // Create entity from model. `fromTransaction` will generate a local UUID
            // since firestoreId will be null for a new offline transaction.
            val transactionEntity = TransactionEntity.fromTransaction(transaction)
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
    suspend fun updateTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating transaction: ${transaction.description}")

            // Create entity from model. The entity's ID will be the existing firestoreId
            val transactionEntity = TransactionEntity.fromTransaction(transaction)
                .copy(syncStatus = SyncStatus.PENDING_UPDATE) // Mark as pending update

            // Update in local database. Uses the existing firestoreId as the primary key.
            transactionDao.updateTransaction(transactionEntity)

            // Update in Firestore. Assumes the transaction model has the firestoreId.
            // updateTransactionInFirestore returns Result<Unit>
            val firestoreResult = firestoreService.updateTransactionInFirestore(transaction)

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
    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting transaction: ${transaction.description}")

            // Create entity from model. The entity's ID will be the existing unique ID (Firestore or local UUID)
            val transactionEntity = TransactionEntity.fromTransaction(transaction)
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
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        // This matches TransactionDao.getTransactionsByDateRange signature
        // The DAO query itself doesn't need sync status filtering here unless you want
        // date-ranged queries to also exclude PENDING_DELETE items.
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
            .map { entities -> entities.map { it.toTransaction() } }
    }

    /**
     * Get transactions by type
     */
    fun getTransactionsByType(type: Transaction.Type): Flow<List<Transaction>> {
        // This matches TransactionDao.getTransactionsByType signature
        // The DAO query itself doesn't need sync status filtering here unless you want
        // type queries to also exclude PENDING_DELETE items.
        return transactionDao.getTransactionsByType(type.name)
            .map { entities -> entities.map { it.toTransaction() } }
    }

    /**
     * Sync pending transactions with Firestore
     */
    suspend fun syncPendingTransactions() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync of pending transactions")

            // Get all pending transactions that are *not* SYNCED
            // Collect the *current* list using first()
            val pendingTransactionsEntities = transactionDao.getPendingSyncTransactions(SyncStatus.SYNCED).first()

            // Process each pending transaction entity
            for (transactionEntity in pendingTransactionsEntities) {
                val transaction = transactionEntity.toTransaction() // Convert to model

                when (transactionEntity.syncStatus) {
                    SyncStatus.PENDING_UPLOAD, SyncStatus.PENDING_CREATE -> {
                        Log.d(TAG, "Sync: Handling PENDING_UPLOAD/CREATE transaction: ${transaction.description}")
                        // Attempt to upload new transactions to Firestore
                        val userId = firestoreService.getCurrentUserId()
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
                        // Ensure firestoreId exists before attempting update
                        transaction.firestoreId?.let { firestoreId ->
                            val firestoreResult = firestoreService.updateTransactionInFirestore(transaction)

                            firestoreResult.onSuccess {
                                Log.d(TAG, "Sync: Updated transaction ${transaction.description}. Firestore ID: $firestoreId")
                                // Mark as synced after successful update
                                transactionDao.updateTransaction(transactionEntity.copy(syncStatus = SyncStatus.SYNCED))
                            }.onFailure { e ->
                                Log.e(TAG, "Sync: Failed to update transaction ${transaction.description}: ${e.message}", e)
                                // Keep status as PENDING_UPDATE if update failed, sync will retry.
                            }
                        } ?: run {
                            Log.e(TAG, "Sync: Cannot update transaction with null firestoreId: ${transaction.description}. Marking as error or SYNCED with warning?")
                            // Decide how to handle updates for entities that somehow lost their firestoreId.
                            // For now, just log and leave as PENDING_UPDATE.
                        }
                    }
                    SyncStatus.PENDING_DELETE -> {
                        Log.d(TAG, "Sync: Deleting transaction: ${transaction.description}")
                        // Ensure firestoreId exists before attempting deletion from Firestore
                        transaction.firestoreId?.let { firestoreId ->
                            val firestoreResult = firestoreService.deleteTransactionFromFirestore(transactionId = firestoreId)

                            firestoreResult.onSuccess {
                                Log.d(TAG, "Sync: Deleted transaction from Firestore: $firestoreId")
                                // Remove from local database only after successful deletion from Firestore
                                transactionDao.deleteTransaction(transactionEntity) // Delete from Room
                            }.onFailure { e ->
                                Log.e(TAG, "Sync: Failed to delete transaction from Firestore: ${e.message}", e)
                                // Keep status as PENDING_DELETE if deletion failed, sync will retry.
                            }
                        } ?: run {
                            // If firestoreId is null, it was never uploaded.
                            // It was already marked PENDING_DELETE, and will be cleaned up here.
                            Log.w(TAG, "Sync: Deleting local-only transaction with null firestoreId: ${transaction.description}")
                            transactionDao.deleteTransaction(transactionEntity) // Delete from Room
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
     * This function should be called periodically to fetch data from Firestore
     * and merge it with the local database. Conflict resolution is handled by OnConflictStrategy.REPLACE
     * using the entity's `id` (which will be the firestoreId for remote transactions).
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
                TransactionEntity.fromTransaction(it).copy(syncStatus = SyncStatus.SYNCED)
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
}