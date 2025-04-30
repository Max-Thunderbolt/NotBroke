package com.example.notbroke.DAO

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.notbroke.models.Transaction
import java.util.UUID

/**
 * Entity class representing a transaction in the local database
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String, // This will store either the Firestore ID or a local UUID
    val userId: String, // User ID to associate with this transaction
    val type: String,
    val amount: Double,
    val description: String,
    val category: String,
    val date: Long, // Timestamp in milliseconds
    val receiptImageUri: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED // Default to SYNCED, overridden as needed
) {
    // Convert from Transaction model to TransactionEntity
    companion object {
        fun fromTransaction(transaction: Transaction, userId: String = ""): TransactionEntity {
            // Use the existing firestoreId or generate a new UUID
            val entityId = transaction.firestoreId ?: UUID.randomUUID().toString()

            return TransactionEntity(
                id = entityId,
                userId = userId,
                type = transaction.type.name,
                amount = transaction.amount,
                description = transaction.description,
                category = transaction.category,
                date = transaction.date,
                receiptImageUri = transaction.receiptImageUri,
                // Set appropriate sync status based on whether it's a new or existing transaction
                syncStatus = when {
                    transaction.firestoreId == null -> SyncStatus.PENDING_CREATE
                    else -> SyncStatus.SYNCED // Default for existing transactions
                }
            )
        }
    }

    // Convert from TransactionEntity to Transaction model
    fun toTransaction(): Transaction {
        return Transaction(
            id = 0L, // Local ID is not used in the model
            firestoreId = this.id, // Use the Entity's String ID as the model's firestoreId
            userId = this.userId, // Pass the userId to the model
            type = Transaction.Type.valueOf(this.type),
            amount = this.amount,
            description = this.description,
            category = this.category,
            date = this.date,
            receiptImageUri = this.receiptImageUri
        )
    }
}