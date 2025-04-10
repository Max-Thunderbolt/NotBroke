package com.example.notbroke.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
    private var transactions: List<Transaction> = emptyList()

    fun setTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    fun getTransactions(): List<Transaction> {
        return transactions
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int = transactions.size

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.transactionTitle)
        private val amountTextView: TextView = itemView.findViewById(R.id.transactionAmount)
        private val dateTextView: TextView = itemView.findViewById(R.id.transactionDate)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.transactionDescription)
        private val receiptIndicator: ImageView = itemView.findViewById(R.id.receiptIndicator)

        fun bind(transaction: Transaction) {
            titleTextView.text = transaction.category
            
            // Format amount based on transaction type (positive for income, negative for expense)
            val formattedAmount = if (transaction.type == Transaction.Type.INCOME) {
                "R%.2f".format(transaction.amount)
            } else {
                "-R%.2f".format(transaction.amount)
            }
            
            amountTextView.text = formattedAmount
            amountTextView.setTextColor(
                if (transaction.type == Transaction.Type.INCOME)
                    itemView.context.getColor(android.R.color.holo_green_light)
                else
                    itemView.context.getColor(android.R.color.holo_red_light)
            )
            
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateTextView.text = sdf.format(Date(transaction.date))
            descriptionTextView.text = transaction.description
            
            // Show receipt indicator if a receipt image is available
            receiptIndicator.visibility = if (transaction.receiptImageUri != null) View.VISIBLE else View.GONE
        }
    }
}
