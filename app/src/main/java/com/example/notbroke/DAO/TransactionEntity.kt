package com.example.notbroke.DAO

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.notbroke.models.Transaction

/**
 * Entity class representing a transaction in the local database
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String, 
    val type: String, 
    val amount: Double,
    val description: String,
    val category: String,
    val date: Long, 
    val receiptImageUri: String? = null, 
    val syncStatus: SyncStatus = SyncStatus.SYNCED 
) {
    // Convert from Transaction model to TransactionEntity
    companion object {
        fun fromTransaction(transaction: Transaction): TransactionEntity {
            return TransactionEntity(
                id = transaction.firestoreId ?: transaction.id.toString(),
                type = transaction.type.name,
                amount = transaction.amount,
                description = transaction.description,
                category = transaction.category,
                date = transaction.date,
                receiptImageUri = transaction.receiptImageUri
            )
        }
    }

    // Convert from TransactionEntity to Transaction model
    fun toTransaction(): Transaction {
        return Transaction(
            id = id.toLongOrNull() ?: 0L,
            firestoreId = id,
            type = Transaction.Type.valueOf(type),
            amount = amount,
            description = description,
            category = category,
            date = date,
            receiptImageUri = receiptImageUri
        )
    }
}

/**
 * Enum representing the sync status of a transaction
 */
enum class SyncStatus {
    SYNCED,           
    PENDING_UPLOAD,   
    PENDING_UPDATE,   
    PENDING_DELETE    
} 