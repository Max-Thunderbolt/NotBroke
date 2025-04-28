package com.example.notbroke.DAO

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for net worth entry operations
 */
@Dao
interface NetWorthEntryDao {

    @Query("SELECT * FROM net_worth_entries WHERE userId = :userId ORDER BY date DESC")
    fun getAllEntries(userId: String): Flow<List<NetWorthEntryEntity>>

    @Query("SELECT * FROM net_worth_entries WHERE id = :entryId")
    fun getEntryById(entryId: String): Flow<NetWorthEntryEntity?>

    @Query("SELECT * FROM net_worth_entries WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getEntriesByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<NetWorthEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: NetWorthEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<NetWorthEntryEntity>)

    @Update
    suspend fun updateEntry(entry: NetWorthEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: NetWorthEntryEntity)

    @Query("DELETE FROM net_worth_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: String)

    @Query("SELECT * FROM net_worth_entries WHERE syncStatus != :status")
    fun getPendingSyncEntries(status: SyncStatus = SyncStatus.SYNCED): Flow<List<NetWorthEntryEntity>>

    @Query("DELETE FROM net_worth_entries")
    suspend fun deleteAllEntries()
} 