package com.example.notbroke.repositories

import com.example.notbroke.DAO.UserPreferencesDao
import com.example.notbroke.DAO.UserPreferencesEntity
import com.example.notbroke.models.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface UserPreferencesRepository {
    suspend fun savePreferences(preferences: UserPreferences): Result<UserPreferences>
    suspend fun getPreferences(userId: String): Result<UserPreferences>
    fun observePreferences(userId: String): Flow<UserPreferences?>
    suspend fun syncPreferences(userId: String)
}

class UserPreferencesRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userPreferencesDao: UserPreferencesDao
) : UserPreferencesRepository {
    private val preferencesCollection = firestore.collection("userPreferences")

    override suspend fun savePreferences(preferences: UserPreferences): Result<UserPreferences> = withContext(Dispatchers.IO) {
        try {
            preferencesCollection.document(preferences.userId).set(preferences).await()
            
            val preferencesEntity = UserPreferencesEntity.fromUserPreferences(preferences)
            userPreferencesDao.insertUserPreferences(preferencesEntity)
            
            Result.success(preferences)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPreferences(userId: String): Result<UserPreferences> = withContext(Dispatchers.IO) {
        try {
            val localPreferences = userPreferencesDao.getUserPreferences(userId).map { it?.toUserPreferences() }
            
            val document = preferencesCollection.document(userId).get().await()
            val firestorePreferences = document.toObject(UserPreferences::class.java)
            
            if (firestorePreferences != null) {
                val preferencesEntity = UserPreferencesEntity.fromUserPreferences(firestorePreferences)
                userPreferencesDao.insertUserPreferences(preferencesEntity)
                Result.success(firestorePreferences)
            } else {
                Result.failure(NoSuchElementException("User preferences not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observePreferences(userId: String): Flow<UserPreferences?> {
        return userPreferencesDao.getUserPreferences(userId).map { entity ->
            entity?.toUserPreferences()
        }
    }

    override suspend fun syncPreferences(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val document = preferencesCollection.document(userId).get().await()
                val firestorePreferences = document.toObject(UserPreferences::class.java)
                
                if (firestorePreferences != null) {
                    val preferencesEntity = UserPreferencesEntity.fromUserPreferences(firestorePreferences)
                    userPreferencesDao.insertUserPreferences(preferencesEntity)
                }
                
                val pendingPreferences = userPreferencesDao.getPendingSyncPreferences().map { it.toUserPreferences() }
                
                for (preferences in pendingPreferences) {
                    try {
                        preferencesCollection.document(preferences.userId).set(preferences).await()
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