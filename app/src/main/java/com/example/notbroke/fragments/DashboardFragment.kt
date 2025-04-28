package com.example.notbroke.fragments

// ===== Keep existing imports =====
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.adapters.TransactionAdapter // Import the adapter
import com.example.notbroke.models.Transaction // *** Ensure this import is correct and points to your models package ***
import android.graphics.Color
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.Entry // Import Entry
import com.github.mikephil.charting.data.PieEntry
import android.content.res.ColorStateList
import com.example.notbroke.R
import android.widget.Button // Still needed for dialog buttons
import android.app.Dialog
import com.google.android.material.button.MaterialButton
import android.Manifest // Keep for permissions
import android.app.Activity // Keep for ActivityResult
import android.content.Intent // Keep for Intents
import android.content.pm.PackageManager // Keep for permissions
import android.graphics.BitmapFactory // Keep for images
import android.net.Uri // Keep for images
import android.os.Environment // Keep for images
import android.provider.MediaStore // Keep for images
import androidx.activity.result.ActivityResultLauncher // Keep for ActivityResult
import androidx.activity.result.contract.ActivityResultContracts // Keep for ActivityResult
import androidx.core.content.ContextCompat // Keep for permissions/colors
import androidx.core.content.FileProvider // Keep for camera
import java.io.File // Keep for camera
import java.io.IOException // Keep for camera
import java.text.SimpleDateFormat // Keep for camera filename
import java.util.Date // Keep for camera filename
import java.util.Locale // Keep for camera filename
import android.widget.ImageView // Keep for image preview
import android.widget.AdapterView // Add for Spinner listener
import android.widget.EditText // Import EditText directly
import android.widget.AutoCompleteTextView // **ADDED BACK for edit dialog**
import androidx.lifecycle.lifecycleScope // Add for Coroutines
import com.example.notbroke.utils.CategorizationUtils // Add the new utils class
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.PercentFormatter // Add for chart formatting
import com.github.mikephil.charting.highlight.Highlight // Add for chart interactivity
import com.github.mikephil.charting.listener.OnChartValueSelectedListener // Add for chart interactivity
import com.google.firebase.auth.FirebaseAuth // Add for Firebase Auth
import com.google.firebase.firestore.FirebaseFirestore // Add for Firestore
import com.google.firebase.firestore.QuerySnapshot // Add for Firestore results
import kotlinx.coroutines.launch // Add for Coroutines
import kotlinx.coroutines.tasks.await // Add for Coroutines + Tasks API
import java.util.* // Add for Calendar
import com.example.notbroke.repositories.RepositoryFactory
import com.example.notbroke.repositories.TransactionRepository
import com.example.notbroke.services.FirestoreService
import com.example.notbroke.services.AuthService
import kotlinx.coroutines.flow.collectLatest // Add this import for collectLatest


// *** MODIFIED: Implement TransactionAdapter.OnItemClickListener ***
class DashboardFragment : Fragment(), TransactionAdapter.OnItemClickListener {
    // Use companion object TAG for consistency
    private val TAG = "DashboardFragment" // Use const val for TAG

    // ===== Keep existing Views =====
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var pieChart: PieChart
    private lateinit var periodSpinner: Spinner
    private lateinit var totalBudgetTextView: TextView
    private lateinit var totalSpentTextView: TextView
    private lateinit var remainingTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var balanceIncomeButton: MaterialButton
    private lateinit var balanceExpenseButton: MaterialButton
    private lateinit var addCategoryButton: MaterialButton

    // ===== Keep existing Receipt image handling =====
    private var currentPhotoPath: String? = null
    private var selectedImageUri: Uri? = null
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var currentDialog: Dialog? = null

    // ===== Replace Firebase instances with Repository =====
    private lateinit var repositoryFactory: RepositoryFactory
    // Get the repository instance from the factory
    private val transactionRepository by lazy {
        repositoryFactory.getTransactionRepository() // Corrected access
    }
    private val authService = AuthService.getInstance()

    // Store the current date range for navigation
    private var currentStartDate: Long = 0L
    private var currentEndDate: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeActivityResultLaunchers()
        // Initialize Repository Factory
        repositoryFactory = RepositoryFactory.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Inflating dashboard fragment layout")

