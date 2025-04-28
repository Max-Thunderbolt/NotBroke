package com.example.notbroke.repositories

import android.util.Log
import com.example.notbroke.DAO.TransactionDao
import com.example.notbroke.DAO.TransactionEntity
// *** IMPORTANT: Ensure this import points to the correct SyncStatus enum for transactions ***
// If you move the enum to TransactionSyncStatus.kt, change this import to:
// import com.example.notbroke.DAO.TransactionSyncStatus
import com.example.notbroke.DAO.SyncStatus // Assuming this now correctly imports the transaction SyncStatus
import com.example.notbroke.models.Transaction
import com.example.notbroke.services.FirestoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first // Import the first() operator
import com.google.firebase.firestore.FirebaseFirestoreException // Import for error checking

/**
 * Repository for handling transaction data from both local database and Firestore
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val firestoreService: FirestoreService
) {
    private val TAG = "TransactionRepository"

    // Get all transactions as Flow
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
        .map { entities -> entities.map { it.toTransaction() } }

    /**
     * Save transaction to both local database and Firestore
     */
    suspend fun saveTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving transaction: ${transaction.description}")

            // Save to local database first with PENDING_UPLOAD status
            // transactionDao.insertTransaction returns Unit, not Long
            val transactionEntity = TransactionEntity.fromTransaction(transaction)
                .copy(syncStatus = SyncStatus.PENDING_UPLOAD) // Use PENDING_UPLOAD from SyncStatus enum
            transactionDao.insertTransaction(transactionEntity) // No return value to capture here

            // Save to Firestore - Firestore ID will be generated here
            // saveTransactionToFirestore returns Result<String>
            val firestoreResult = firestoreService.saveTransactionToFirestore(transaction, firestoreService.getCurrentUserId())

            firestoreResult.onSuccess { firestoreId ->
                Log.d(TAG, "Transaction uploaded to Firestore with ID: $firestoreId")
                // Update local database with Firestore ID and SYNCED status
                // Use the ID from Firestore to create an updated entity and update Room
                val updatedEntity = transactionEntity.copy(id = firestoreId, syncStatus = SyncStatus.SYNCED) // Corrected: Removed duplicate firestoreId parameter
                transactionDao.updateTransaction(updatedEntity) // Update based on the String ID

            }.onFailure { e ->
                // Handle case where Firestore save failed but local save succeeded (status is already PENDING_UPLOAD)
                Log.e(TAG, "Failed to upload transaction to Firestore: ${e.message}", e)
                // The local entity remains with PENDING_UPLOAD status to be retried later
            }

            Log.d(TAG, "Transaction save process initiated") // Log initiated, not necessarily completed Firestore sync
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

            val transactionEntity = TransactionEntity.fromTransaction(transaction)
                .copy(syncStatus = SyncStatus.PENDING_UPLOAD) // Use PENDING_UPLOAD from SyncStatus enum
            transactionDao.insertTransaction(transactionEntity) // Returns Unit

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

            // Update in local database with PENDING_UPDATE status
            val transactionEntity = TransactionEntity.fromTransaction(transaction)
                .copy(syncStatus = SyncStatus.PENDING_UPDATE) // Use PENDING_UPDATE from SyncStatus enum
            transactionDao.updateTransaction(transactionEntity)

            // Update in Firestore
            // updateTransactionInFirestore returns Result<Unit>
            val firestoreResult = firestoreService.updateTransactionInFirestore(transaction)

            firestoreResult.onSuccess {
                Log.d(TAG, "Transaction updated in Firestore: ${transaction.firestoreId}")
                // Status will be SYNCED during the syncPendingTransactions process
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

            // Mark for deletion in local database
            val transactionEntity = TransactionEntity.fromTransaction(transaction)
                .copy(syncStatus = SyncStatus.PENDING_DELETE) // Use PENDING_DELETE from SyncStatus enum
            transactionDao.updateTransaction(transactionEntity) // Mark for deletion

            // Delete from Firestore
            // deleteTransactionFromFirestore returns Result<Unit>
            // Access firestoreId correctly and ensure it's not null for Firestore deletion
            transaction.firestoreId?.let { firestoreId ->
                // *** Correction: Use parameter name 'transactionId' as defined in FirestoreService ***
                val firestoreResult = firestoreService.deleteTransactionFromFirestore(transactionId = firestoreId)

                firestoreResult.onSuccess {
                    Log.d(TAG, "Transaction deleted from Firestore: $firestoreId")
                    // The record will be removed from local DB during syncPendingTransactions after successful Firestore deletion
                }.onFailure { e ->
                    Log.e(TAG, "Failed to delete transaction from Firestore: ${e.message}", e)
                    // Keep status as PENDING_DELETE if deletion failed
                }
            } ?: run {
                Log.w(TAG, "Attempted to delete transaction with null firestoreId from Firestore: ${transaction.description}")
                // If firestoreId is null, it means it was likely a local-only transaction that was never uploaded.
                // In this case, just delete it from the local database.
                transactionDao.deleteTransaction(transactionEntity) // Delete from Room if no Firestore ID
                Log.d(TAG, "Deleted local-only transaction: ${transaction.description}")
            }

            Log.d(TAG, "Transaction deletion process initiated")
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
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
            .map { entities -> entities.map { it.toTransaction() } }
    }

    /**
     * Get transactions by type
     */
    fun getTransactionsByType(type: Transaction.Type): Flow<List<Transaction>> {
        // This matches TransactionDao.getTransactionsByType signature
        return transactionDao.getTransactionsByType(type.name)
            .map { entities -> entities.map { it.toTransaction() } }
    }

    /**
     * Sync pending transactions with Firestore
     */
    suspend fun syncPendingTransactions() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync of pending transactions")

            // Get all pending transactions - collect the *current* list using first()
            // This matches TransactionDao.getPendingSyncTransactions signature
            val pendingTransactionsEntities = transactionDao.getPendingSyncTransactions(SyncStatus.SYNCED).first() // Collect the first list emitted

            // Process each pending transaction entity
            for (transactionEntity in pendingTransactionsEntities) {
                val transaction = transactionEntity.toTransaction()

                when (transactionEntity.syncStatus) {
                    SyncStatus.PENDING_CREATE -> {
                        Log.d(TAG, "Sync: Handling PENDING_CREATE transaction: ${transaction.description}")
                        // Implement the logic to create this transaction in Firestore.
                        // This would likely involve a similar process to PENDING_UPLOAD:
                        val userId = firestoreService.getCurrentUserId()
                        val firestoreResult = firestoreService.saveTransactionToFirestore(transaction, userId)

                        firestoreResult.onSuccess { firestoreId ->
                            Log.d(TAG, "Sync: Created PENDING_CREATE transaction ${transaction.description}. Firestore ID: $firestoreId")
                            // Update local entity with Firestore ID and SYNCED status
                            val updatedEntity = transactionEntity.copy(id = firestoreId, syncStatus = SyncStatus.SYNCED)
                            transactionDao.updateTransaction(updatedEntity)
                        }.onFailure { e ->
                            Log.e(TAG, "Sync: Failed to create PENDING_CREATE transaction ${transaction.description}: ${e.message}", e)
                            // Keep status as PENDING_CREATE (or PENDING_UPLOAD, depending on your retry logic) if failed
                            // The status PENDING_CREATE implies it hasn't been attempted in Firestore yet, so keeping PENDING_CREATE
                            // for a retry mechanism or converting to PENDING_UPLOAD might be appropriate.
                            // For simplicity here, we'll just log and leave it as PENDING_CREATE for now.
                        }
                    }
                    SyncStatus.PENDING_UPLOAD -> {
                        Log.d(TAG, "Sync: Uploading transaction: ${transaction.description}")
                        val userId = firestoreService.getCurrentUserId()
                        // saveTransactionToFirestore returns Result<String>
                        val firestoreResult = firestoreService.saveTransactionToFirestore(transaction, userId)

                        firestoreResult.onSuccess { firestoreId ->
                            Log.d(TAG, "Sync: Uploaded transaction ${transaction.description}. Firestore ID: $firestoreId")
                            // Update local entity with Firestore ID and SYNCED status
                            // Use the ID from Firestore to create an updated entity and update Room
                            val updatedEntity = transactionEntity.copy(id = firestoreId, syncStatus = SyncStatus.SYNCED)
                            transactionDao.updateTransaction(updatedEntity)
                        }.onFailure { e ->
                            Log.e(TAG, "Sync: Failed to upload transaction ${transaction.description}: ${e.message}", e)
                            // Keep status as PENDING_UPLOAD if upload failed
                        }
                    }
                    SyncStatus.PENDING_UPDATE -> {
                        Log.d(TAG, "Sync: Updating transaction: ${transaction.description}")
                        // Access firestoreId correctly and updateTransactionInFirestore returns Result<Unit>
                        transaction.firestoreId?.let { firestoreId -> // Use safe call and let for the nullable firestoreId
                            val firestoreResult = firestoreService.updateTransactionInFirestore(transaction)

                            firestoreResult.onSuccess {
                                Log.d(TAG, "Sync: Updated transaction ${transaction.description}. Firestore ID: $firestoreId")
                                // Mark as synced after successful update
                                transactionDao.updateTransaction(transactionEntity.copy(syncStatus = SyncStatus.SYNCED))
                            }.onFailure { e ->
                                Log.e(TAG, "Sync: Failed to update transaction ${transaction.description}: ${e.message}", e)
                                // Keep status as PENDING_UPDATE if update failed
                            }
                        } ?: Log.e(TAG, "Sync: Cannot update transaction with null firestoreId: ${transaction.description}")
                    }
                    SyncStatus.PENDING_DELETE -> {
                        Log.d(TAG, "Sync: Deleting transaction: ${transaction.description}")
                        // Access firestoreId correctly and deleteTransactionFromFirestore returns Result<Unit>
                        transaction.firestoreId?.let { firestoreId -> // Use safe call and let for the nullable firestoreId
                            val firestoreResult = firestoreService.deleteTransactionFromFirestore(transactionId = firestoreId)

                            firestoreResult.onSuccess {
                                Log.d(TAG, "Sync: Deleted transaction from Firestore: $firestoreId")
                                // Remove from local database after successful deletion from Firestore
                                transactionDao.deleteTransaction(transactionEntity) // Delete from Room
                            }.onFailure { e ->
                                Log.e(TAG, "Sync: Failed to delete transaction from Firestore: ${e.message}", e)
                                // Keep status as PENDING_DELETE if deletion failed
                            }
                        } ?: run {
                            Log.w(TAG, "Sync: Attempted to delete local-only transaction with null firestoreId: ${transaction.description}")
                            // If firestoreId is null, it was never uploaded, just delete from local DB
                            transactionDao.deleteTransaction(transactionEntity) // Delete from Room if no Firestore ID
                            Log.d(TAG, "Sync: Deleted local-only transaction: ${transaction.description}")
                        }
                    }
                    SyncStatus.SYNCED -> {
                        // This case should ideally not be reached due to the query filtering,
                        // but handle it for exhaustiveness.
                        Log.w(TAG, "Sync: Encountered a SYNCED transaction in pending list: ${transaction.description}")
                    }
                }
            }

            Log.d(TAG, "Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending transactions", e)
            // Consider more granular error handling and retry logic
            throw e
        }
    }

    /**
     * Load transactions from Firestore and save to local database
     * This function should be called periodically to fetch data from Firestore
     * and merge it with the local database. Conflict resolution may be needed.
     */
    suspend fun loadTransactionsFromFirestore(userId: String, startDate: Long, endDate: Long) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading transactions from Firestore for period: $startDate to $endDate")

            // Get transactions from Firestore
            // This matches FirestoreService.getTransactionsForPeriod signature
            val firestoreTransactions = firestoreService.getTransactionsForPeriod(userId, startDate, endDate)

            // Convert to entities and save to local database
            val firestoreEntities = firestoreTransactions.map {
                // When loading from Firestore, they are already synced
                // Use the mapping function from TransactionEntity.kt
                TransactionEntity.fromTransaction(it).copy(syncStatus = SyncStatus.SYNCED)
            }

            // Insert fetched transactions.
            // This matches TransactionDao.insertTransactions signature
            // Your DAO uses OnConflictStrategy.REPLACE, which means if an entity with the same String ID exists, it will be replaced.
            // This is important for syncing, but be aware of potential data loss if not handled carefully.
            transactionDao.insertTransactions(firestoreEntities)

            Log.d(TAG, "Loaded and inserted ${firestoreTransactions.size} transactions from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading transactions from Firestore", e)
            throw e
        }
    }
}

// Ensure these mapping functions are defined in TransactionEntity.kt
// You DO NOT need to include these functions here if they are already in TransactionEntity.kt
/*
fun TransactionEntity.toTransaction(): Transaction {
    return Transaction(
        id = this.id.toLongOrNull() ?: 0L, // Convert String ID to Long for the model
        firestoreId = this.id, // Use the String ID from entity as firestoreId in the model
        type = Transaction.Type.valueOf(this.type),
        amount = this.amount,
        description = this.description,
        category = this.category,
        date = this.date,
        receiptImageUri = this.receiptImageUri
    )
}

fun Transaction.toTransactionEntity(): TransactionEntity {
    return TransactionEntity(
        id = this.firestoreId ?: this.id.toString(), // Use firestoreId if available, else local ID as String
        type = this.type.name,
        amount = this.amount,
        description = this.description,
        category = this.category,
        date = this.date,
        receiptImageUri = this.receiptImageUri,
        syncStatus = SyncStatus.SYNCED // Default status, will be overridden when marking pending
    )
}
*/