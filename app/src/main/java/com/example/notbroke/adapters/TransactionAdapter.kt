package com.example.notbroke.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    interface OnItemClickListener {
        fun onItemClick(transaction: Transaction)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)

        holder.itemView.setOnClickListener {
            listener?.onItemClick(transaction)
        }
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.transactionTitle)
        private val amountTextView: TextView = itemView.findViewById(R.id.transactionAmount)
        private val dateTextView: TextView = itemView.findViewById(R.id.transactionDate)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.transactionDescription)
        private val receiptIndicator: ImageView = itemView.findViewById(R.id.receiptIndicator)

        fun bind(transaction: Transaction) {
            titleTextView.text = transaction.category

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

            val sdf = SimpleDateFormat("MMM dd,yyyy", Locale.getDefault())
            dateTextView.text = sdf.format(Date(transaction.date))
            descriptionTextView.text = transaction.description

            receiptIndicator.visibility = if (transaction.receiptImageUri != null) View.VISIBLE else View.GONE
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            // Assuming id is the local primary key and is stable
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            // Data class automatically generates equals(), so this compares all properties
            return oldItem == newItem
        }
    }
}