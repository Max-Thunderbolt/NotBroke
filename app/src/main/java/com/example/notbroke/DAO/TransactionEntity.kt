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
        fun fromTransaction(transaction: Transaction): TransactionEntity {
            // *** FIX for "saves only last entry" ***
            // Generate a unique local UUID if firestoreId is null (new transaction)
            // Otherwise, use the existing firestoreId as the primary key in Room
            val entityId = transaction.firestoreId ?: UUID.randomUUID().toString()

            return TransactionEntity(
                id = entityId, // Use the generated UUID or Firestore ID as the primary key
                type = transaction.type.name,
                amount = transaction.amount,
                description = transaction.description,
                category = transaction.category,
                date = transaction.date,
                receiptImageUri = transaction.receiptImageUri,
                // syncStatus will be set by the repository when saving/updating/deleting
                syncStatus = SyncStatus.SYNCED // Default when creating entity, will be overridden
            )
        }
    }

    // Convert from TransactionEntity to Transaction model
    fun toTransaction(): Transaction {
        return Transaction(
            // The Transaction model has a Long ID and a String firestoreId.
            // The Entity's String ID holds the unique key (Firestore ID or local UUID).
            // We should populate the model's firestoreId from the entity's id.
            // The model's Long ID might not be necessary or could be handled separately if needed elsewhere.
            // For now, set the Long ID to 0L as per the model's default and use the entity's ID for firestoreId.
            id = 0L, // Keeping the default Long ID from the model
            firestoreId = this.id, // Use the Entity's String ID as the model's firestoreId

            type = Transaction.Type.valueOf(this.type),
            amount = this.amount,
            description = this.description,
            category = this.category,
            date = this.date,
            receiptImageUri = this.receiptImageUri
            // Do NOT map syncStatus here as the Transaction model doesn't have this field.
        )
    }
}