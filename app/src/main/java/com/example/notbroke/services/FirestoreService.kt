package com.example.notbroke.services

import com.example.notbroke.models.Debt
import com.example.notbroke.models.DebtStrategyType
import com.example.notbroke.models.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreService {
    private val db: FirebaseFirestore = Firebase.firestore
    private val debtsCollection = db.collection("debts")
    private val userPreferencesCollection = db.collection("userPreferences")
    
    // Create a new debt
    suspend fun createDebt(debt: Debt): Result<String> = withContext(Dispatchers.IO) {
        try {
            val documentRef = debtsCollection.add(debt).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get all debts for a user
    suspend fun getDebts(userId: String): Result<List<Debt>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = debtsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val debts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Debt::class.java)?.copy(id = doc.id)
            }
            Result.success(debts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Observe debts in real-time
    fun observeDebts(userId: String): Flow<List<Debt>> = callbackFlow {
        val subscription = debtsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val debts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Debt::class.java)?.copy(id = doc.id)
                    }
                    trySend(debts)
                }
            }
            
        awaitClose { subscription.remove() }
    }
    
    // Update an existing debt
    suspend fun updateDebt(debtId: String, debt: Debt): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            debtsCollection.document(debtId)
                .set(debt)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Delete a debt
    suspend fun deleteDebt(debtId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            debtsCollection.document(debtId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Make a payment on a debt
    suspend fun makePayment(debtId: String, paymentAmount: Double): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val debtDoc = debtsCollection.document(debtId).get().await()
            val debt = debtDoc.toObject(Debt::class.java)
                ?: return@withContext Result.failure(Exception("Debt not found"))
            
            val amountApplied = debt.makePayment(paymentAmount)
            
            // Update the debt in Firestore
            debtsCollection.document(debtId)
                .set(debt)
                .await()
                
            Result.success(amountApplied)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get debt statistics
    suspend fun getDebtStatistics(userId: String): Result<DebtStatistics> = withContext(Dispatchers.IO) {
        try {
            val snapshot = debtsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val debts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Debt::class.java)
            }
            
            val statistics = DebtStatistics(
                totalDebt = debts.sumOf { it.totalAmount },
                totalPaid = debts.sumOf { it.amountPaid },
                totalMonthlyPayment = debts.sumOf { it.monthlyPayment },
                debtCount = debts.size
            )
            
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Observe debt statistics in real-time
    fun observeDebtStatistics(userId: String): Flow<DebtStatistics> = callbackFlow {
        val subscription = debtsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val debts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Debt::class.java)
                    }
                    
                    val statistics = DebtStatistics(
                        totalDebt = debts.sumOf { it.totalAmount },
                        totalPaid = debts.sumOf { it.amountPaid },
                        totalMonthlyPayment = debts.sumOf { it.monthlyPayment },
                        debtCount = debts.size
                    )
                    
                    trySend(statistics)
                }
            }
            
        awaitClose { subscription.remove() }
    }
    
    // Get user preferences
    suspend fun getUserPreferences(userId: String): Result<UserPreferences> = withContext(Dispatchers.IO) {
        try {
            val doc = userPreferencesCollection.document(userId).get().await()
            
            if (doc.exists()) {
                try {
                    // Try to deserialize directly first
                    val preferences = doc.toObject(UserPreferences::class.java)
                    if (preferences != null) {
                        return@withContext Result.success(preferences)
                    }
                    
                    // If direct deserialization fails, try manual conversion
                    val data = doc.data
                    if (data != null) {
                        val preferences = UserPreferences(
                            userId = data["userId"] as? String ?: userId,
                            lastUpdated = (data["lastUpdated"] as? Long) ?: System.currentTimeMillis()
                        )
                        
                        // Handle the enum separately
                        val strategyName = data["selectedDebtStrategy"] as? String
                        if (strategyName != null) {
                            preferences.setSelectedDebtStrategyFromString(strategyName)
                        }
                        
                        return@withContext Result.success(preferences)
                    }
                    
                    return@withContext Result.failure(Exception("Failed to parse user preferences"))
                } catch (e: Exception) {
                    // If all deserialization attempts fail, create default preferences
                    val defaultPreferences = UserPreferences(userId)
                    userPreferencesCollection.document(userId)
                        .set(defaultPreferences)
                        .await()
                    return@withContext Result.success(defaultPreferences)
                }
            } else {
                // Create default preferences if they don't exist
                val defaultPreferences = UserPreferences(userId)
                userPreferencesCollection.document(userId)
                    .set(defaultPreferences)
                    .await()
                Result.success(defaultPreferences)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update user preferences
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userPreferencesCollection.document(preferences.userId)
                .set(preferences)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Observe user preferences in real-time
    fun observeUserPreferences(userId: String): Flow<UserPreferences> = callbackFlow {
        val subscription = userPreferencesCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        // Try to deserialize directly first
                        val preferences = snapshot.toObject(UserPreferences::class.java)
                        if (preferences != null) {
                            trySend(preferences)
                            return@addSnapshotListener
                        }
                        
                        // If direct deserialization fails, try manual conversion
                        val data = snapshot.data
                        if (data != null) {
                            val preferences = UserPreferences(
                                userId = data["userId"] as? String ?: userId,
                                lastUpdated = (data["lastUpdated"] as? Long) ?: System.currentTimeMillis()
                            )
                            
                            // Handle the enum separately
                            val strategyName = data["selectedDebtStrategy"] as? String
                            if (strategyName != null) {
                                preferences.setSelectedDebtStrategyFromString(strategyName)
                            }
                            
                            trySend(preferences)
                            return@addSnapshotListener
                        }
                    } catch (e: Exception) {
                        // If deserialization fails, create default preferences
                        val defaultPreferences = UserPreferences(userId)
                        userPreferencesCollection.document(userId)
                            .set(defaultPreferences)
                        trySend(defaultPreferences)
                    }
                } else {
                    // Create default preferences if they don't exist
                    val defaultPreferences = UserPreferences(userId)
                    userPreferencesCollection.document(userId)
                        .set(defaultPreferences)
                    trySend(defaultPreferences)
                }
            }
            
        awaitClose { subscription.remove() }
    }
    
    // Data class for debt statistics
    data class DebtStatistics(
        val totalDebt: Double,
        val totalPaid: Double,
        val totalMonthlyPayment: Double,
        val debtCount: Int
    )
    
    companion object {
        @Volatile
        private var INSTANCE: FirestoreService? = null
        
        fun getInstance(): FirestoreService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreService().also { INSTANCE = it }
            }
        }
    }
} 