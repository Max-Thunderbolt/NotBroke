package com.example.notbroke.DAO

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlin.jvm.Volatile

@Database(
    entities = [
        TransactionEntity::class,
        UserProfileEntity::class,
        DebtEntity::class,
        NetWorthEntryEntity::class,
        RewardEntity::class,
        UserPreferencesEntity::class
    ],
    version = 3, // ✅ already updated
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun debtDao(): DebtDao
    abstract fun netWorthEntryDao(): NetWorthEntryDao
    abstract fun rewardDao(): RewardDao
    abstract fun userPreferencesDao(): UserPreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notbroke_database"
                )
                    .fallbackToDestructiveMigration() // ✅ This line ensures app won't crash on schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
