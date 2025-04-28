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
            
            // Save to local database
            val transactionEntity = TransactionEntity.fromTransaction(transaction)
            transactionDao.insertTransaction(transactionEntity)
            
            // Save to Firestore
            val userId = firestoreService.getCurrentUserId()
            firestoreService.saveTransactionToFirestore(transaction, userId)
            
            Log.d(TAG, "Transaction saved successfully")
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
                .copy(syncStatus = SyncStatus.PENDING_UPLOAD)
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
            
            // Update in local database
            val transactionEntity = TransactionEntity.fromTransaction(transaction)
                .copy(syncStatus = SyncStatus.PENDING_UPDATE)
            transactionDao.updateTransaction(transactionEntity)
            
            // Update in Firestore
            firestoreService.updateTransactionInFirestore(transaction)
            
            Log.d(TAG, "Transaction updated successfully")
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
            
            // Delete from local database
            val transactionEntity = TransactionEntity.fromTransaction(transaction)
                .copy(syncStatus = SyncStatus.PENDING_DELETE)
            transactionDao.deleteTransaction(transactionEntity)
            
            // Delete from Firestore
            firestoreService.deleteTransactionFromFirestore(transaction.firestoreId)
            
            Log.d(TAG, "Transaction deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting transaction", e)
            throw e
        }
    }
    
    /**
     * Get transactions by date range
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
            .map { entities -> entities.map { it.toTransaction() } }
    }
    
    /**
     * Get transactions by type
     */
    fun getTransactionsByType(type: Transaction.Type): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type.name)
            .map { entities -> entities.map { it.toTransaction() } }
    }
    
    /**
     * Sync pending transactions with Firestore
     */
    suspend fun syncPendingTransactions() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync of pending transactions")
            
            // Get all pending transactions
            val pendingTransactions = transactionDao.getPendingSyncTransactions(SyncStatus.SYNCED).map { 
                it.map { entity -> entity.toTransaction() } 
            }
            
            // Process each pending transaction
            pendingTransactions.collect { transactions ->
                for (transaction in transactions) {
                    val entity = TransactionEntity.fromTransaction(transaction)
                    
                    when (entity.syncStatus) {
                        SyncStatus.PENDING_UPLOAD -> {
                            Log.d(TAG, "Uploading transaction: ${transaction.description}")
                            val userId = firestoreService.getCurrentUserId()
                            firestoreService.saveTransactionToFirestore(transaction, userId)
                        }
                        SyncStatus.PENDING_UPDATE -> {
                            Log.d(TAG, "Updating transaction: ${transaction.description}")
                            firestoreService.updateTransactionInFirestore(transaction)
                        }
                        SyncStatus.PENDING_DELETE -> {
                            Log.d(TAG, "Deleting transaction: ${transaction.description}")
                            firestoreService.deleteTransactionFromFirestore(transaction.firestoreId)
                        }
                        else -> {
                            Log.d(TAG, "Transaction already synced: ${transaction.description}")
                            continue
                        }
                    }
                    
                    // Mark as synced after successful operation
                    transactionDao.updateTransaction(entity.copy(syncStatus = SyncStatus.SYNCED))
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
            Log.d(TAG, "Loading transactions from Firestore")
            
            // Get transactions from Firestore
            val transactions = firestoreService.getTransactionsForPeriod(userId, startDate, endDate)
            
            // Convert to entities and save to local database
            val entities = transactions.map { TransactionEntity.fromTransaction(it) }
            transactionDao.insertTransactions(entities)
            
            Log.d(TAG, "Loaded ${transactions.size} transactions from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading transactions from Firestore", e)
            throw e
        }
    }
} 