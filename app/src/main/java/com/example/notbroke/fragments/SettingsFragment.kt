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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.adapters.CategoryGoalsAdapter
import com.example.notbroke.models.Category
import com.example.notbroke.models.CategoryGoalDisplayItem
import com.example.notbroke.repositories.RepositoryFactory
import com.example.notbroke.repositories.TransactionRepository
import com.example.notbroke.services.AuthService
import com.example.notbroke.utils.CategorizationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first // Import first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Implement the listener interface from your adapter
class SettingsFragment : Fragment(), CategoryGoalsAdapter.OnDeleteButtonClickListener {

    private lateinit var addCategoryButton: Button
    private lateinit var setMonthLimitButton: Button
    private lateinit var categoryGoalsRecyclerView: RecyclerView
    private lateinit var categoryGoalsAdapter: CategoryGoalsAdapter
    private lateinit var noCategoriesText: TextView

    private val authService = AuthService.getInstance()
    private val repositoryFactory by lazy { RepositoryFactory.getInstance(requireContext()) }
    private val categoryRepository by lazy { repositoryFactory.categoryRepository }
    private val transactionRepository by lazy { repositoryFactory.transactionRepository }

    // Helper data class for managing categories in the set limit dialog
    private data class DisplayableCategoryForLimit(
        val categoryName: String,
        val currentLimit: Double?,
        val firestoreId: String? // null if it's a hard-coded category not yet in DB
    ) {
        override fun toString(): String {
            return if (currentLimit != null && currentLimit > 0) {
                "$categoryName (Current Limit: R${String.format(Locale("en", "ZA"), "%.2f", currentLimit)})"
            } else {
                "$categoryName (No Limit Set)"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addCategoryButton = view.findViewById(R.id.addCategoryButton)
        setMonthLimitButton = view.findViewById(R.id.setMonthLimitButton)
        categoryGoalsRecyclerView = view.findViewById(R.id.categoryGoalsRecyclerView)
        noCategoriesText = view.findViewById(R.id.noCategoriesText)

        setupCategoryGoalsRecyclerView()
        loadInitialDataAndGoals()

        addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }

        setMonthLimitButton.setOnClickListener {
            showSetLimitDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh goals when the fragment is resumed
        loadCategoryGoalsAndProgress()
    }

    private fun setupCategoryGoalsRecyclerView() {
        categoryGoalsAdapter = CategoryGoalsAdapter()
        categoryGoalsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        categoryGoalsRecyclerView.adapter = categoryGoalsAdapter
        categoryGoalsRecyclerView.isNestedScrollingEnabled = false
        // Set the delete button click listener on the adapter
        categoryGoalsAdapter.setOnDeleteButtonClickListener(this)
    }

    private fun loadInitialDataAndGoals() {
        lifecycleScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId.isEmpty()) {
                    Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                // Ensure categories are loaded and cached before attempting to load goals
                CategorizationUtils.loadCategoriesFromDatabase(requireContext(), userId)
                loadCategoryGoalsAndProgress()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load initial data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCategoryGoalsAndProgress() {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            noCategoriesText.visibility = View.VISIBLE
            noCategoriesText.text = "User not logged in. Cannot load goals."
            categoryGoalsRecyclerView.visibility = View.GONE
            categoryGoalsAdapter.submitList(emptyList())
            return
        }

        lifecycleScope.launch {
            try {
                // Fetch all expense categories from the database
                val dbExpenseCategories = categoryRepository
                    .getCategoriesByType(userId, Category.Type.EXPENSE)
                    .first()

                // Filter for categories that have a monthly limit set
                val categoriesWithLimits = dbExpenseCategories.filter { it.monthLimit != null && it.monthLimit > 0.0 }

                if (categoriesWithLimits.isEmpty()) {
                    noCategoriesText.visibility = View.VISIBLE
                    noCategoriesText.text = "No category limits set yet. Tap 'Set Category Monthly Limits' below."
                    categoryGoalsRecyclerView.visibility = View.GONE
                    categoryGoalsAdapter.submitList(emptyList())
                } else {
                    noCategoriesText.visibility = View.GONE
                    categoryGoalsRecyclerView.visibility = View.VISIBLE

                    val displayItems = mutableListOf<CategoryGoalDisplayItem>()
                    val (currentMonthStart, currentMonthEnd) = getCurrentMonthDateRange()

                    for (category in categoriesWithLimits) {
                        // Fetch actual spend for the category in the current month
                        val currentSpend = transactionRepository.getTotalSpendForCategoryInDateRange(
                            userId,
                            category.categoryName,
                            currentMonthStart.time, // Convert Date to Long (milliseconds)
                            currentMonthEnd.time   // Convert Date to Long (milliseconds)
                        ).first() ?: 0.0 // Use first() to get the value from Flow, default to 0.0 if null

                        val monthlyLimit = category.monthLimit!! // Safe due to filter

                        val progress = if (monthlyLimit > 0) {
                            ((currentSpend / monthlyLimit) * 100).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }

                        displayItems.add(
                            CategoryGoalDisplayItem(
                                categoryName = category.categoryName,
                                currentSpend = currentSpend,
                                monthlyLimit = monthlyLimit,
                                progress = progress,
                                firestoreId = category.firestoreId // Include the firestoreId
                            )
                        )
                    }
                    // Submit the list to the adapter and sort by category name
                    categoryGoalsAdapter.submitList(displayItems.sortedBy { it.categoryName })
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load category goals: ${e.message}", Toast.LENGTH_LONG).show()
                noCategoriesText.visibility = View.VISIBLE
                noCategoriesText.text = "Error loading goals: ${e.message}"
                categoryGoalsRecyclerView.visibility = View.GONE
                categoryGoalsAdapter.submitList(emptyList())
            }
        }
    }

    private fun getCurrentMonthDateRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time
        return Pair(startDate, endDate)
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Add New Category")
            .setView(dialogView)
            .create()

        val categoryNameInput = dialogView.findViewById<TextInputEditText>(R.id.categoryNameInput)
        val categoryKeywordInput = dialogView.findViewById<TextInputEditText>(R.id.categoryKeyWordInput)
        val categoryTypeGroup = dialogView.findViewById<RadioGroup>(R.id.categoryTypeGroup)
        val incomeRadioButton = dialogView.findViewById<RadioButton>(R.id.incomeRadioButton)
        dialogView.findViewById<RadioButton>(R.id.expenseRadioButton)?.isChecked = true

        dialogView.findViewById<Button>(R.id.addCategoryDialogButton).setOnClickListener {
            val categoryName = categoryNameInput.text.toString().trim()
            val keyword = categoryKeywordInput.text.toString().trim()

            if (categoryName.isEmpty()) {
                categoryNameInput.error = "Category name cannot be empty"
                return@setOnClickListener
            }
            categoryNameInput.error = null

            val categoryType = if (incomeRadioButton.isChecked) Category.Type.INCOME else Category.Type.EXPENSE

            lifecycleScope.launch {
                try {
                    val userId = getCurrentUserId()
                    if (userId.isEmpty()) {
                        Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val localCategories = categoryRepository.getAllCategories(userId).first()
                    val categoryExists = localCategories.any {
                        it.categoryName.equals(categoryName, ignoreCase = true) && it.categoryType == categoryType
                    }

                    if (categoryExists) {
                        Toast.makeText(context, "Category '$categoryName' already exists as ${categoryType.name.lowercase()}", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val newCategory = Category(
                        userId = userId,
                        categoryName = categoryName,
                        categoryType = categoryType,
                        keyword = keyword.takeIf { it.isNotEmpty() }
                    )

                    categoryRepository.saveCategory(newCategory)
                    // Reload categories into cache and refresh goals after adding
                    CategorizationUtils.loadCategoriesFromDatabase(requireContext(), userId)
                    loadCategoryGoalsAndProgress()

                    Toast.makeText(context, "Category '$categoryName' added", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()

                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to add category: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        dialogView.findViewById<Button>(R.id.cancelCategoryButton).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showSetLimitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_limit, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Set Monthly Limit")
            .setView(dialogView)
            .create()

        val categorySpinner = dialogView.findViewById<AppCompatAutoCompleteTextView>(R.id.categorySpinner)
        val limitAmountEditText = dialogView.findViewById<TextInputEditText>(R.id.limitAmountEditText)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelLimitButton)
        val setLimitButton = dialogView.findViewById<Button>(R.id.addLimitButton)

        val displayableCategories = mutableListOf<DisplayableCategoryForLimit>()

        lifecycleScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId.isEmpty()) {
                    Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@launch
                }

                // 1. Fetch existing expense categories from DB
                val dbExpenseCategories = categoryRepository.getCategoriesByType(userId, Category.Type.EXPENSE).first()
                val dbCategoryMap = dbExpenseCategories.associateBy { it.categoryName }

                dbExpenseCategories.forEach {
                    displayableCategories.add(
                        DisplayableCategoryForLimit(
                            it.categoryName,
                            it.monthLimit,
                            it.firestoreId
                        )
                    )
                }

                // 2. Get hard-coded expense categories and add if not already present
                val hardcodedCategoryNames = CategorizationUtils.expenseCategories // This now includes custom and rules
                hardcodedCategoryNames.forEach { name ->
                    if (!dbCategoryMap.containsKey(name)) {
                        displayableCategories.add(
                            DisplayableCategoryForLimit(
                                name,
                                null, // No limit set yet, not in DB
                                null  // No Firestore ID yet
                            )
                        )
                    }
                }
                // Sort for consistent display
                displayableCategories.sortBy { it.categoryName }


                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    displayableCategories // ArrayAdapter will use toString() of DisplayableCategoryForLimit
                )
                categorySpinner.setAdapter(adapter)

                if (displayableCategories.isNotEmpty()) {
                    categorySpinner.setText(displayableCategories[0].toString(), false)
                    categorySpinner.isEnabled = true // Enable if there are categories
                    setLimitButton.isEnabled = true // Enable if there are categories
                } else {
                    categorySpinner.setText("No expense categories available", false)
                    categorySpinner.isEnabled = false
                    setLimitButton.isEnabled = false
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load categories: ${e.message}", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        setLimitButton.setOnClickListener {
            val selectedCategoryString = categorySpinner.text.toString()
            val limitAmount = limitAmountEditText.text.toString().toDoubleOrNull()

            // Find the selected DisplayableCategoryForLimit object by comparing their toString() representation
            val selectedDisplayableCategory = displayableCategories.find { it.toString() == selectedCategoryString }

            if (selectedDisplayableCategory == null) {
                categorySpinner.error = "Please select a valid category"
                return@setOnClickListener
            }
            categorySpinner.error = null

            if (limitAmount == null || limitAmount < 0) {
                limitAmountEditText.error = "Enter a valid, non-negative limit (0 to remove)"
                return@setOnClickListener
            }
            limitAmountEditText.error = null

            val selectedCategoryName = selectedDisplayableCategory.categoryName
            val firestoreId = selectedDisplayableCategory.firestoreId // Get firestoreId if available

            lifecycleScope.launch {
                try {
                    val userId = getCurrentUserId()
                    if (userId.isEmpty()) {
                        Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        return@launch
                    }

                    // Attempt to find the category in the database using firestoreId if available, or by name
                    val existingCategoryFromDb = if (firestoreId != null) {
                        categoryRepository.getCategoryById(userId, firestoreId).first()
                    } else {
                        categoryRepository.getCategoriesByType(userId, Category.Type.EXPENSE)
                            .first()
                            .find { it.categoryName == selectedCategoryName }
                    }


                    if (existingCategoryFromDb != null) {
                        // Category exists, update it
                        val updatedCategory = existingCategoryFromDb.copy(
                            monthLimit = if (limitAmount == 0.0) null else limitAmount // Store null if limit is 0
                        )
                        categoryRepository.updateCategory(updatedCategory)
                        Toast.makeText(context, "Limit for '$selectedCategoryName' updated", Toast.LENGTH_SHORT).show()
                    } else {
                        // Category does not exist in DB (it was a hard-coded one without a prior limit or a new custom one not yet limited)
                        // Create and save it
                        val newCategory = Category(
                            userId = userId,
                            categoryName = selectedCategoryName,
                            categoryType = Category.Type.EXPENSE,
                            monthLimit = if (limitAmount == 0.0) null else limitAmount, // Store null if limit is 0
                            keyword = null // Keywords are for auto-categorization, not directly stored with limits this way
                        )
                        categoryRepository.saveCategory(newCategory)
                        // Reload categories into cache after saving a potentially new one
                        CategorizationUtils.loadCategoriesFromDatabase(requireContext(), userId)
                        Toast.makeText(context, "Limit for '$selectedCategoryName' set", Toast.LENGTH_SHORT).show()
                    }

                    // Refresh the list of category goals displayed
                    loadCategoryGoalsAndProgress()
                    dialog.dismiss()

                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to set limit: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        dialog.show()
    }

    // 2. Implement the onDeleteClick method from the adapter's interface
    override fun onDeleteClick(categoryGoalItem: CategoryGoalDisplayItem) {
        // 3. Show a confirmation dialog before deleting
        showDeleteConfirmationDialog(categoryGoalItem)
    }

    // 3. Add the delete confirmation dialog function
    private fun showDeleteConfirmationDialog(categoryGoalItem: CategoryGoalDisplayItem) {
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Delete Category Limit")
            .setMessage("Are you sure you want to delete the monthly limit for '${categoryGoalItem.categoryName}'?")
            .setPositiveButton("Delete") { dialog, which ->
                // 4. Call the function to delete the limit
                deleteCategoryLimit(categoryGoalItem)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    // 4. Add the function to delete the category limit
    private fun deleteCategoryLimit(categoryGoalItem: CategoryGoalDisplayItem) {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Use the firestoreId from the display item to find the category
        val categoryFirestoreId = categoryGoalItem.firestoreId

        if (categoryFirestoreId == null) {
            Toast.makeText(context, "Cannot delete limit for this category (no ID found).", Toast.LENGTH_SHORT).show()
            return
        }


        lifecycleScope.launch {
            try {
                // Fetch the full Category object using its firestoreId
                val categoryToDeleteLimit = categoryRepository.getCategoryById(userId, categoryFirestoreId).first()

                if (categoryToDeleteLimit != null) {
                    // Update the category by setting the monthLimit to null
                    val updatedCategory = categoryToDeleteLimit.copy(monthLimit = null)
                    categoryRepository.updateCategory(updatedCategory) // Save the updated category

                    Toast.makeText(context, "Limit for '${categoryGoalItem.categoryName}' deleted", Toast.LENGTH_SHORT).show()
                    // Refresh the list of goals to reflect the change
                    loadCategoryGoalsAndProgress()
                } else {
                    Toast.makeText(context, "Category not found in database.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete limit: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun getCurrentUserId(): String {
        return authService.getCurrentUserId() ?: ""
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}