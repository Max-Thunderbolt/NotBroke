// RecyclerView Adapter (create NetWorthAdapter.kt)
package com.example.notbroke.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.models.NetWorthEntry
import java.text.SimpleDateFormat
import java.util.*

class NetWorthAdapter : ListAdapter<NetWorthEntry, NetWorthAdapter.NetWorthViewHolder>(NetWorthDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetWorthViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_net_worth, parent, false)
        return NetWorthViewHolder(view)
    }

    override fun onBindViewHolder(holder: NetWorthViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NetWorthViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val amountText: TextView = view.findViewById(R.id.amountText)
        private val dateText: TextView = view.findViewById(R.id.dateText)

        fun bind(entry: NetWorthEntry) {
            amountText.text = "R%.2f".format(entry.amount)
            dateText.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(entry.date)
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
