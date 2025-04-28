package com.example.notbroke.DAO

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.notbroke.models.Reward
import com.example.notbroke.models.RewardType

/**
 * Entity class representing a reward in the local database
 */
@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val experiencePoints: Int,
    val iconResId: Int,
    val type: String, // Stored as string (RewardType.name)
    val isUnlocked: Boolean = false,
    val claimed: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    // Convert from Reward model to RewardEntity
    companion object {
        fun fromReward(reward: Reward, userId: String): RewardEntity {
            return RewardEntity(
                id = reward.id.toString(),
                userId = userId,
                name = reward.name,
                description = reward.description,
                experiencePoints = reward.experiencePoints,
                iconResId = reward.iconResId,
                type = reward.type.name,
                isUnlocked = reward.isUnlocked,
                claimed = reward.claimed
            )
        }
    }

    // Convert from RewardEntity to Reward model
    fun toReward(): Reward {
        return Reward(
            id = id.toIntOrNull() ?: 0,
            name = name,
            description = description,
            experiencePoints = experiencePoints,
            iconResId = iconResId,
            type = RewardType.valueOf(type),
            isUnlocked = isUnlocked,
            claimed = claimed
        )
    }
} 