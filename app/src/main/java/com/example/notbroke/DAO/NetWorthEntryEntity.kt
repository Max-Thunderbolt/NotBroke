package com.example.notbroke.DAO

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.notbroke.models.NetWorthEntry
import java.util.Date

/**
 * Entity class representing a net worth entry in the local database
 */
@Entity(tableName = "net_worth_entries")
data class NetWorthEntryEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val amount: Double,
    val date: Long, // Stored as timestamp in milliseconds
    val syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    // Convert from NetWorthEntry model to NetWorthEntryEntity
    companion object {
        fun fromNetWorthEntry(
            entry: NetWorthEntry, 
            userId: String, 
            id: String,
            syncStatus: SyncStatus = SyncStatus.SYNCED
        ): NetWorthEntryEntity {
            return NetWorthEntryEntity(
                id = id,
                userId = userId,
                amount = entry.amount,
                date = entry.date.time,
                syncStatus = syncStatus
            )
        }
    }

    // Convert from NetWorthEntryEntity to NetWorthEntry model
    fun toNetWorthEntry(): NetWorthEntry {
        return NetWorthEntry(
            id = id,
            userId = userId,
            amount = amount,
            date = Date(date)
        )
    }
} 