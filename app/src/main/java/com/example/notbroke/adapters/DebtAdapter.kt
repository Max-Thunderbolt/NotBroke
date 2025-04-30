package com.example.notbroke.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.models.Debt
import java.text.NumberFormat
import java.util.Locale

class DebtAdapter(
    private val onDeleteClick: (Debt) -> Unit,
    private val onDebtClick: (Debt) -> Unit
) : ListAdapter<Debt, DebtAdapter.DebtViewHolder>(DebtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_debt, parent, false)
        return DebtViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DebtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.debtNameTextView)
        private val amountTextView: TextView = itemView.findViewById(R.id.debtAmountTextView)
        private val progressTextView: TextView = itemView.findViewById(R.id.debtProgressTextView)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.debtItemProgressBar)
        private val interestRateTextView: TextView = itemView.findViewById(R.id.interestRateTextView)
        private val monthlyPaymentTextView: TextView = itemView.findViewById(R.id.monthlyPaymentItemTextView)
        private val timeRemainingTextView: TextView = itemView.findViewById(R.id.timeRemainingTextView)
        private val deleteButton: View = itemView.findViewById(R.id.deleteDebtButton)

        private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        fun bind(debt: Debt) {
            nameTextView.text = debt.name
            amountTextView.text = formatCurrency(debt.getRemainingBalance())
            
            val progressText = "${formatCurrency(debt.amountPaid)} / ${formatCurrency(debt.totalAmount)}"
            progressTextView.text = progressText
            
            progressBar.progress = debt.getProgressPercentage()
            
            interestRateTextView.text = "${debt.interestRate}%"
            monthlyPaymentTextView.text = formatCurrency(debt.monthlyPayment)
            
            val monthsRemaining = debt.getMonthsRemaining()
            timeRemainingTextView.text = when {
                monthsRemaining == 0 -> "Paid off"
                monthsRemaining == 1 -> "1 month"
                monthsRemaining < 12 -> "$monthsRemaining months"
                monthsRemaining % 12 == 0 -> "${monthsRemaining / 12} years"
                else -> "${monthsRemaining / 12}y ${monthsRemaining % 12}m"
            }

            // Set click listeners
            deleteButton.setOnClickListener { onDeleteClick(debt) }
            itemView.setOnClickListener { onDebtClick(debt) }
        }

        private fun formatCurrency(amount: Double): String {
            return currencyFormatter.format(amount)
        }
    }

    private class DebtDiffCallback : DiffUtil.ItemCallback<Debt>() {
        override fun areItemsTheSame(oldItem: Debt, newItem: Debt): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Debt, newItem: Debt): Boolean {
            return oldItem == newItem
        }
    }
} 