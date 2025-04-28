package com.example.notbroke.repositories

import com.example.notbroke.DAO.NetWorthEntryDao
import com.example.notbroke.DAO.NetWorthEntryEntity
import com.example.notbroke.models.NetWorthEntry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
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
            
            val documentRef = netWorthCollection.document(entryId)
            val entryWithId = entry.copy(id = entryId)
            documentRef.set(entryWithId).await()
            
            val entryEntity = NetWorthEntryEntity.fromNetWorthEntry(entry, userId = "", id = entryId)
            netWorthEntryDao.insertEntry(entryEntity)
            
            Result.success(entryWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEntry(entry: NetWorthEntry): Result<NetWorthEntry> = withContext(Dispatchers.IO) {
        try {
            netWorthCollection.document(entry.id).set(entry).await()
            
            val entryEntity = NetWorthEntryEntity.fromNetWorthEntry(entry, userId = "", id = entry.id)
            netWorthEntryDao.updateEntry(entryEntity)
            
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEntry(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            netWorthCollection.document(entryId).delete().await()
            
            netWorthEntryDao.deleteEntryById(entryId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEntry(entryId: String): Result<NetWorthEntry> = withContext(Dispatchers.IO) {
        try {
            val localEntry = netWorthEntryDao.getEntryById(entryId).map { it?.toNetWorthEntry() }
            
            val document = netWorthCollection.document(entryId).get().await()
            val firestoreEntry = document.toObject(NetWorthEntry::class.java)
            
            if (firestoreEntry != null) {
                val entryEntity = NetWorthEntryEntity.fromNetWorthEntry(firestoreEntry, userId = "", id = entryId)
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
                val snapshot = netWorthCollection.whereEqualTo("userId", userId).get().await()
                val firestoreEntries = snapshot.toObjects(NetWorthEntry::class.java)
                
                val entryEntities = firestoreEntries.map { 
                    NetWorthEntryEntity.fromNetWorthEntry(it, userId = userId, id = it.id) 
                }
                
                netWorthEntryDao.insertEntries(entryEntities)
                
                val pendingEntries = netWorthEntryDao.getPendingSyncEntries().map { it.toNetWorthEntry() }
                
                for (entry in pendingEntries) {
                    try {
                        netWorthCollection.document(entry.id).set(entry).await()
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