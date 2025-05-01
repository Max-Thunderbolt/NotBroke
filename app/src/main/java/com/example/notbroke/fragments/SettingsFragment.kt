package com.example.notbroke.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.notbroke.R
import com.example.notbroke.models.Category
import com.example.notbroke.repositories.RepositoryFactory
import com.example.notbroke.services.AuthService
import com.example.notbroke.utils.CategorizationUtils
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
            showSetLimitDialog()
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
                    val userId = getCurrentUserId()

                    // Get categories from local Room (assumes they are already synced)
                    val localCategories = repository.getAllCategories(userId).first()

                    // Check if the category already exists (by name and type)
                    val categoryExists = localCategories.any {
                        it.categoryName.equals(categoryName, ignoreCase = true) &&
                                it.categoryType == categoryType
                    }

                    if (categoryExists) {
                        Toast.makeText(context, "Category already exists", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Proceed to create a new category
                    val newCategory = Category(
                        userId = userId,
                        categoryName = categoryName,
                        categoryType = categoryType,
                        keyword = keyword.takeIf { it.isNotEmpty() }
                    )

                    repository.saveCategory(newCategory)

                    // Update categorization utils in-memory cache
                    CategorizationUtils.loadCategoriesFromDatabase(requireContext(), userId)

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

    private fun showSetLimitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_limit, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val categorySpinner = dialogView.findViewById<AppCompatAutoCompleteTextView>(R.id.categorySpinner)
        val limitAmountEditText = dialogView.findViewById<TextInputEditText>(R.id.limitAmountEditText)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelLimitButton)
        val setLimitButton = dialogView.findViewById<Button>(R.id.addLimitButton)

        // Get expense categories for the dropdown
        lifecycleScope.launch {
            try {
                val repository = RepositoryFactory.getInstance(requireContext()).categoryRepository
                val userId = AuthService.getInstance().getCurrentUserId()

                val allCategories = CategorizationUtils.expenseCategories
                val existingCategories = repository.getCategoriesByType(userId, Category.Type.EXPENSE).first()

                // For fast lookup by name
                val existingCategoryNames = existingCategories.map { it.categoryName }.toSet()

                // UI display with current limits
                val categoryItems = allCategories.map { categoryName ->
                    val existingCategory = existingCategories.find { it.categoryName == categoryName }
                    "${categoryName} (Current Limit: R${existingCategory?.monthLimit ?: 0.0})"
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    categoryItems
                )
                categorySpinner.setAdapter(adapter)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        setLimitButton.setOnClickListener {
            val selectedText = categorySpinner.text.toString()
            val limitAmount = limitAmountEditText.text.toString().toDoubleOrNull()

            if (selectedText.isEmpty()) {
                Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (limitAmount == null || limitAmount <= 0) {
                Toast.makeText(context, "Please enter a valid limit amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCategory = selectedText.substringBefore(" (Current Limit:")

            lifecycleScope.launch {
                try {
                    val repository = RepositoryFactory.getInstance(requireContext()).categoryRepository
                    val userId = AuthService.getInstance().getCurrentUserId()

                    val existingCategories = repository.getCategoriesByType(userId, Category.Type.EXPENSE).first()
                    val existingCategoryNames = existingCategories.map { it.categoryName }.toSet()

                    if (selectedCategory in existingCategoryNames) {
                        // Update existing
                        val existingCategory = existingCategories.first { it.categoryName == selectedCategory }
                        //val updatedCategory = existingCategory.copy(monthLimit = limitAmount)
                        val updatedCategory = existingCategory.copy(
                            monthLimit = limitAmount,
                            firestoreId = existingCategory.firestoreId // ensure it's preserved
                        )

                        repository.updateCategory(updatedCategory)
                    } else {
                        // Create new
                        val newCategory = Category(
                            userId = userId,
                            categoryName = selectedCategory,
                            categoryType = Category.Type.EXPENSE,
                            monthLimit = limitAmount
                        )
                        repository.saveCategory(newCategory)
                    }

                    Toast.makeText(context, "Monthly limit updated successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to set limit: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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