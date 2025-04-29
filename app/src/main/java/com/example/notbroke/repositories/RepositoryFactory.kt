package com.example.notbroke.repositories

import android.content.Context
import com.example.notbroke.DAO.AppDatabase
import com.example.notbroke.services.FirestoreService
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Factory class to provide repository instances.
 * Uses lazy initialization for all repositories to ensure they are only created when needed.
 */
class RepositoryFactory private constructor(context: Context) {

    // Private properties
    private val database = AppDatabase.getDatabase(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val firestoreService = FirestoreService.getInstance()

    // Repository instances - all lazy initialized for consistency
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

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao(), firestoreService)
    }

    companion object {
        @Volatile
        private var INSTANCE: RepositoryFactory? = null

        /**
         * Get the singleton instance of RepositoryFactory.
         * Uses double-checked locking pattern for thread safety.
         */
        fun getInstance(context: Context): RepositoryFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RepositoryFactory(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}