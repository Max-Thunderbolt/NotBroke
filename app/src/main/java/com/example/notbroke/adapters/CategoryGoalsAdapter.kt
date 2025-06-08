package com.example.notbroke.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button // Import Button or MaterialButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.models.CategoryGoalDisplayItem
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.NumberFormat
import java.util.Locale

class CategoryGoalsAdapter : ListAdapter<CategoryGoalDisplayItem, CategoryGoalsAdapter.ViewHolder>(CategoryGoalDiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA")) // For R currency format

    // 1. Define a listener interface for delete button clicks
    interface OnDeleteButtonClickListener {
        fun onDeleteClick(categoryGoalItem: CategoryGoalDisplayItem)
    }

    // 2. Add a listener property
    private var deleteListener: OnDeleteButtonClickListener? = null

    // Method to set the listener from the Fragment
    fun setOnDeleteButtonClickListener(listener: OnDeleteButtonClickListener) {
        deleteListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_goal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, currencyFormat, deleteListener) // Pass the listener to bind
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryNameTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)
        private val categoryProgressTextView: TextView = itemView.findViewById(R.id.categoryProgressTextView)
        private val categoryProgressBar: LinearProgressIndicator = itemView.findViewById(R.id.categoryProgressBar)
        // 3. Get a reference to the delete button (use Button or MaterialButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteCategoryLimitButton)

        // Update the bind method to accept the listener
        fun bind(item: CategoryGoalDisplayItem, currencyFormat: NumberFormat, listener: OnDeleteButtonClickListener?) {
            categoryNameTextView.text = item.categoryName
            val progressText = "${currencyFormat.format(item.currentSpend)} / ${currencyFormat.format(item.monthlyLimit)}"
            categoryProgressTextView.text = progressText
            categoryProgressBar.progress = item.progress

            // Change progress bar color if over budget
            if (item.currentSpend > item.monthlyLimit && item.monthlyLimit > 0) {
                categoryProgressBar.setIndicatorColor(itemView.context.getColor(R.color.material_red_700))
            } else {
                categoryProgressBar.setIndicatorColor(itemView.context.getColor(R.color.dashboard_yellow_accent))
            }

            // 4. Set an OnClickListener on the delete button
            deleteButton.setOnClickListener {
                listener?.onDeleteClick(item) // Call the listener's method
            }
        }
    }

    class CategoryGoalDiffCallback : DiffUtil.ItemCallback<CategoryGoalDisplayItem>() {
        override fun areItemsTheSame(oldItem: CategoryGoalDisplayItem, newItem: CategoryGoalDisplayItem): Boolean {
            // Assuming categoryName is a unique identifier for goals,
            // if you added firestoreId to CategoryGoalDisplayItem, you might
            // want to use that for more robust identification.
            return oldItem.categoryName == newItem.categoryName
            // If using firestoreId: return oldItem.firestoreId == newItem.firestoreId
        }

        override fun areContentsTheSame(oldItem: CategoryGoalDisplayItem, newItem: CategoryGoalDisplayItem): Boolean {
            // Data class automatically generates equals(), so this compares all properties
            return oldItem == newItem
        }
    }
}