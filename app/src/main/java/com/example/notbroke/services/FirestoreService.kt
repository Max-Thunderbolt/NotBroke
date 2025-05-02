package com.example.notbroke.services

import com.example.notbroke.models.Debt
import com.example.notbroke.models.DebtStrategyType
import com.example.notbroke.models.UserPreferences
import com.example.notbroke.models.Transaction
import com.example.notbroke.models.Reward
import com.example.notbroke.models.NetWorthEntry
import com.example.notbroke.models.UserProfile
import com.example.notbroke.models.Category
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.NoSuchElementException

class FirestoreService {
    private val db: FirebaseFirestore = Firebase.firestore
    private val debtsCollection = db.collection("debts")
    private val userPreferencesCollection = db.collection("userPreferences")
    private val transactionsCollection = db.collection("transactions")
    private val rewardsCollection = db.collection("rewards")
    private val netWorthCollection = db.collection("netWorth")
    private val userProfilesCollection = db.collection("userProfiles")
    private val categoriesCollection = db.collection("categories")
    
    // User Profile Methods
    suspend fun saveUserProfile(userId: String, username: String, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userProfile = UserProfile(
                userId = userId,
                username = username,
                email = email
            )
            
            userProfilesCollection.document(userId)
                .set(userProfile)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfile(userId: String, username: String, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userProfile = UserProfile(
                userId = userId,
                username = username,
                email = email
            )
            
            userProfilesCollection.document(userId)
                .set(userProfile)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteUserProfile(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userProfilesCollection.document(userId)
                .delete()
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserProfile(userId: String): UserProfile = withContext(Dispatchers.IO) {
        try {
            val doc = userProfilesCollection.document(userId).get().await()
            
            if (doc.exists()) {
                val userProfile = doc.toObject(UserProfile::class.java)
                if (userProfile != null) {
                    return@withContext userProfile
                }
            }
            
            // Return default profile if not found
            UserProfile(
                userId = userId,
                username = "User",
                email = ""
            )
        } catch (e: Exception) {
            // Return default profile on error
            UserProfile(
                userId = userId,
                username = "User",
                email = ""
            )
        }
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
    
    // Transaction Methods
    suspend fun saveTransactionToFirestore(transaction: Transaction, userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Use the existing firestoreId if available, otherwise create a new document
            val documentRef = if (transaction.firestoreId != null) {
                transactionsCollection.document(transaction.firestoreId)
            } else {
                transactionsCollection.document()
            }
            
            // Add userId to the transaction
            val transactionWithUserId = transaction.copy(firestoreId = documentRef.id)
            
            // Save to Firestore
            documentRef.set(transactionWithUserId).await()
            
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTransactionInFirestore(transaction: Transaction): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val transactionId = transaction.firestoreId ?: return@withContext Result.failure(Exception("Transaction ID is required"))
            transactionsCollection.document(transactionId)
                .set(transaction)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransactionFromFirestore(transactionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionsCollection.document(transactionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeTransactions(userId: String): Flow<List<Transaction>> = callbackFlow {
        val subscription = transactionsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(firestoreId = doc.id)
                    }
                    trySend(transactions)
                }
            }
            
        awaitClose { subscription.remove() }
    }

    suspend fun getTransactionsForPeriod(userId: String, startDate: Long, endDate: Long): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            val snapshot = transactionsCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)?.copy(firestoreId = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTransactionById(transactionId: String): Result<Transaction> = withContext(Dispatchers.IO) {
        try {
            val doc = transactionsCollection.document(transactionId).get().await()
            val transaction = doc.toObject(Transaction::class.java)
            
            if (transaction != null) {
                Result.success(transaction.copy(firestoreId = doc.id))
            } else {
                Result.failure(NoSuchElementException("Transaction not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Reward Methods
    suspend fun saveRewardToFirestore(reward: Reward, userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val documentRef = rewardsCollection.document()
            val rewardWithId = reward.copy(id = documentRef.id.toIntOrNull() ?: 0)
            documentRef.set(rewardWithId).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRewardInFirestore(reward: Reward): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            rewardsCollection.document(reward.id.toString())
                .set(reward)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRewardFromFirestore(rewardId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            rewardsCollection.document(rewardId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeRewards(userId: String): Flow<List<Reward>> = callbackFlow {
        val subscription = rewardsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val rewards = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Reward::class.java)
                    }
                    trySend(rewards)
                }
            }
            
        awaitClose { subscription.remove() }
    }

    suspend fun getRewardById(rewardId: String): Result<Reward> = withContext(Dispatchers.IO) {
        try {
            val doc = rewardsCollection.document(rewardId).get().await()
            val reward = doc.toObject(Reward::class.java)
            
            if (reward != null) {
                Result.success(reward)
            } else {
                Result.failure(NoSuchElementException("Reward not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRewardsByType(userId: String, type: String): Result<List<Reward>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = rewardsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", type)
                .get()
                .await()
            
            val rewards = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Reward::class.java)
            }
            
            Result.success(rewards)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnlockedRewards(userId: String): Result<List<Reward>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = rewardsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isUnlocked", true)
                .get()
                .await()
            
            val rewards = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Reward::class.java)
            }
            
            Result.success(rewards)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClaimedRewards(userId: String): Result<List<Reward>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = rewardsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("claimed", true)
                .get()
                .await()
            
            val rewards = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Reward::class.java)
            }
            
            Result.success(rewards)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper method to get current user ID
    fun getCurrentUserId(): String {
        return Firebase.auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }
    
    // Data class for debt statistics
    data class DebtStatistics(
        val totalDebt: Double,
        val totalPaid: Double,
        val totalMonthlyPayment: Double,
        val debtCount: Int
    )
    
    // Debt Methods
    suspend fun saveDebtToFirestore(debt: Debt): Result<String> = withContext(Dispatchers.IO) {
        try {
            val documentRef = debtsCollection.document(debt.id ?: "")
            documentRef.set(debt).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDebtInFirestore(debt: Debt): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            debtsCollection.document(debt.id ?: "")
                .set(debt)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDebtFromFirestore(debtId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            debtsCollection.document(debtId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDebtById(debtId: String): Result<Debt> = withContext(Dispatchers.IO) {
        try {
            val doc = debtsCollection.document(debtId).get().await()
            val debt = doc.toObject(Debt::class.java)
            
            if (debt != null) {
                Result.success(debt)
            } else {
                Result.failure(NoSuchElementException("Debt not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeDebts(userId: String): Flow<List<Debt>> = callbackFlow {
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
                    trySend(debts)
                }
            }
            
        awaitClose { subscription.remove() }
    }

    // Net Worth Methods
    suspend fun saveNetWorthEntryToFirestore(entry: NetWorthEntry): Result<String> = withContext(Dispatchers.IO) {
        try {
            val documentRef = netWorthCollection.document()
            val entryWithId = entry.copy(id = documentRef.id)
            documentRef.set(entryWithId).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNetWorthEntryInFirestore(entry: NetWorthEntry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            netWorthCollection.document(entry.id)
                .set(entry)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNetWorthEntryFromFirestore(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            netWorthCollection.document(entryId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNetWorthEntryById(entryId: String): Result<NetWorthEntry> = withContext(Dispatchers.IO) {
        try {
            val doc = netWorthCollection.document(entryId).get().await()
            val entry = doc.toObject(NetWorthEntry::class.java)
            
            if (entry != null) {
                Result.success(entry)
            } else {
                Result.failure(NoSuchElementException("NetWorth entry not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeNetWorthEntries(userId: String): Flow<List<NetWorthEntry>> = callbackFlow {
        val subscription = netWorthCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val entries = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NetWorthEntry::class.java)
                    }
                    trySend(entries)
                }
            }
            
        awaitClose { subscription.remove() }
    }

    // Category Methods
    // Get categories collection for a specific user
    private fun getCategoriesCollection(userId: String) =
        db.collection("users").document(userId).collection("categories")

    suspend fun saveCategoryToFirestore(category: Category, userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Use the existing firestoreId if available, otherwise create a new document
            val documentRef = if (category.firestoreId != null) {
                getCategoriesCollection(userId).document(category.firestoreId)
            } else {
                getCategoriesCollection(userId).document()
            }

            // Add userId to the category and set the Firestore ID
            val categoryWithId = category.copy(
                firestoreId = documentRef.id,
                userId = userId
            )

            // Save to Firestore
            documentRef.set(categoryWithId).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategoryInFirestore(category: Category): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val categoryId = category.firestoreId ?: return@withContext Result.failure(Exception("Category ID is required"))
            getCategoriesCollection(category.userId).document(categoryId)
                .set(category)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategoryFromFirestore(categoryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // First get the category to find its userId
            val category = getCategoryById(categoryId).getOrNull()
                ?: return@withContext Result.failure(Exception("Category not found"))

            getCategoriesCollection(category.userId).document(categoryId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategoryById(categoryId: String): Result<Category> = withContext(Dispatchers.IO) {
        try {
            // Search in all users for the category
            val querySnapshot = db.collection("users")
                .get()
                .await()

            for (userDoc in querySnapshot.documents) {
                val categoryDoc = userDoc.reference.collection("categories")
                    .document(categoryId)
                    .get()
                    .await()

                val category = categoryDoc.toObject(Category::class.java)
                if (category != null) {
                    return@withContext Result.success(category.copy(firestoreId = categoryDoc.id))
                }
            }

            Result.failure(NoSuchElementException("Category not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeCategories(userId: String): Flow<List<Category>> = callbackFlow {
        val subscription = getCategoriesCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val categories = querySnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Category::class.java)?.copy(
                            firestoreId = doc.id
                        )
                    }
                    trySend(categories)
                }
            }

        awaitClose { subscription.remove() }
    }
    
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