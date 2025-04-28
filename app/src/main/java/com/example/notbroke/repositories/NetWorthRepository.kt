package com.example.notbroke.repositories

import com.example.notbroke.DAO.NetWorthEntryDao
import com.example.notbroke.DAO.NetWorthEntryEntity
import com.example.notbroke.DAO.SyncStatus
import com.example.notbroke.models.NetWorthEntry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

interface NetWorthRepository {
    suspend fun addEntry(entry: NetWorthEntry): Result<NetWorthEntry>
    suspend fun updateEntry(entry: NetWorthEntry): Result<NetWorthEntry>
    suspend fun deleteEntry(entryId: String): Result<Unit>
    suspend fun getEntry(entryId: String): Result<NetWorthEntry>
    fun getAllEntries(userId: String): Flow<List<NetWorthEntry>>
    suspend fun syncEntries(userId: String)
}

class NetWorthRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val netWorthEntryDao: NetWorthEntryDao
) : NetWorthRepository {
    private val netWorthCollection = firestore.collection("netWorth")

    override suspend fun addEntry(entry: NetWorthEntry): Result<NetWorthEntry> = withContext(Dispatchers.IO) {
        try {
            val entryId = UUID.randomUUID().toString()
            val entryWithId = entry.copy(id = entryId)
            
            // Create local entry first with PENDING_CREATE status
            val entryEntity = NetWorthEntryEntity.fromNetWorthEntry(
                entry = entryWithId,
                userId = entry.userId,
                id = entryId,
                syncStatus = SyncStatus.PENDING_CREATE
            )
            netWorthEntryDao.insertEntry(entryEntity)
            
            // Try to sync with Firestore
            try {
                val documentRef = netWorthCollection.document(entryId)
                documentRef.set(entryWithId).await()
                
                // Update sync status to SYNCED
                netWorthEntryDao.updateEntry(entryEntity.copy(syncStatus = SyncStatus.SYNCED))
            } catch (e: Exception) {
                // Keep the local entry with PENDING_CREATE status if Firestore sync fails
                e.printStackTrace()
            }
            
            Result.success(entryWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEntry(entry: NetWorthEntry): Result<NetWorthEntry> = withContext(Dispatchers.IO) {
        try {
            // Update local entry first with PENDING_UPDATE status
            val entryEntity = NetWorthEntryEntity.fromNetWorthEntry(
                entry = entry,
                userId = entry.userId,
                id = entry.id,
                syncStatus = SyncStatus.PENDING_UPDATE
            )
            netWorthEntryDao.updateEntry(entryEntity)
            
            // Try to sync with Firestore
            try {
                netWorthCollection.document(entry.id).set(entry).await()
                
                // Update sync status to SYNCED
                netWorthEntryDao.updateEntry(entryEntity.copy(syncStatus = SyncStatus.SYNCED))
            } catch (e: Exception) {
                // Keep the local entry with PENDING_UPDATE status if Firestore sync fails
                e.printStackTrace()
            }
            
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEntry(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Mark local entry for deletion first
            val currentEntry = netWorthEntryDao.getEntryById(entryId).first()
            if (currentEntry != null) {
                netWorthEntryDao.updateEntry(currentEntry.copy(syncStatus = SyncStatus.PENDING_DELETE))
                
                // Try to delete from Firestore
                try {
                    netWorthCollection.document(entryId).delete().await()
                    netWorthEntryDao.deleteEntryById(entryId)
                } catch (e: Exception) {
                    // Keep the local entry with PENDING_DELETE status if Firestore sync fails
                    e.printStackTrace()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEntry(entryId: String): Result<NetWorthEntry> = withContext(Dispatchers.IO) {
        try {
            // First try to get from local database
            val localEntry = netWorthEntryDao.getEntryById(entryId).first()
            
            if (localEntry != null && localEntry.syncStatus == SyncStatus.SYNCED) {
                return@withContext Result.success(localEntry.toNetWorthEntry())
            }
            
            // If not found locally or not synced, try Firestore
            val document = netWorthCollection.document(entryId).get().await()
            val firestoreEntry = document.toObject(NetWorthEntry::class.java)
            
            if (firestoreEntry != null) {
                val entryEntity = NetWorthEntryEntity.fromNetWorthEntry(
                    entry = firestoreEntry,
                    userId = firestoreEntry.userId,
                    id = entryId,
                    syncStatus = SyncStatus.SYNCED
                )
                netWorthEntryDao.insertEntry(entryEntity)
                Result.success(firestoreEntry)
            } else {
                Result.failure(NoSuchElementException("NetWorth entry not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllEntries(userId: String): Flow<List<NetWorthEntry>> {
        return netWorthEntryDao.getAllEntries(userId).map { entities ->
            entities.map { it.toNetWorthEntry() }
        }
    }

    override suspend fun syncEntries(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Get all entries from Firestore
                val snapshot = netWorthCollection.whereEqualTo("userId", userId).get().await()
                val firestoreEntries = snapshot.toObjects(NetWorthEntry::class.java)
                
                // Update local database with Firestore entries
                val entryEntities = firestoreEntries.map { 
                    NetWorthEntryEntity.fromNetWorthEntry(
                        entry = it,
                        userId = userId,
                        id = it.id,
                        syncStatus = SyncStatus.SYNCED
                    )
                }
                netWorthEntryDao.insertEntries(entryEntities)
                
                // Handle pending local changes
                val pendingEntries = netWorthEntryDao.getPendingSyncEntries().first()
                
                for (entry in pendingEntries) {
                    try {
                        when (entry.syncStatus) {
                            SyncStatus.PENDING_CREATE, SyncStatus.PENDING_UPDATE -> {
                                netWorthCollection.document(entry.id).set(entry.toNetWorthEntry()).await()
                                netWorthEntryDao.updateEntry(entry.copy(syncStatus = SyncStatus.SYNCED))
                            }
                            SyncStatus.PENDING_DELETE -> {
                                netWorthCollection.document(entry.id).delete().await()
                                netWorthEntryDao.deleteEntryById(entry.id)
                            }
                            else -> { /* No action needed */ }
                        }
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