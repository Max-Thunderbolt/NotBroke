package com.example.notbroke.adapters

import android.graphics.Color // Import Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat // Import ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.models.NetWorthEntry
import java.text.NumberFormat // For better currency formatting
import java.text.SimpleDateFormat
import java.util.*

class NetWorthAdapter(
    private val onClick: (NetWorthEntry) -> Unit,
    private val onLongClick: (NetWorthEntry) -> Unit
) : ListAdapter<NetWorthEntry, NetWorthAdapter.NetWorthViewHolder>(NetWorthDiffCallback()) {

    // Using NumberFormat for consistent currency display
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA")) // For R currency format

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetWorthViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_net_worth, parent, false)
        return NetWorthViewHolder(view)
    }

    override fun onBindViewHolder(holder: NetWorthViewHolder, position: Int) {
        holder.bind(getItem(position), currencyFormat)
    }

    inner class NetWorthViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Corrected TextView IDs to match item_net_worth.xml provided previously
        private val nameText: TextView = view.findViewById(R.id.itemNameTextView)
        private val amountText: TextView = view.findViewById(R.id.itemAmountTextView)
        private val dateText: TextView = view.findViewById(R.id.itemDateTextView)

        fun bind(entry: NetWorthEntry, formatter: NumberFormat) {
            nameText.text = entry.name
            dateText.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(entry.date)

            // Format amount and set color
            amountText.text = formatter.format(entry.amount)
            if (entry.amount >= 0) {
                // Positive amount (Asset) - Use a green color
                amountText.setTextColor(ContextCompat.getColor(itemView.context, R.color.positive_green))
            } else {
                // Negative amount (Liability) - Use a red color
                amountText.setTextColor(ContextCompat.getColor(itemView.context, R.color.negative_red))
            }

            itemView.setOnClickListener {
                onClick(entry)
            }

            itemView.setOnLongClickListener {
                onLongClick(entry)
                true
            }
        }
    }

    private class NetWorthDiffCallback : DiffUtil.ItemCallback<NetWorthEntry>() {
        override fun areItemsTheSame(oldItem: NetWorthEntry, newItem: NetWorthEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NetWorthEntry, newItem: NetWorthEntry): Boolean {
            return oldItem == newItem
        }
    }
}