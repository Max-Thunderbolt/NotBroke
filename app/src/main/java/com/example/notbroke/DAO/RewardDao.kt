package com.example.notbroke.DAO

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for reward operations
 */
@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards WHERE userId = :userId ORDER BY type, name")
    fun getAllRewards(userId: String): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards WHERE id = :rewardId")
    fun getRewardById(rewardId: String): Flow<RewardEntity?>

    @Query("SELECT * FROM rewards WHERE userId = :userId AND type = :type ORDER BY name")
    fun getRewardsByType(userId: String, type: String): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards WHERE userId = :userId AND isUnlocked = 1 ORDER BY type, name")
    fun getUnlockedRewards(userId: String): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards WHERE userId = :userId AND claimed = 1 ORDER BY type, name")
    fun getClaimedRewards(userId: String): Flow<List<RewardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: RewardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewards(rewards: List<RewardEntity>)

    @Update
    suspend fun updateReward(reward: RewardEntity)

    @Delete
    suspend fun deleteReward(reward: RewardEntity)

    @Query("DELETE FROM rewards WHERE id = :rewardId")
    suspend fun deleteRewardById(rewardId: String)

    @Query("SELECT * FROM rewards WHERE syncStatus != :status")
    fun getPendingSyncRewards(status: SyncStatus = SyncStatus.SYNCED): Flow<List<RewardEntity>>

    @Query("DELETE FROM rewards")
    suspend fun deleteAllRewards()
} 