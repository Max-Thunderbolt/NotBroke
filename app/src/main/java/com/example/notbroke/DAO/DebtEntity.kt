package com.example.notbroke.DAO

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.notbroke.models.Debt

/**
 * Entity class representing a debt in the local database
 */
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val totalAmount: Double,
    val amountPaid: Double,
    val interestRate: Double,
    val monthlyPayment: Double,
    val creationDate: Long,
    val lastPaymentDate: Long?,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    // Convert from Debt model to DebtEntity
    companion object {
        fun fromDebt(debt: Debt): DebtEntity {
            return DebtEntity(
                id = debt.id,
                userId = debt.userId,
                name = debt.name,
                totalAmount = debt.totalAmount,
                amountPaid = debt.amountPaid,
                interestRate = debt.interestRate,
                monthlyPayment = debt.monthlyPayment,
                creationDate = debt.creationDate,
                lastPaymentDate = debt.lastPaymentDate
            )
        }
    }

    // Convert from DebtEntity to Debt model
    fun toDebt(): Debt {
        return Debt(
            id = id,
            userId = userId,
            name = name,
            totalAmount = totalAmount,
            amountPaid = amountPaid,
            interestRate = interestRate,
            monthlyPayment = monthlyPayment,
            creationDate = creationDate,
            lastPaymentDate = lastPaymentDate
        )
    }
} 