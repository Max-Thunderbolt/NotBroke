package com.example.notbroke.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.notbroke.R
import com.example.notbroke.models.Category
import com.example.notbroke.repositories.RepositoryFactory
import com.example.notbroke.services.AuthService
import com.example.notbroke.utils.CategorizationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.notbroke.DAO.AppDatabase

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addCategoryButton = view.findViewById<Button>(R.id.addCategoryButton)
        val setMonthLimitButton = view.findViewById<Button>(R.id.setMonthLimitButton)

        // Load categories when fragment is created
        lifecycleScope.launch {
            try {
                CategorizationUtils.loadCategoriesFromDatabase(requireContext(), getCurrentUserId())
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        addCategoryButton.setOnClickListener {
            Toast.makeText(context, "Add Category clicked", Toast.LENGTH_SHORT).show()
            showAddCategoryDialog()
        }

        setMonthLimitButton.setOnClickListener {
            Toast.makeText(context, "Monthly limit feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val categoryNameInput = dialogView.findViewById<TextInputEditText>(R.id.categoryNameInput)
        val categoryKeywordInput = dialogView.findViewById<TextInputEditText>(R.id.categoryKeyWordInput)
        val categoryTypeGroup = dialogView.findViewById<RadioGroup>(R.id.categoryTypeGroup)
        val incomeRadioButton = dialogView.findViewById<RadioButton>(R.id.incomeRadioButton)
        val expenseRadioButton = dialogView.findViewById<RadioButton>(R.id.expenseRadioButton)

        dialogView.findViewById<View>(R.id.addCategoryButton).setOnClickListener {
            val categoryName = categoryNameInput.text.toString().trim()
            val keyword = categoryKeywordInput.text.toString().trim()

            if (categoryName.isEmpty()) {
                Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categoryType = if (incomeRadioButton.isChecked) {
                Category.Type.INCOME
            } else {
                Category.Type.EXPENSE
            }

            // Create new category
            val category = Category(
                userId = getCurrentUserId(), // You'll need to implement this
                categoryName = categoryName,
                categoryType = categoryType,
                keyword = keyword.takeIf { it.isNotEmpty() }
            )

            // Save category using repository
            lifecycleScope.launch {
                try {
                    val repository = RepositoryFactory.getInstance(requireContext()).categoryRepository
                    repository.saveCategory(category)

                    // Reload categories from database
                    CategorizationUtils.loadCategoriesFromDatabase(requireContext(), getCurrentUserId())

                    Toast.makeText(context, "Category added successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to add category: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialogView.findViewById<View>(R.id.cancelCategoryButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getCurrentUserId(): String {
        return AuthService.getInstance().getCurrentUserId()
    }


    companion object {
        fun newInstance() = SettingsFragment()
    }
}