        return try {
            inflater.inflate(R.layout.fragment_dashboard, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error inflating layout R.layout.fragment_dashboard", e)

            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up dashboard fragment views and logic")

        try {
            initializeViews(view)
            setupButtonListeners()
            setupTransactionsRecyclerView()
            transactionAdapter.setOnItemClickListener(this)


            setupPieChart()
            setupChartListener()
            setupPeriodSpinner()

            // Replace loadTransactions() call with observeTransactions()
            observeTransactions()

            Log.d(TAG, "onViewCreated: Setup complete.")

        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error initializing views", e)
            showToast("Error: Could not load dashboard components.")
            // Optionally show a simplified error state in the UI
        } catch (e: Exception) {
            Log.e(TAG, "Error during onViewCreated setup", e)
            showToast("Error setting up dashboard: ${e.message}")
        }
    }

    private fun initializeViews(view: View) {
        try {
            transactionsRecyclerView = view.findViewById(R.id.transactionsRecyclerView)
            pieChart = view.findViewById(R.id.pieChart)
            periodSpinner = view.findViewById(R.id.periodSpinner)
            totalBudgetTextView = view.findViewById(R.id.totalBudgetTextView) // Note: Budget value isn't loaded from Firestore yet
            totalSpentTextView = view.findViewById(R.id.totalSpentTextView)
            remainingTextView = view.findViewById(R.id.remainingTextView) // Note: Remaining value isn't calculated yet
            balanceTextView = view.findViewById(R.id.balanceTextView)
            balanceIncomeButton = view.findViewById(R.id.balanceIncomeButton)
            balanceExpenseButton = view.findViewById(R.id.balanceExpenseButton)
            addCategoryButton = view.findViewById(R.id.addCategoryButton)
            Log.d(TAG, "initializeViews: Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            // Throw specific error to be caught in onViewCreated
            throw IllegalStateException("Could not initialize essential views in DashboardFragment", e)
        }
    }

    private fun setupTransactionsRecyclerView() {
        Log.d(TAG, "setupTransactionsRecyclerView: Setting up...")
        transactionAdapter = TransactionAdapter() // Initialize adapter
        transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            // Add item decoration for spacing if needed
            // addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
        Log.d(TAG, "setupTransactionsRecyclerView: Setup complete.")
    }


    private fun updateBalance(transactions: List<Transaction>) {
        var balance = 0.0
        transactions.forEach { transaction ->
            // Use safe access in case transaction object is malformed (though mapping should prevent this)
            if (transaction.type == Transaction.Type.INCOME) {
                balance += transaction.amount
            } else if (transaction.type == Transaction.Type.EXPENSE) {
                balance -= transaction.amount
            }
        }
        // Use requireContext() safely with context check
        context?.let {
            balanceTextView.text = String.format(Locale.getDefault(), "R %.2f", balance)
            Log.d(TAG, "updateBalance: Balance updated to R ${"%.2f".format(balance)}")
        } ?: Log.w(TAG, "updateBalance: Context is null, cannot format currency.")
    }

    private fun setupPieChart() {
        Log.d(TAG, "setupPieChart: Configuring pie chart.")
        pieChart.apply {
            description.isEnabled = false
            // setUsePercentValues(true) // Set this in updatePieChart when data exists

            // <<< CHANGE: Reduced offsets to allow chart to fill more space
            setExtraOffsets(5f, 5f, 5f, 5f) // Smaller offsets = potentially bigger chart

            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT) // Keep hole transparent

            // <<< CHANGE: Increased hole size for more center text space
            holeRadius = 58f // Increased from 50f
            transparentCircleRadius = 61f // Keep slightly larger than holeRadius

            setDrawCenterText(true)
            centerText = "Total\nR 0.00" // Initial text

            // <<< CHANGE: Optionally increase center text size slightly if hole is bigger
            setCenterTextSize(16f) // Increased from 14f, adjust as needed
            setCenterTextColor(Color.WHITE) // Keep white or change if background isn't dark

            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            // --- Legend Configuration ---
            // Consider disabling if you need maximum space for the pie:
            // legend.isEnabled = false
            legend.apply {
                isEnabled = true // Keep enabled for now
                textColor = Color.WHITE
                textSize = 10f // Keep legend text size reasonable
                formSize = 8f
                form = Legend.LegendForm.CIRCLE
                verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                isWordWrapEnabled = true
                yOffset = 0f
                // <<< CHANGE: Increase X Offset slightly if legend feels too close after reducing chart offsets
                xOffset = 10f // Slightly more space from the chart edge
            }

            setDrawEntryLabels(false) // Keep slice labels off
            // setEntryLabelColor(Color.WHITE) // Not needed if entry labels are off
            // setEntryLabelTextSize(10f)    // Not needed if entry labels off

            // --- No Data Text ---
            context?.let { ctx ->
                setNoDataText("No expense data for this period.")
                setNoDataTextColor(ContextCompat.getColor(ctx, android.R.color.darker_gray))
            } ?: Log.w(TAG, "setupPieChart: Context is null, cannot set no data text color.")
        }
        Log.d(TAG, "setupPieChart: Configuration complete.")
    }

    private fun setupChartListener() {
        Log.d(TAG, "setupChartListener: Setting up chart value selection listener.")
        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) { // Parameters are nullable
                if (e is PieEntry && e.label != null) {
                    val categoryName = e.label
                    Log.d(TAG, "Pie slice selected: Category '$categoryName'")
                    navigateToCategoryDetails(categoryName)
                } else {
                    Log.w(TAG, "Selected entry is not a PieEntry or its label is null.")
                }
            }

            override fun onNothingSelected() {
                Log.d(TAG, "Pie chart - nothing selected")
            }
        })
    }

    private fun setupPeriodSpinner() {
        Log.d(TAG, "setupPeriodSpinner: Setting up period spinner.")
        val periods = listOf("This Month", "Last Month", "This Year") // Consider making this an enum

        // Use context safely
        context?.let { ctx ->
            // Use the custom layout for the selected item
            val adapter = ArrayAdapter(ctx, R.layout.spinner_selected_item, periods)
            // Use the custom layout for the dropdown items
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            periodSpinner.adapter = adapter

            periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // Remove manual text color setting, handled by spinner_selected_item.xml
                    val selectedPeriod = periods[position]
                    Log.i(TAG, "Period selected via spinner: $selectedPeriod")
                    // Update date range based on selection
                    val (startDate, endDate) = getDateRangeForPeriod(selectedPeriod)
                    if (startDate != null && endDate != null) {
                        currentStartDate = startDate
                        currentEndDate = endDate
                        // Observing allTransactions flow will automatically react to date range changes in filter
                        // No explicit load call needed here if the flow is already being observed and filtered
                    } else {
                        // Handle invalid period selection if necessary
                        currentStartDate = 0L
                        currentEndDate = 0L
                        clearUiData() // Clear data if period is invalid or date range cannot be calculated
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) { /* No action needed */ }
            }
            // Set background tint safely
            periodSpinner.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
            Log.d(TAG, "setupPeriodSpinner: Setup complete.")
        } ?: Log.e(TAG, "setupPeriodSpinner: Context is null, cannot setup spinner.")
    }


    private fun updatePieChart(transactions: List<Transaction>) { // Changed parameter to List<Transaction>
        Log.d(TAG, "updatePieChart: Updating with ${transactions.size} transactions.")
        if (!isAdded) {
            Log.w(TAG, "updatePieChart: Fragment not attached, skipping update.")
            return
        }

        // Filter for expense transactions and group by category to calculate totals
        val categoryTotals = transactions
            .filter { it.type == Transaction.Type.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }


        // Check for empty or all-zero data
        val positiveEntries = categoryTotals.filter { it.value > 0 }
        if (positiveEntries.isEmpty()) {
            pieChart.data = null
            pieChart.centerText = "Total\nR 0.00"
            // <<< ADDED: Ensure usePercentValues is false when there's no data
            pieChart.setUsePercentValues(false)
            pieChart.invalidate()
            Log.d(TAG, "updatePieChart: No positive expense data, chart cleared.")
            return
        }

        val entries = ArrayList<PieEntry>()
        positiveEntries.forEach { (category, total) ->
            entries.add(PieEntry(total.toFloat(), category))
        }

        Log.d(TAG, "updatePieChart: Created ${entries.size} PieEntry objects.")

        val dataSet = PieDataSet(entries, "")
        dataSet.apply {
            colors = getChartColors(positiveEntries.keys.toList())
            sliceSpace = 3f
            selectionShift = 5f

            // --- Value (Percentage) Configuration ---
            setDrawValues(true)

            // <<< CHANGE: Set values to draw INSIDE the slices >>>
            yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE

            // <<< CHANGE: Set text size for percentages (adjust as needed) >>>
            valueTextSize = 11f // Keep slightly larger size for now

            valueTextColor = Color.WHITE

            // Use PercentFormatter
            valueFormatter = PercentFormatter(pieChart)
        }

        val data = PieData(dataSet)

        pieChart.data = data

        // <<< IMPORTANT: Call setUsePercentValues AFTER setting data when using PercentFormatter(pieChart)
        pieChart.setUsePercentValues(true)

        // Update center text
        val totalSpent = positiveEntries.values.sum()
        pieChart.centerText = String.format(Locale.getDefault(), "Total\nR %.2f", totalSpent)

        // Refresh chart
        pieChart.animateY(1000) // Keep animation
        pieChart.invalidate()
        Log.d(TAG, "updatePieChart: Chart updated and refreshed.")
    }


    private fun setupButtonListeners() {
        Log.d(TAG, "setupButtonListeners: Setting up income/expense button listeners.")
        balanceIncomeButton.setOnClickListener {
            Log.d(TAG, "Add Income button clicked.")
            showTransactionDialog(Transaction.Type.INCOME)
        }
        balanceExpenseButton.setOnClickListener {
            Log.d(TAG, "Add Expense button clicked.")
            showTransactionDialog(Transaction.Type.EXPENSE)
        }
        addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }
    }


    // *************************************************************************
    // showTransactionDialog for Automatic Expense Categorization
    // *************************************************************************
    private fun showTransactionDialog(type: Transaction.Type) {
        Log.d(TAG, "showTransactionDialog: Showing dialog for type: ${type.name}")
        if (currentDialog?.isShowing == true) {
            Log.w(TAG, "showTransactionDialog: Dialog already showing.")
            return
        }

        // Use requireContext() safely, return if context is not available
        val context = requireContext() ?: return

        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_add_transaction) // Use your modified layout
        currentDialog = dialog

        try {
            val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting dialog layout params", e)
        }

        // Initialize dialog views (Assume IDs remain the same for now)
        val titleTextView = dialog.findViewById<TextView>(R.id.dialogTitleTextView)
        val amountEditText = dialog.findViewById<EditText>(R.id.amountEditText) // Changed to EditText
        val descriptionEditText = dialog.findViewById<EditText>(R.id.descriptionEditText) // Changed to EditText
        // REMOVED: val categoryInputLayout = dialog.findViewById<View>(R.id.categoryInputLayout)
        val takePictureButton = dialog.findViewById<Button>(R.id.takePictureButton)
        val chooseImageButton = dialog.findViewById<Button>(R.id.chooseImageButton)
        val receiptImageView = dialog.findViewById<ImageView>(R.id.receiptImagePreview)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val addButton = dialog.findViewById<Button>(R.id.addButton)

        // Declare and initialize categoryAutoComplete if it exists in this dialog layout
        // Based on the conflicting declaration error, it seems you might have intended to use it here,
        // but the logic determines the category automatically. If your dialog_add_transaction.xml
        // *does* have a categoryAutoComplete, initialize it here. Otherwise, remove this line.
        // For now, assuming it might exist for completeness, but the logic below doesn't use its text.
        val categoryAutoComplete = dialog.findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete)


        if (amountEditText == null || descriptionEditText == null || addButton == null || cancelButton == null || titleTextView == null) {
            Log.e(TAG, "showTransactionDialog: Could not find essential views. Aborting.")
            showToast("Error displaying dialog.")
            dialog.dismiss()
            currentDialog = null
            return
        }

        // Reset image selection
        selectedImageUri = null
        receiptImageView?.setImageResource(android.R.drawable.ic_menu_gallery) // Reset preview

        // Set Title and Add Button text
        titleTextView.text = if (type == Transaction.Type.INCOME) "Add Income" else "Add Expense"
        addButton.text = if (type == Transaction.Type.INCOME) "ADD INCOME" else "ADD EXPENSE"



        // Keep image button listeners
        takePictureButton?.setOnClickListener { if (checkCameraPermission()) dispatchTakePictureIntent() }
        chooseImageButton?.setOnClickListener { openGallery() }

        cancelButton.setOnClickListener {
            Log.d(TAG, "Add Transaction Dialog: Cancel clicked.")
            dialog.dismiss()
            currentDialog = null
        }

        addButton.setOnClickListener {
            Log.d(TAG, "Add Transaction Dialog: Add button clicked.")
            val amountStr = amountEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()
            // Removed duplicate declaration of category
            // val category = categoryAutoComplete?.text?.toString()?.trim() ?: "" // This line was using unresolved reference initially
            val date = System.currentTimeMillis()


            // Validation
            if (amountStr.isBlank() || description.isBlank()) {
                showToast("Please fill amount and description")
                return@setOnClickListener
            }
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                showToast("Please enter a valid positive amount")
                return@setOnClickListener
            }

            // **MODIFICATION**: Determine Category
            val category: String // Single declaration
            category = if (type == Transaction.Type.INCOME) {
                "Income" // Assuming "Income" is a valid category for income transactions
            } else {
                // Use your existing logic for suggesting/defaulting category for expenses
                CategorizationUtils.suggestCategory(description) ?: "Other"
            }
            Log.d(TAG,"Determined category: $category for type: ${type.name}, description: '$description'")


            val transaction = Transaction(
                // id is generated by Firestore when using add() - repository handles local ID
                type = type,
                amount = amount,
                description = description,
                category = category, // Use determined category
                date = date, // Using client-side date for now, will be overwritten by serverTimestamp if FirestoreService does that
                receiptImageUri = selectedImageUri?.toString() // Keep image URI (String? is fine)
            )

            Log.d(TAG, "Attempting to save transaction: $transaction")
            addTransaction(transaction) // Save using the repository

            dialog.dismiss()
            currentDialog = null
        }

        dialog.setOnDismissListener {
            Log.d(TAG, "Add Transaction Dialog dismissed.")
            currentDialog = null
        }


        dialog.show()
        Log.d(TAG, "showTransactionDialog: Dialog shown.")
    }

    // *** ADDED: Implementation of TransactionAdapter.OnItemClickListener ***
    override fun onItemClick(transaction: Transaction) {
        Log.d(TAG, "Transaction item clicked: ${transaction.description}")
        // Show the edit dialog for this transaction
        showEditTransactionDialog(transaction)
    }


    // *** ADDED: Function to show the Edit Transaction Dialog ***
    private fun showEditTransactionDialog(transaction: Transaction) {
        Log.d(TAG, "showEditTransactionDialog: Showing dialog for transaction: ${transaction.firestoreId}")
        if (currentDialog?.isShowing == true) {
            Log.w(TAG, "showEditTransactionDialog: Dialog already showing.")
            return
        }

        val context = requireContext() ?: return

        val dialog = Dialog(context)
        // *** IMPORTANT: Use the new edit dialog layout ***
        dialog.setContentView(R.layout.dialog_edit_transaction)
        currentDialog = dialog

        try {
            val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting dialog layout params", e)
        }


        // Initialize dialog views
        val titleTextView = dialog.findViewById<TextView>(R.id.dialogTitleTextView)
        val amountEditText = dialog.findViewById<EditText>(R.id.amountEditText)
        val descriptionEditText = dialog.findViewById<EditText>(R.id.descriptionEditText)
        // Category AutoCompleteTextView from the edit dialog layout - Declared and initialized
        val categoryAutoComplete = dialog.findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val deleteButton = dialog.findViewById<Button>(R.id.deleteButton) // Added Delete button
        val saveButton = dialog.findViewById<Button>(R.id.saveButton) // Renamed Add to Save

        if (amountEditText == null || descriptionEditText == null || categoryAutoComplete == null ||
            saveButton == null || cancelButton == null || deleteButton == null || titleTextView == null) {
            Log.e(TAG, "showEditTransactionDialog: Could not find essential views. Aborting.")
            showToast("Error displaying edit dialog.")
            dialog.dismiss()
            currentDialog = null
            return
        }


        // Populate dialog with transaction data
        titleTextView.text = "Edit ${transaction.type.name.lowercase().replaceFirstChar { it.uppercase() }}"
        amountEditText.setText(String.format(Locale.getDefault(), "%.2f", transaction.amount))
        descriptionEditText.setText(transaction.description)


        // Setup Category AutoCompleteTextView with categories, ensuring 'Other' is last
        var allCategories = CategorizationUtils.allCategories.toMutableList()
        if (allCategories.remove("Other")) {
            allCategories.add("Other")
        }
        val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, allCategories)

        categoryAutoComplete.setAdapter(categoryAdapter)
        categoryAutoComplete.threshold = 1

        // Set the current category in the AutoCompleteTextView
        categoryAutoComplete.setText(transaction.category, false) // false to not show dropdown


        cancelButton.setOnClickListener {
            Log.d(TAG, "Edit Transaction Dialog: Cancel clicked.")
            dialog.dismiss()
            currentDialog = null
        }

        deleteButton.setOnClickListener {
            Log.d(TAG, "Edit Transaction Dialog: Delete clicked for ID: ${transaction.firestoreId}")
            // *** ADDED: Implement Delete functionality ***
            deleteTransaction(transaction) // Use the repository
            dialog.dismiss()
            currentDialog = null
        }


        saveButton.setOnClickListener {
            Log.d(TAG, "Edit Transaction Dialog: Save clicked for ID: ${transaction.firestoreId}")
            val amountStr = amountEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()
            val selectedCategory = categoryAutoComplete.text.toString().trim() // Use initialized variable

            // Validation
            if (amountStr.isBlank() || description.isBlank() || selectedCategory.isBlank()) {
                showToast("Please fill all fields")
                return@setOnClickListener
            }
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                showToast("Please enter a valid positive amount")
                return@setOnClickListener
            }
            // Optional: Validate if the selected category is in the list of all categories if strict
            if (!allCategories.contains(selectedCategory) && selectedCategory != "Uncategorized") {
                Log.w(TAG, "Selected category '$selectedCategory' not in known categories.")
            }


            // Create an updated transaction object
            val updatedTransaction = transaction.copy(
                amount = amount,
                description = description,
                category = selectedCategory
                // Receipt image handling would go here if implemented for edit dialog
            )

            // *** ADDED: Call function to update in Firestore ***
            updateTransaction(updatedTransaction) // Use the repository

            dialog.dismiss()
            currentDialog = null
        }

        dialog.setOnDismissListener {
            Log.d(TAG, "Edit Transaction Dialog dismissed.")
            currentDialog = null
        }


        dialog.show()
        Log.d(TAG, "showEditTransactionDialog: Dialog shown.")
    }

    // *** ADDED: Placeholder for showAddCategoryDialog ***
    private fun showAddCategoryDialog() {
        Log.d(TAG, "showAddCategoryDialog: Showing dialog to add a new category (Placeholder)")
        // Implement the logic to show a dialog for adding a new category.
        // This would typically involve:
        // 1. Inflating a dialog layout with an EditText for the new category name.
        // 2. Getting the user input.
        // 3. Adding the new category to your list of available categories (e.g., in CategorizationUtils or a separate repository).
        // 4. Refreshing any UI elements that display categories (like the AutoCompleteTextViews).
        context?.let {
            Toast.makeText(it, "Add Category dialog would be shown here.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showToast(message: String) {
        // Use context safely
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        // Define TAG consistently
        private const val TAG = "DashboardFragment"
        fun newInstance(): DashboardFragment {
            Log.d(TAG, "Creating new instance requested.")
            return DashboardFragment()
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: Cleaning up dialog reference.")
        currentDialog?.dismiss() // Dismiss safely
        currentDialog = null
        // Optional: Nullify view references if not using view binding
        // pieChart = null // etc.
        super.onDestroyView()
    }

    private fun initializeActivityResultLaunchers() {
        Log.d(TAG, "initializeActivityResultLaunchers: Setting up.")
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> // Use correct contract
            if (result.resultCode == Activity.RESULT_OK) {
                currentPhotoPath?.let { path ->
                    val file = File(path)
                    if(file.exists() && file.length() > 0) { // Check file size too
                        Log.d(TAG, "Camera Result OK, File exists: $path, Size: ${file.length()}")
                        // Get URI using FileProvider *after* confirming file exists
                        selectedImageUri = try {
                            FileProvider.getUriForFile(requireContext(),"com.example.notbroke.fileprovider", file)
                        } catch (e: IllegalArgumentException) {
                            Log.e(TAG, "Error getting URI for file: $path", e)
                            null
                        }

                        if (selectedImageUri != null) {
                            // This part needs to be updated to handle preview in the *current* dialog (add or edit)
                            // loadBitmapFromUri(selectedImageUri) // Original call
                            currentDialog?.findViewById<ImageView>(R.id.receiptImagePreview)?.let { imageView ->
                                loadBitmapIntoImageView(selectedImageUri, imageView)
                            } ?: Log.w(TAG, "Receipt preview ImageView not found in current dialog.")

                        } else {
                            showToast("Failed to get image URI.")
                        }

                    } else {
                        Log.e(TAG, "Camera Result OK, but file not found or empty at: $path")
                        showToast("Failed to save picture")
                        currentPhotoPath = null // Reset path if file is bad
                    }
                } ?: run {
                    Log.e(TAG, "Camera Result OK, but currentPhotoPath is null")
                    showToast("Failed to get picture path")
                }
            } else {
                Log.w(TAG, "Camera Result NOT OK, Code: ${result.resultCode}")
                // Optionally delete the empty file if created
                currentPhotoPath?.let { path -> File(path).delete() }
                currentPhotoPath = null
            }
        }
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> // Use correct contract
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    Log.d(TAG, "Gallery Result OK, URI: $uri")
                    // Persist permission for gallery URI if needed, though less common for simple display
                    // val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    // requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                    selectedImageUri = uri
                    // This part needs to be updated to handle preview in the *current* dialog (add or edit)
                    // loadBitmapFromUri(selectedImageUri) // Original call
                    currentDialog?.findViewById<ImageView>(R.id.receiptImagePreview)?.let { imageView ->
                        loadBitmapIntoImageView(selectedImageUri, imageView)
                    } ?: Log.w(TAG, "Receipt preview ImageView not found in current dialog.")

                } ?: run {
                    Log.w(TAG, "Gallery Result OK, but URI is null")
                    showToast("Failed to get image from gallery")
                }
            } else {
                Log.w(TAG, "Gallery Result NOT OK, Code: ${result.resultCode}")
            }
        }
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> // Use correct contract
            if (isGranted) {
                Log.d(TAG, "Camera Permission Granted.")
                dispatchTakePictureIntent() // Retry taking picture
            } else {
                Log.w(TAG, "Camera Permission Denied.")
                showToast("Camera permission is required to take pictures")
            }
        }
    }

    private fun loadBitmapIntoImageView(uri: Uri?, imageView: ImageView?) {
        if (uri == null || imageView == null) return
        context?.let { ctx ->
            try {
                ctx.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    imageView.setImageBitmap(bitmap)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    Log.d(TAG, "Bitmap loaded into preview from URI: $uri")
                } ?: Log.e(TAG, "Failed to open input stream for URI: $uri")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bitmap from URI: $uri", e)
                showToast("Failed to load image preview")
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } ?: Log.w(TAG, "loadBitmapIntoImageView: Context is null.")
    }

    private fun checkCameraPermission(): Boolean {
        return context?.let { ctx ->
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                Log.d(TAG, "Camera permission not granted, requesting...")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                false
            }
        } ?: run {
            Log.w(TAG, "checkCameraPermission: Context is null, cannot check permission.")
            false
        }
    }


    private fun dispatchTakePictureIntent() {
        Log.d(TAG, "dispatchTakePictureIntent: Attempting to launch camera.")
        context?.let { ctx ->
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                if (takePictureIntent.resolveActivity(ctx.packageManager) == null) {
                    Log.e(TAG, "No camera app found to handle intent.")
                    showToast("No camera application found")
                    return
                }

                try {
                    val photoFile: File = createImageFile()
                    val photoURI: Uri = FileProvider.getUriForFile(
                        ctx,
                        "com.example.notbroke.fileprovider",
                        photoFile
                    )
                    currentPhotoPath = photoFile.absolutePath
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    Log.d(TAG, "Launching camera intent with output URI: $photoURI and path: $currentPhotoPath")
                    takePictureLauncher.launch(takePictureIntent)
                } catch (ex: IOException) {
                    Log.e(TAG, "Error creating image file", ex)
                    showToast("Could not prepare camera (file error)")
                    currentPhotoPath = null // Reset path on error
                } catch (ex: IllegalArgumentException) {
                    Log.e(TAG, "Error creating FileProvider URI (check provider config)", ex)
                    showToast("Could not prepare camera (URI error)")
                    currentPhotoPath = null
                } catch (ex: SecurityException) {
                    Log.e(TAG, "Security exception launching camera, check permissions?", ex)
                    showToast("Camera permission issue") // This is where the "Camera permission issue" toast originates
                    currentPhotoPath = null
                }
            }
        } ?: Log.w(TAG, "dispatchTakePictureIntent: Context is null.")
    }



    @Throws(IOException::class)
    private fun createImageFile(): File {
        val context = requireContext() ?: throw IOException("Context is unavailable")
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir == null) {
            Log.e(TAG, "External picture directory is null.")
            throw IOException("Cannot access picture storage directory")
        }
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e(TAG, "External picture directory does not exist and could not be created.")
            throw IOException("Cannot create picture storage directory")
        }

        Log.d(TAG, "Creating image file in: ${storageDir.absolutePath}")
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }


    private fun openGallery() {
        Log.d(TAG, "openGallery: Launching gallery picker intent.")
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    // ==========================================================
    //              Firestore & Data Handling Methods
    // ==========================================================

    private fun addTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                transactionRepository.saveTransaction(transaction)
                Toast.makeText(context, "Transaction added successfully", Toast.LENGTH_SHORT).show()
                // Data will be updated automatically via the observed Flow
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to add transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error adding transaction", e)
            }
        }
    }

    private fun updateTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                transactionRepository.updateTransaction(transaction)
                Toast.makeText(context, "Transaction updated successfully", Toast.LENGTH_SHORT).show()
                // Data will be updated automatically via the observed Flow
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error updating transaction", e)
            }
        }
    }

    private fun deleteTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction)
                Toast.makeText(context, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
                // Data will be updated automatically via the observed Flow
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error deleting transaction", e)
            }
        }
    }

    // This function is now primarily used to set the date range,
    // the observation of allTransactions handles the filtering and UI updates.
    private fun loadTransactionsForPeriod(period: String) {
        val userId = authService.getCurrentUserId() ?: return
        Log.d(TAG, "Loading transactions for period: $period")
        // The observation of allTransactions flow will filter based on the set currentStartDate and currentEndDate
    }


    private fun updateTransactionSummary(transactions: List<Transaction>) {
        updateBalance(transactions)
        // Calculate total spent ONLY for Expense type transactions within the current date range
        val totalSpent = transactions
            .filter { it.type == Transaction.Type.EXPENSE }
            .sumOf { it.amount }
        totalSpentTextView.text = String.format(Locale.getDefault(), "R %.2f", totalSpent)

        totalBudgetTextView.text = "R ----.--" // TODO: Load actual budget for the period
        remainingTextView.text = "R ----.--" // TODO: Calculate remaining based on budget and spent
    }

    private fun getDateRangeForPeriod(period: String): Pair<Long?, Long?> {
        val calendar = Calendar.getInstance()
        var startDateMillis: Long?
        var endDateMillis: Long?

        try {
            when (period) {
                "This Month" -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    setCalendarToStartOfDay(calendar)
                    startDateMillis = calendar.timeInMillis

                    // End of month calculation (go to start of next month, subtract 1ms)
                    calendar.add(Calendar.MONTH, 1)
                    setCalendarToStartOfDay(calendar) // Go to start of next month
                    calendar.add(Calendar.MILLISECOND, -1) // Subtract 1ms to get end of current month
                    endDateMillis = calendar.timeInMillis
                }
                "Last Month" -> {
                    // Go to start of this month
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    setCalendarToStartOfDay(calendar)
                    // Subtract 1ms to get end of last month
                    calendar.add(Calendar.MILLISECOND, -1)
                    endDateMillis = calendar.timeInMillis

                    // Calendar is now at the end of last month. Set to start of last month.
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    setCalendarToStartOfDay(calendar)
                    calendar.add(Calendar.MONTH, -1) // Go back one month
                    startDateMillis = calendar.timeInMillis
                }
                "This Year" -> {
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    setCalendarToStartOfDay(calendar)
                    startDateMillis = calendar.timeInMillis

                    // End of year calculation
                    calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                    calendar.set(Calendar.DAY_OF_MONTH, 31)
                    setCalendarToEndOfDay(calendar)
                    endDateMillis = calendar.timeInMillis
                }
                else -> {
                    Log.w(TAG, "getDateRangeForPeriod: Unknown period '$period'. Returning null.")
                    return Pair(null, null)
                }
            }
            // Log the calculated date range for debugging
            if(startDateMillis != null && endDateMillis != null) {
                Log.d(TAG,"Date range for '$period': ${Date(startDateMillis)} to ${Date(endDateMillis)}")
            }
            // The calling function will set currentStartDate and currentEndDate
            return Pair(startDateMillis, endDateMillis)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating date range for '$period'", e)
            return Pair(null, null)
        }
    }

    // Helper to set Calendar time to 00:00:00.000
    private fun setCalendarToStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    // Helper to set Calendar time to 23:59:59.999
    private fun setCalendarToEndOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
    }

    // Helper to get consistent colors for categories
    private fun getChartColors(categories: List<String>): List<Int> {
        // Use a predefined color palette or generate consistent colors based on category hashcode
        val colors = mutableListOf<Int>()
        val predefinedColors = listOf(
            Color.parseColor("#F44336"), Color.parseColor("#E91E63"), Color.parseColor("#9C27B0"),
            Color.parseColor("#673AB7"), Color.parseColor("#3F51B5"), Color.parseColor("#2196F3"),
            Color.parseColor("#03A9F4"), Color.parseColor("#00BCD4"), Color.parseColor("#009688"),
            Color.parseColor("#4CAF50"), Color.parseColor("#8BC34A"), Color.parseColor("#CDDC39"),
            Color.parseColor("#FFEB3B"), Color.parseColor("#FFC107"), Color.parseColor("#FF9800"),
            Color.parseColor("#FF5722"), Color.parseColor("#795548"), Color.parseColor("#9E9E9E"),
            Color.parseColor("#607D8B")
        )

        categories.forEachIndexed { index, category ->
            // Use predefined colors first, then generate if needed
            colors.add(predefinedColors.getOrElse(index % predefinedColors.size) {
                // Fallback: generate color from hashcode if more categories than predefined colors
                val hue = (category.hashCode() % 360).toFloat()
                Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.8f))
            })
        }
        return colors
    }

    // Placeholder for navigation - implement this based on your navigation setup
    private fun navigateToCategoryDetails(categoryName: String) {
        if (currentStartDate == 0L || currentEndDate == 0L) {
            Log.w(TAG, "navigateToCategoryDetails: Date range not set, cannot navigate.")
            showToast("Error: Date range not available")
            return
        }
        Log.i(TAG,"Attempting navigation to details for category: '$categoryName' (Period: ${Date(currentStartDate)} - ${Date(currentEndDate)})")
        showToast("Navigate for: $categoryName") // Placeholder
    }


    private fun clearUiData() {
        Log.d(TAG, "clearUiData: Clearing transaction list, chart, and balance.")
        // Use runOnUiThread safely checking fragment attachment
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            transactionAdapter.submitList(emptyList())
            updatePieChart(emptyList()) // Pass empty list to updatePieChart
            updateBalance(emptyList())
            totalSpentTextView.text = "R 0.00"
            totalBudgetTextView.text = "R ----.--"
            remainingTextView.text = "R ----.--"
        }
    }

    private fun observeTransactions() {
        val userId = authService.getCurrentUserId() ?: return

        lifecycleScope.launch {
            // Explicitly specify the type of the collected list
            transactionRepository.allTransactions.collectLatest { transactions: List<Transaction> ->
                Log.d(TAG, "Observed ${transactions.size} transactions. Current date range: ${Date(currentStartDate)} to ${Date(currentEndDate)}")
                // Filter transactions by the currently set date range
                val filteredTransactions = if (currentStartDate > 0 && currentEndDate > 0) {
                    transactions.filter { transaction: Transaction -> // Explicitly specify the type of 'it'
                        transaction.date in currentStartDate..currentEndDate
                    }
                } else {
                    // If no specific date range is set, show all or handle as per default period logic
                    transactions // Showing all if range is 0..0, adjust if default period should be "This Month" initially
                }

                Log.d(TAG, "Filtered to ${filteredTransactions.size} transactions for current period.")

                // Update adapter with filtered transactions
                transactionAdapter.submitList(filteredTransactions)

                // Update UI with transaction data
                updateTransactionSummary(filteredTransactions) // Pass filtered transactions
                updatePieChart(filteredTransactions) // Pass filtered transactions
            }
        }
    }

}