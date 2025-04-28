package com.example.notbroke.repositories

import android.content.Context
import com.example.notbroke.DAO.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Factory class to provide repository instances
 */
class RepositoryFactory private constructor(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val firestore = FirebaseFirestore.getInstance()

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