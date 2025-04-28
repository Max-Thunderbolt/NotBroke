package com.example.notbroke.repositories

import com.example.notbroke.DAO.RewardDao
import com.example.notbroke.DAO.RewardEntity
import com.example.notbroke.models.Reward
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface RewardRepository {
    suspend fun addReward(reward: Reward, userId: String): Result<Reward>
    suspend fun updateReward(reward: Reward, userId: String): Result<Reward>
    suspend fun deleteReward(rewardId: String): Result<Unit>
    suspend fun getReward(rewardId: String): Result<Reward>
    fun getAllRewards(userId: String): Flow<List<Reward>>
    suspend fun syncRewards(userId: String)
}

class RewardRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val rewardDao: RewardDao
) : RewardRepository {
    private val rewardsCollection = firestore.collection("rewards")

    override suspend fun addReward(reward: Reward, userId: String): Result<Reward> = withContext(Dispatchers.IO) {
        try {
            val documentRef = rewardsCollection.document()
            val rewardWithId = reward.copy(id = documentRef.id.toIntOrNull() ?: 0)
            documentRef.set(rewardWithId).await()
            
            val rewardEntity = RewardEntity.fromReward(rewardWithId, userId = userId)
            rewardDao.insertReward(rewardEntity)
            
            Result.success(rewardWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReward(reward: Reward, userId: String): Result<Reward> = withContext(Dispatchers.IO) {
        try {
            rewardsCollection.document(reward.id.toString()).set(reward).await()
            
            val rewardEntity = RewardEntity.fromReward(reward, userId = userId)
            rewardDao.updateReward(rewardEntity)
            
            Result.success(reward)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReward(rewardId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            rewardsCollection.document(rewardId).delete().await()
            
            rewardDao.deleteRewardById(rewardId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReward(rewardId: String): Result<Reward> = withContext(Dispatchers.IO) {
        try {
            // First try to get from local database
            val localReward = rewardDao.getRewardById(rewardId).first()?.toReward()
            if (localReward != null) {
                return@withContext Result.success(localReward)
            }
            
            // If not found locally, try Firestore
            val document = rewardsCollection.document(rewardId).get().await()
            val firestoreReward = document.toObject(Reward::class.java)
            
            if (firestoreReward != null) {
                // Get the userId from the document
                val userId = document.getString("userId") ?: ""
                val rewardEntity = RewardEntity.fromReward(firestoreReward, userId = userId)
                rewardDao.insertReward(rewardEntity)
                Result.success(firestoreReward)
            } else {
                Result.failure(NoSuchElementException("Reward not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllRewards(userId: String): Flow<List<Reward>> {
        return rewardDao.getAllRewards(userId).map { entities ->
            entities.map { it.toReward() }
        }
    }

    override suspend fun syncRewards(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = rewardsCollection.whereEqualTo("userId", userId).get().await()
                val firestoreRewards = snapshot.toObjects(Reward::class.java)
                
                val rewardEntities = firestoreRewards.map { RewardEntity.fromReward(it, userId = userId) }
                
                rewardDao.insertRewards(rewardEntities)
                
                val pendingRewards = rewardDao.getPendingSyncRewards().first()
                
                for (reward in pendingRewards) {
                    try {
                        rewardsCollection.document(reward.id).set(reward.toReward()).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 