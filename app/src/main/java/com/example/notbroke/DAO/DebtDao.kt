package com.example.notbroke.DAO

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for debt operations
 */
@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY creationDate DESC")
    fun getAllDebts(userId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :debtId")
    fun getDebtById(debtId: String): Flow<DebtEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtEntity>)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :debtId")
    suspend fun deleteDebtById(debtId: String)

    @Query("SELECT * FROM debts WHERE syncStatus != :status")
    fun getPendingSyncDebts(status: SyncStatus = SyncStatus.SYNCED): Flow<List<DebtEntity>>

    @Query("DELETE FROM debts")
    suspend fun deleteAllDebts()
} 