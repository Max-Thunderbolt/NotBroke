package com.example.notbroke.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.notbroke.repositories.RepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service to handle data synchronization between Room and Firestore
 */
class SyncService : Service() {
    private val TAG = "SyncService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var repositoryFactory: RepositoryFactory

    override fun onCreate() {
        super.onCreate()
        repositoryFactory = RepositoryFactory.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getStringExtra(EXTRA_USER_ID) ?: return START_NOT_STICKY
        
        serviceScope.launch {
            try {
                syncAllData(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing data: ${e.message}", e)
            } finally {
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private suspend fun syncAllData(userId: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting data sync for user: $userId")
        
        try {
            // Sync debts
            repositoryFactory.debtRepository.syncDebts(userId)
            Log.d(TAG, "Debts synced successfully")
            
            // Sync net worth entries
            repositoryFactory.netWorthRepository.syncEntries(userId)
            Log.d(TAG, "Net worth entries synced successfully")
            
            // Sync rewards
            repositoryFactory.rewardRepository.syncRewards(userId)
            Log.d(TAG, "Rewards synced successfully")
            
            // Sync user preferences
            repositoryFactory.userPreferencesRepository.syncPreferences(userId)
            Log.d(TAG, "User preferences synced successfully")
            
            Log.d(TAG, "All data synced successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync: ${e.message}", e)
            throw e
        }
    }

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        
        fun startSync(context: android.content.Context, userId: String) {
            val intent = Intent(context, SyncService::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
            context.startService(intent)
        }
    }
} 