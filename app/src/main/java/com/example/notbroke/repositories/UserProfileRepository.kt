package com.example.notbroke.repositories

import android.util.Log
import com.example.notbroke.DAO.UserProfileDao
import com.example.notbroke.DAO.UserProfileEntity
import com.example.notbroke.DAO.SyncStatus
import com.example.notbroke.services.FirestoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for handling user profile data from both local database and Firestore
 */
class UserProfileRepository(
    private val userProfileDao: UserProfileDao,
    private val firestoreService: FirestoreService
) {
    private val TAG = "UserProfileRepository"

    fun getUserProfile(userId: String): Flow<UserProfileEntity?> {
        return userProfileDao.getUserProfile(userId)
    } 

    suspend fun saveUserProfile(userId: String, username: String, email: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving user profile for user: $userId")
            
            val userProfile = UserProfileEntity(
                userId = userId,
                username = username,
                email = email
            )
            
            userProfileDao.insertUserProfile(userProfile)
            
            firestoreService.saveUserProfile(userId, username, email)
            
            Log.d(TAG, "User profile saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile", e)
            throw e
        }
    }
    
    suspend fun saveUserProfileOffline(userId: String, username: String, email: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving user profile offline for user: $userId")
            
            val userProfile = UserProfileEntity(
                userId = userId,
                username = username,
                email = email,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            
            userProfileDao.insertUserProfile(userProfile)
            
            Log.d(TAG, "User profile saved offline successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile offline", e)
            throw e
        }
    }
    
    suspend fun updateUserProfile(userId: String, username: String, email: String) = withContext(Dispatchers.IO) {
    suspend fun updateUserProfile(userId: String, username: String, email: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating user profile for user: $userId")
            
            val existingProfile = userProfileDao.getUserProfile(userId).map { it ?: throw Exception("User profile not found") }
            
            val updatedProfile = UserProfileEntity(
                userId = userId,
                username = username,
                email = email,
                syncStatus = SyncStatus.PENDING_UPDATE
            )
            userProfileDao.updateUserProfile(updatedProfile)
            
            firestoreService.updateUserProfile(userId, username, email)
            
            Log.d(TAG, "User profile updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            throw e
        }
    }
    
    suspend fun deleteUserProfile(userId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting user profile for user: $userId")

            val existingProfile = userProfileDao.getUserProfile(userId).map { it ?: throw Exception("User profile not found") }
            
            val profileToDelete = UserProfileEntity(
                userId = userId,
                username = "",
                email = "",
                syncStatus = SyncStatus.PENDING_DELETE
            )
            userProfileDao.deleteUserProfile(profileToDelete)
            
            firestoreService.deleteUserProfile(userId)
            
            Log.d(TAG, "User profile deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user profile", e)
            throw e
        }
    }
    
    suspend fun syncPendingProfiles() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync of pending user profiles")
            
            val pendingProfiles = userProfileDao.getPendingSyncProfiles(SyncStatus.SYNCED).map { it }
            
            pendingProfiles.collect { profiles ->
                for (profile in profiles) {
                    when (profile.syncStatus) {
                        SyncStatus.PENDING_UPLOAD -> {
                            Log.d(TAG, "Uploading user profile: ${profile.username}")
                            firestoreService.saveUserProfile(profile.userId, profile.username, profile.email)
                        }
                        SyncStatus.PENDING_UPDATE -> {
                            Log.d(TAG, "Updating user profile: ${profile.username}")
                            firestoreService.updateUserProfile(profile.userId, profile.username, profile.email)
                        }
                        SyncStatus.PENDING_DELETE -> {
                            Log.d(TAG, "Deleting user profile: ${profile.username}")
                            firestoreService.deleteUserProfile(profile.userId)
                        }
                        else -> {
                            Log.d(TAG, "User profile already synced: ${profile.username}")
                            continue
                        }
                    }
                    
                    userProfileDao.updateUserProfile(profile.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
            
            Log.d(TAG, "Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending user profiles", e)
            throw e
        }
    }

    suspend fun loadUserProfileFromFirestore(userId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading user profile from Firestore")
            
            val userProfile = firestoreService.getUserProfile(userId)
            
            val entity = UserProfileEntity(
                userId = userId,
                username = userProfile.username,
                email = userProfile.email
            )
            userProfileDao.insertUserProfile(entity)
            
            Log.d(TAG, "Loaded user profile from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user profile from Firestore", e)
            throw e
        }
    }
} 