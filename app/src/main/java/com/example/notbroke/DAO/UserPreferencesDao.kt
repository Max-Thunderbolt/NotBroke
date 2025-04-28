package com.example.notbroke.DAO

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user preferences operations
 */
@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    fun getUserPreferences(userId: String): Flow<UserPreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreferences(preferences: UserPreferencesEntity)

    @Update
    suspend fun updateUserPreferences(preferences: UserPreferencesEntity)

    @Delete
    suspend fun deleteUserPreferences(preferences: UserPreferencesEntity)

    @Query("SELECT * FROM user_preferences WHERE syncStatus != :status")
    fun getPendingSyncPreferences(status: SyncStatus = SyncStatus.SYNCED): Flow<List<UserPreferencesEntity>>

    @Query("DELETE FROM user_preferences")
    suspend fun deleteAllUserPreferences()
} 