package com.example.notbroke.DAO

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user profile operations
 */
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getUserProfile(userId: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfileEntity)

    @Update
    suspend fun updateUserProfile(userProfile: UserProfileEntity)

    @Delete
    suspend fun deleteUserProfile(userProfile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE syncStatus != :excludeStatus")
    fun getPendingSyncProfiles(excludeStatus: SyncStatus): Flow<List<UserProfileEntity>>

    @Query("DELETE FROM user_profiles")
    suspend fun deleteAllUserProfiles()
} 