package com.example.notbroke.repositories

import android.util.Log // Import Log
import com.example.notbroke.DAO.DebtDao
import com.example.notbroke.DAO.DebtEntity
import com.example.notbroke.models.Debt
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException // Import FirebaseFirestoreException
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
    private val TAG = "DebtRepositoryImpl" // Add TAG for logging
    private val debtsCollection = firestore.collection("debts")

    override suspend fun addDebt(debt: Debt): Result<Debt> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to add debt to Firestore: ${debt.id}")
            val documentRef = debtsCollection.document(debt.id)
            Log.d(TAG, "Firestore document reference created for ID: ${debt.id}")
            documentRef.set(debt).await() // This is the line we suspect might be causing issues
            Log.d(TAG, "Successfully added debt to Firestore: ${debt.id}")

            Log.d(TAG, "Attempting to insert debt into Room: ${debt.id}")
            val debtEntity = DebtEntity.fromDebt(debt)
            debtDao.insertDebt(debtEntity)
            Log.d(TAG, "Successfully inserted debt into Room: ${debt.id}")

            Result.success(debt)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore Error adding debt: ${e.message}", e) // Log specific Firestore error
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Generic Error adding debt: ${e.message}", e) // Log other errors
            Result.failure(e)
        }
    }

    override suspend fun updateDebt(debt: Debt): Result<Debt> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to update debt in Firestore: ${debt.id}")
            debtsCollection.document(debt.id).set(debt).await()
            Log.d(TAG, "Successfully updated debt in Firestore: ${debt.id}")

            Log.d(TAG, "Attempting to update debt in Room: ${debt.id}")
            val debtEntity = DebtEntity.fromDebt(debt)
            debtDao.updateDebt(debtEntity)
            Log.d(TAG, "Successfully updated debt in Room: ${debt.id}")

            Result.success(debt)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore Error updating debt: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Generic Error updating debt: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteDebt(debtId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to delete debt from Firestore: $debtId")
            debtsCollection.document(debtId).delete().await()
            Log.d(TAG, "Successfully deleted debt from Firestore: $debtId")

            Log.d(TAG, "Attempting to delete debt from Room: $debtId")
            debtDao.deleteDebtById(debtId)
            Log.d(TAG, "Successfully deleted debt from Room: $debtId")

            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore Error deleting debt: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Generic Error deleting debt: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getDebt(debtId: String): Result<Debt> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to get debt from Room: $debtId")
            val localDebt = debtDao.getDebtById(debtId).map { entity ->
                entity?.toDebt()
            }

            Log.d(TAG, "Attempting to get debt from Firestore: $debtId")
            val document = debtsCollection.document(debtId).get().await()
            val firestoreDebt = document.toObject(Debt::class.java)

            if (firestoreDebt != null) {
                Log.d(TAG, "Successfully retrieved debt from Firestore: ${firestoreDebt.id}. Inserting/Updating in Room.")
                val debtEntity = DebtEntity.fromDebt(firestoreDebt)
                debtDao.insertDebt(debtEntity) // Use insert for upsert behavior
                Result.success(firestoreDebt)
            } else {
                Log.d(TAG, "Debt not found in Firestore: $debtId. Checking local.")
                val local = localDebt.first() // Get the current value from the flow
                if (local != null) {
                    Log.d(TAG, "Debt found in Room: $debtId")
                    Result.success(local)
                } else {
                    Log.d(TAG, "Debt not found in Room or Firestore: $debtId")
                    Result.failure(NoSuchElementException("Debt not found"))
                }
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore Error getting debt: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Generic Error getting debt: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getAllDebts(userId: String): Flow<List<Debt>> {
        Log.d(TAG, "Getting all debts Flow for user: $userId")
        return debtDao.getAllDebts(userId).map { entities ->
            Log.d(TAG, "Room emitted ${entities.size} debts for user: $userId")
            entities.map { it.toDebt() }
        }
    }

    override suspend fun syncDebts(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting debt sync for user: $userId")
                val snapshot = debtsCollection.whereEqualTo("userId", userId).get().await()
                val firestoreDebts = snapshot.toObjects(Debt::class.java)
                Log.d(TAG, "Fetched ${firestoreDebts.size} debts from Firestore for sync.")

                val debtEntities = firestoreDebts.map { DebtEntity.fromDebt(it) }

                Log.d(TAG, "Inserting/Updating ${debtEntities.size} debts into Room during sync.")
                debtDao.insertDebts(debtEntities)
                Log.d(TAG, "Finished inserting/updating debts into Room during sync.")

                // Get the list of pending debts from the Flow
                Log.d(TAG, "Getting pending sync debts from Room.")
                val pendingDebtsList = debtDao.getPendingSyncDebts().map { entities ->
                    entities.map { it.toDebt() }
                }.first()
                Log.d(TAG, "Found ${pendingDebtsList.size} pending sync debts in Room.")

                // Process each pending debt
                for (debt in pendingDebtsList) {
                    try {
                        Log.d(TAG, "Syncing pending debt to Firestore: ${debt.id}")
                        debtsCollection.document(debt.id).set(debt).await()
                        // Note: You might want to update the syncStatus in Room after successful sync here
                        Log.d(TAG, "Successfully synced pending debt to Firestore: ${debt.id}")
                    } catch (e: FirebaseFirestoreException) {
                        Log.e(TAG, "Firestore Error syncing pending debt ${debt.id} to Firestore: ${e.message}", e)
                        e.printStackTrace()
                    } catch (e: Exception) {
                        Log.e(TAG, "Generic Error syncing pending debt ${debt.id} to Firestore: ${e.message}", e)
                        e.printStackTrace() // Keep printStackTrace for detailed error in logs
                    }
                }
                Log.d(TAG, "Debt sync completed for user: $userId")
            } catch (e: FirebaseFirestoreException) {
                Log.e(TAG, "Firestore Error during debt sync for user $userId: ${e.message}", e)
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "Generic Error during debt sync for user $userId: ${e.message}", e)
                e.printStackTrace() // Keep printStackTrace for detailed error in logs
            }
        }
    }
}
