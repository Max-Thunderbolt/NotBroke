package com.example.notbroke.DAO

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a user profile in the local database
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String,
    val username: String,
    val email: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
) 