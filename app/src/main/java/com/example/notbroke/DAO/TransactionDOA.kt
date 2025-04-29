package com.example.notbroke.DAO

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transactions
 */
@Dao
interface TransactionDao {
    // *** FIX for deletion not immediately updating UI ***
    // Filter out transactions marked as PENDING_DELETE
    @Query("SELECT * FROM transactions WHERE syncStatus != :pendingDeleteStatus ORDER BY date DESC")
    fun getAllTransactions(pendingDeleteStatus: SyncStatus = SyncStatus.PENDING_DELETE): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    fun getTransactionById(transactionId: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)

    // Query to get transactions that need syncing (not SYNCED)
    @Query("SELECT * FROM transactions WHERE syncStatus != :status")
    fun getPendingSyncTransactions(status: SyncStatus = SyncStatus.SYNCED): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}