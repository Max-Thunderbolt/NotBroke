package com.example.notbroke.DAO

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.notbroke.models.UserPreferences
import com.example.notbroke.models.DebtStrategyType

/**
 * Entity class representing user preferences in the local database
 */
@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey
    val userId: String,
    val selectedDebtStrategy: String, // Stored as string (DebtStrategyType.name)
    val lastUpdated: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    // Convert from UserPreferences model to UserPreferencesEntity
    companion object {
        fun fromUserPreferences(preferences: UserPreferences): UserPreferencesEntity {
            return UserPreferencesEntity(
                userId = preferences.userId,
                selectedDebtStrategy = preferences.selectedDebtStrategy.name,
                lastUpdated = preferences.lastUpdated
            )
        }
    }

    // Convert from UserPreferencesEntity to UserPreferences model
    fun toUserPreferences(): UserPreferences {
        return UserPreferences(
            userId = userId,
            selectedDebtStrategy = try {
                DebtStrategyType.valueOf(selectedDebtStrategy)
            } catch (e: IllegalArgumentException) {
                DebtStrategyType.AVALANCHE
            },
            lastUpdated = lastUpdated
        )
    }
} 