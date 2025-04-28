package com.example.notbroke.repositories

import android.content.Context
import com.example.notbroke.DAO.AppDatabase
import com.example.notbroke.services.FirestoreService // Import FirestoreService
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Factory class to provide repository instances
 */
class RepositoryFactory private constructor(context: Context) {

    // Keep database private, access it via DAO
    private val database = AppDatabase.getDatabase(context)
    private val firestore = FirebaseFirestore.getInstance()

    // Initialize FirestoreService
    private val firestoreService = FirestoreService.getInstance()


    // Repository instances
    val debtRepository: DebtRepository by lazy {
        DebtRepositoryImpl(firestore, database.debtDao())
    }

    val netWorthRepository: NetWorthRepository by lazy {
        NetWorthRepositoryImpl(firestore, database.netWorthEntryDao())
    }

    val rewardRepository: RewardRepository by lazy {
        RewardRepositoryImpl(firestore, database.rewardDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepositoryImpl(firestore, database.userPreferencesDao())
    }

    // Provide a public method to get the TransactionRepository
    fun getTransactionRepository(): TransactionRepository {
        return TransactionRepository(database.transactionDao(), firestoreService)
    }


    companion object {
        @Volatile
        private var INSTANCE: RepositoryFactory? = null

        fun getInstance(context: Context): RepositoryFactory {
            return INSTANCE ?: synchronized(this) {
                val instance = RepositoryFactory(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}