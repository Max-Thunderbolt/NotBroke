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

    @Query("SELECT * FROM transactions WHERE syncStatus != :pendingDeleteStatus AND userId = :userId ORDER BY date DESC")
    fun getAllTransactions(pendingDeleteStatus: SyncStatus = SyncStatus.PENDING_DELETE, userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    fun getTransactionById(transactionId: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate AND userId = :userId AND syncStatus != :pendingDeleteStatus ORDER BY date DESC")
    fun getTransactionsByDateRange(
        startDate: Long,
        endDate: Long,
        userId: String,
        pendingDeleteStatus: SyncStatus = SyncStatus.PENDING_DELETE // Added default parameter
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type AND userId = :userId ORDER BY date DESC")
    fun getTransactionsByType(type: String, userId: String): Flow<List<TransactionEntity>>

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

    @Query("SELECT * FROM transactions WHERE syncStatus != :status AND userId = :userId")
    fun getPendingSyncTransactions(status: SyncStatus = SyncStatus.SYNCED, userId: String): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}