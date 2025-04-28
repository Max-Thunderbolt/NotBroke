package com.example.notbroke.repositories

import com.example.notbroke.DAO.DebtDao
import com.example.notbroke.DAO.DebtEntity
import com.example.notbroke.models.Debt
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface DebtRepository {
    suspend fun addDebt(debt: Debt): Result<Debt>
    suspend fun updateDebt(debt: Debt): Result<Debt>
    suspend fun deleteDebt(debtId: String): Result<Unit>
    suspend fun getDebt(debtId: String): Result<Debt>
    fun getAllDebts(userId: String): Flow<List<Debt>>
    suspend fun syncDebts(userId: String)
}

class DebtRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val debtDao: DebtDao
) : DebtRepository {
    private val debtsCollection = firestore.collection("debts")

    override suspend fun addDebt(debt: Debt): Result<Debt> = withContext(Dispatchers.IO) {
        try {
            val documentRef = debtsCollection.document(debt.id)
            documentRef.set(debt).await()
            
            val debtEntity = DebtEntity.fromDebt(debt)
            debtDao.insertDebt(debtEntity)
            
            Result.success(debt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDebt(debt: Debt): Result<Debt> = withContext(Dispatchers.IO) {
        try {
            debtsCollection.document(debt.id).set(debt).await()
            
            val debtEntity = DebtEntity.fromDebt(debt)
            debtDao.updateDebt(debtEntity)
            
            Result.success(debt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDebt(debtId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            debtsCollection.document(debtId).delete().await()
            
            debtDao.deleteDebtById(debtId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDebt(debtId: String): Result<Debt> = withContext(Dispatchers.IO) {
        try {
            val localDebt = debtDao.getDebtById(debtId).map { entity -> 
                entity?.toDebt() 
            }
            
            val document = debtsCollection.document(debtId).get().await()
            val firestoreDebt = document.toObject(Debt::class.java)
            
            if (firestoreDebt != null) {
                val debtEntity = DebtEntity.fromDebt(firestoreDebt)
                debtDao.insertDebt(debtEntity)
                Result.success(firestoreDebt)
            } else {
                Result.failure(NoSuchElementException("Debt not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllDebts(userId: String): Flow<List<Debt>> {
        return debtDao.getAllDebts(userId).map { entities ->
            entities.map { it.toDebt() }
        }
    }

    override suspend fun syncDebts(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = debtsCollection.whereEqualTo("userId", userId).get().await()
                val firestoreDebts = snapshot.toObjects(Debt::class.java)
                
                val debtEntities = firestoreDebts.map { DebtEntity.fromDebt(it) }
                
                debtDao.insertDebts(debtEntities)
                
                // Get the list of pending debts from the Flow
                val pendingDebtsList = debtDao.getPendingSyncDebts().map { entities ->
                    entities.map { it.toDebt() }
                }.first()
                
                // Process each pending debt
                for (debt in pendingDebtsList) {
                    try {
                        debtsCollection.document(debt.id).set(debt).await()
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