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
import com.example.notbroke.adapters.TransactionAdapter
import com.example.notbroke.models.Transaction
import android.graphics.Color
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.Entry // Import Entry
import com.github.mikephil.charting.data.PieEntry
import android.content.res.ColorStateList
import com.example.notbroke.R
// REMOVE: import com.google.android.material.textfield.TextInputEditText // Not needed after removing manual category
// REMOVE: import android.widget.AutoCompleteTextView // Not needed after removing manual category
import android.widget.Button // Still needed for dialog buttons
import android.app.Dialog
import com.google.android.material.button.MaterialButton
import android.Manifest // Keep for permissions
import android.app.Activity // Keep for ActivityResult
import android.content.Intent // Keep for Intents
import android.content.pm.PackageManager // Keep for permissions
import android.graphics.Bitmap // Keep for images
import android.graphics.BitmapFactory // Keep for images
import android.net.Uri // Keep for images
import android.os.Environment // Keep for images
import android.provider.MediaStore // Keep for images
import androidx.activity.result.ActivityResultLauncher // Keep for ActivityResult
import androidx.activity.result.contract.ActivityResultContracts // Keep for ActivityResult
import androidx.core.app.ActivityCompat // Keep for permissions
import androidx.core.content.ContextCompat // Keep for permissions/colors
import androidx.core.content.FileProvider // Keep for camera
import java.io.File // Keep for camera
import java.io.IOException // Keep for camera
import java.text.SimpleDateFormat // Keep for camera filename
import java.util.Date // Keep for camera filename
import java.util.Locale // Keep for camera filename
import android.widget.ImageView // Keep for image preview
// REMOVE: import android.text.Editable // Not needed after removing manual category suggestion
// REMOVE: import android.text.TextWatcher // Not needed after removing manual category suggestion
import android.widget.AdapterView // Add for Spinner listener
import android.widget.AutoCompleteTextView
import android.widget.EditText // Import EditText directly
import android.widget.RadioGroup
import com.google.android.material.textfield.TextInputEditText

// ===== Add necessary new imports =====
import androidx.lifecycle.lifecycleScope // Add for Coroutines
import com.example.notbroke.utils.CategorizationUtils // Add the new utils class
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.PercentFormatter // Add for chart formatting
import com.github.mikephil.charting.highlight.Highlight // Add for chart interactivity
import com.github.mikephil.charting.listener.OnChartValueSelectedListener // Add for chart interactivity
import com.google.firebase.auth.FirebaseAuth // Add for Firebase Auth
import com.google.firebase.firestore.FirebaseFirestore // Add for Firestore
import com.google.firebase.firestore.Query // Add for Firestore queries
import com.google.firebase.firestore.QuerySnapshot // Add for Firestore results
// REMOVE: import com.google.firebase.firestore.ktx.toObject // Not needed if mapping manually
import kotlinx.coroutines.launch // Add for Coroutines
import kotlinx.coroutines.tasks.await // Add for Coroutines + Tasks API
import java.util.* // Add for Calendar


class DashboardFragment : Fragment() {
    // Use companion object TAG for consistency
    // private val TAG = "DashboardFragment" // Remove this line

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

    // ===== Add Firebase instances =====
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Store the current date range for navigation
    private var currentStartDate: Long = 0L
    private var currentEndDate: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeActivityResultLaunchers() // Keep this
        // Initialize Firebase
        try {
            db = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
            Log.d(TAG, "onCreate: Initialized Firebase Auth and Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
            showToast("Failed to initialize core services. Please restart the app.")
            // Consider preventing fragment load if Firebase fails
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Inflating dashboard fragment layout")
        // Use try-catch for layout inflation
        return try {
            inflater.inflate(R.layout.fragment_dashboard, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error inflating layout R.layout.fragment_dashboard", e)
            // Optionally return a simple error view
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up dashboard fragment views and logic")

        try {
            initializeViews(view)
            setupButtonListeners() // Renamed from setupFabListeners
            setupTransactionsRecyclerView()

            // Setup budget components - Chart and Spinner setup remains
            setupPieChart()
            setupChartListener()
            setupPeriodSpinner() // This will now trigger the initial data load via its listener

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
            // setEntryLabelTextSize(10f)    // Not needed if entry labels are off

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
            val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, periods)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            periodSpinner.adapter = adapter

            periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // Style the selected item (optional, handle potential null view)
                    (view as? TextView)?.setTextColor(Color.parseColor("#FFD700"))
                    val selectedPeriod = periods[position]
                    Log.i(TAG, "Period selected via spinner: $selectedPeriod")
                    loadTransactionsForPeriod(selectedPeriod) // Trigger Firestore load
                }
                override fun onNothingSelected(parent: AdapterView<*>?) { /* No action needed */ }
            }
            // Set background tint safely
            periodSpinner.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
            Log.d(TAG, "setupPeriodSpinner: Setup complete.")
        } ?: Log.e(TAG, "setupPeriodSpinner: Context is null, cannot setup spinner.")
    }


    private fun updatePieChart(categoryTotals: Map<String, Double>) {
        Log.d(TAG, "updatePieChart: Updating with ${categoryTotals.size} categories.")
        if (!isAdded) {
            Log.w(TAG, "updatePieChart: Fragment not attached, skipping update.")
            return
        }

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
    // ** MODIFIED: showTransactionDialog for Automatic Expense Categorization **
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
        val categoryInputLayout = dialog.findViewById<View>(R.id.categoryInputLayout) // Get the Layout container for category
        val categoryAutoComplete = dialog.findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete) // Get the AutoCompleteTextView
        val takePictureButton = dialog.findViewById<Button>(R.id.takePictureButton)
        val chooseImageButton = dialog.findViewById<Button>(R.id.chooseImageButton)
        val receiptImageView = dialog.findViewById<ImageView>(R.id.receiptImagePreview)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val addButton = dialog.findViewById<Button>(R.id.addButton)

        if (amountEditText == null || descriptionEditText == null ||addButton == null || cancelButton == null || titleTextView == null) {
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

//        // **MODIFICATION**: Hide category input for expenses
//        if (type == Transaction.Type.EXPENSE) {
//            categoryInputLayout.visibility = View.GONE
//            Log.d(TAG, "Dialog Type: EXPENSE - Hiding category input.")
//        } else {
//            categoryInputLayout.visibility = View.VISIBLE
//            Log.d(TAG, "Dialog Type: INCOME - Showing category input.")
//            // Setup Category Adapter for INCOME only
//            // (Assuming R.id.categoryAutoComplete is inside R.id.categoryInputLayout)
//            val categoryAutoComplete = dialog.findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete)
//            categoryAutoComplete?.let { // Ensure AutoCompleteTextView exists if layout is visible
//                val categories = CategorizationUtils.incomeCategories // Use specific income categories
//                val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)
//                it.setAdapter(categoryAdapter)
//                it.threshold = 1
//                Log.d(TAG, "Category adapter set for INCOME with ${categories.size} categories.")
//            } ?: Log.w(TAG, "Category AutoCompleteTextView not found, even though layout is visible for INCOME.")
//        }Replaced by code underneath temp
        // Load categories from Firestore
        val categoryType = if (type == Transaction.Type.INCOME) "income" else "expense"
        db.collection("categories")
            .whereEqualTo("type", categoryType)
            .whereEqualTo("userId", auth.currentUser?.uid)
            .get()
            .addOnSuccessListener { documents ->
                val categories = documents.map { it.getString("name") ?: "" }
                if (categories.isNotEmpty()) {
                    categoryInputLayout.visibility = View.VISIBLE
                    val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)
                    categoryAutoComplete?.setAdapter(categoryAdapter)
                    categoryAutoComplete?.threshold = 1
                    Log.d(TAG, "Loaded ${categories.size} categories for $categoryType")
                } else {
                    categoryInputLayout.visibility = View.GONE
                    Log.d(TAG, "No categories found for $categoryType")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading categories", e)
                categoryInputLayout.visibility = View.GONE
            }
        //end off replce code for modification


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
            val category = categoryAutoComplete?.text?.toString()?.trim() ?: ""
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

//            // **MODIFICATION**: Determine Category
//            val category: String
//            if (type == Transaction.Type.INCOME) {
//                // Get category from dropdown for income
//                val categoryAutoComplete = dialog.findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete)
//                category = categoryAutoComplete?.text?.toString()?.trim() ?: ""
//                if (category.isBlank() || !CategorizationUtils.incomeCategories.contains(category)) {
//                    showToast("Please select a valid income category")
//                    return@setOnClickListener
//                }
//                Log.d(TAG,"Income category selected: $category")
//            } else {
//                // Suggest category automatically for expense
//                category = CategorizationUtils.suggestCategory(description) ?: "Other" // Default to "Other" if no suggestion
//                Log.d(TAG,"Expense category suggested: $category for description: '$description'")
//            }
//
//            val transaction = Transaction(
//                id = date, // Temporary local ID
//                type = type,
//                amount = amount,
//                description = description,
//                category = category, // Use determined category
//                date = date,
//                receiptImageUri = selectedImageUri?.toString() // Keep image URI
//            )
//
//            Log.d(TAG, "Attempting to save transaction: $transaction")
//            saveTransactionToFirestore(transaction) // Save to Firestore
//
//            dialog.dismiss()
//            currentDialog = null
//        }
//
//        dialog.setOnDismissListener {
//            Log.d(TAG, "Add Transaction Dialog dismissed.")
//            currentDialog = null --Temp replacement--
            // Create transaction object
            val transaction = hashMapOf(
                "amount" to amount,
                "description" to description,
                "category" to category,
                "type" to type.name.toLowerCase(),
                "date" to date,
                "userId" to auth.currentUser?.uid,
                "imageUrl" to selectedImageUri?.toString()
            )
            // Save to Firestore
            db.collection("transactions")
                .add(transaction)
                .addOnSuccessListener {
                    showToast("Transaction added successfully")
                    dialog.dismiss()
                    currentDialog = null
                    loadTransactionsForPeriod(periodSpinner.selectedItem.toString())
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding transaction", e)
                    showToast("Failed to add transaction")
                }
        }

        dialog.show()
        Log.d(TAG, "showTransactionDialog: Dialog shown.")
    }
    private fun showAddCategoryDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_category)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val categoryNameInput = dialog.findViewById<TextInputEditText>(R.id.categoryNameInput)
        val categoryTypeGroup = dialog.findViewById<RadioGroup>(R.id.categoryTypeGroup)
        val addButton = dialog.findViewById<Button>(R.id.addCategoryButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelCategoryButton)

        addButton.setOnClickListener{
            val categoryName = categoryNameInput.text.toString().trim()
            if (categoryName.isEmpty()) {
                categoryNameInput.error = "Category name is required"
                return@setOnClickListener
            }
            val isIncome = categoryTypeGroup.checkedRadioButtonId == R.id.incomeRadioButton

            val categoryType = if (isIncome) "income" else "expense"

            // Save category to Firestore
            val category = hashMapOf(
                "name" to categoryName,
                "type" to categoryType,
                "userId" to auth.currentUser?.uid
            )

            db.collection("categories")
                .add(category)
                .addOnSuccessListener {
                    showToast("Category added successfully")
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding category", e)
                    showToast("Failed to add category")
                }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
    // *************************************************************************
    // ** END OF MODIFICATION **
    // *************************************************************************


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
                            loadBitmapFromUri(selectedImageUri)
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
                    loadBitmapFromUri(selectedImageUri) // Load preview
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


    private fun loadBitmapFromUri(uri: Uri?) {
        if (uri == null) return
        // Use context safely
        context?.let { ctx ->
            try {
                // Use contentResolver to open InputStream
                ctx.contentResolver.openInputStream(uri)?.use { inputStream -> // Use 'use' for auto-closing
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    currentDialog?.findViewById<ImageView>(R.id.receiptImagePreview)?.let { imageView ->
                        imageView.setImageBitmap(bitmap)
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        Log.d(TAG, "Bitmap loaded into preview from URI: $uri")
                    }
                } ?: Log.e(TAG, "Failed to open input stream for URI: $uri")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bitmap from URI: $uri", e)
                showToast("Failed to load image preview")
                // Reset preview on error
                currentDialog?.findViewById<ImageView>(R.id.receiptImagePreview)?.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } ?: Log.w(TAG, "loadBitmapFromUri: Context is null.")
    }


    private fun checkCameraPermission(): Boolean {
        // Use context safely
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
            false // Cannot proceed without context
        }
    }


    private fun dispatchTakePictureIntent() {
        Log.d(TAG, "dispatchTakePictureIntent: Attempting to launch camera.")
        // Use context safely
        context?.let { ctx ->
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Ensure the intent can be resolved
                if (takePictureIntent.resolveActivity(ctx.packageManager) == null) {
                    Log.e(TAG, "No camera app found to handle intent.")
                    showToast("No camera application found")
                    return
                }

                try {
                    val photoFile: File = createImageFile()
                    val photoURI: Uri = FileProvider.getUriForFile(
                        ctx,
                        "com.example.notbroke.fileprovider", // Ensure this matches AndroidManifest provider authority
                        photoFile
                    )
                    currentPhotoPath = photoFile.absolutePath // Store path *after* successful file creation
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    // Grant temporary write permission to the camera app
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
                    showToast("Camera permission issue")
                    currentPhotoPath = null
                }
            }
        } ?: Log.w(TAG, "dispatchTakePictureIntent: Context is null.")
    }



    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Use context safely
        val context = requireContext() ?: throw IOException("Context is unavailable")
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir == null) { // Check if storageDir itself is null
            Log.e(TAG, "External picture directory is null.")
            throw IOException("Cannot access picture storage directory")
        }
        if (!storageDir.exists() && !storageDir.mkdirs()) { // Check if exists OR can be created
            Log.e(TAG, "External picture directory does not exist and could not be created.")
            throw IOException("Cannot create picture storage directory")
        }

        Log.d(TAG, "Creating image file in: ${storageDir.absolutePath}")
        // Create the file
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }


    private fun openGallery() {
        Log.d(TAG, "openGallery: Launching gallery picker intent.")
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        // Consider adding type filtering if needed: intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    // ==========================================================
    // ===== Firestore & Data Handling Methods (with improvements) ====
    // ==========================================================

    private fun saveTransactionToFirestore(transaction: Transaction) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "saveTransactionToFirestore: User not logged in.")
            showToast("Error: Not logged in")
            return
        }
        Log.d(TAG, "saveTransactionToFirestore: Saving transaction for user $userId")

        // Using Server Timestamp is generally recommended for consistency
        val transactionData = mapOf(
            "amount" to transaction.amount,
            "type" to transaction.type.name,
            "description" to transaction.description,
            "category" to transaction.category,
            // "date" to transaction.date, // Keep local if offline support is complex
            "date" to com.google.firebase.firestore.FieldValue.serverTimestamp(), // Use server time
            "receiptImageUri" to transaction.receiptImageUri // Store as String or null
        )

        db.collection("users").document(userId)
            .collection("transactions")
            .add(transactionData) // Let Firestore generate the ID
            .addOnSuccessListener { documentReference ->
                Log.i(TAG, "saveTransactionToFirestore: Success! Document ID: ${documentReference.id}")
                showToast("${transaction.type.name.lowercase().replaceFirstChar { it.uppercase() }} added")
                // Refresh data for the currently selected period
                val selectedPeriod = periodSpinner.selectedItem as? String ?: "This Month"
                Log.d(TAG, "saveTransactionToFirestore: Refreshing data for period '$selectedPeriod'.")
                loadTransactionsForPeriod(selectedPeriod)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "saveTransactionToFirestore: Error adding transaction", e)
                showToast("Error saving transaction: ${e.localizedMessage}")
            }
    }

    private fun loadTransactionsForPeriod(period: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "loadTransactionsForPeriod: User not logged in.")
            clearUiData()
            return
        }

        val (startDate, endDate) = getDateRangeForPeriod(period)
        if (startDate == null || endDate == null) {
            Log.e(TAG, "loadTransactionsForPeriod: Invalid date range for period '$period'. Cannot load.")
            clearUiData()
            return
        }

        Log.i(TAG, "loadTransactionsForPeriod: Loading for '$period' (User: $userId, Start: ${Date(startDate)}, End: ${Date(endDate)})")
        // Show loading indicator?

        viewLifecycleOwner.lifecycleScope.launch { // Use lifecycleScope
            try {
                // Convert Long dates to Firestore Timestamp for querying
                val startTimestamp = com.google.firebase.Timestamp(startDate / 1000, (startDate % 1000 * 1000000).toInt())
                val endTimestamp = com.google.firebase.Timestamp(endDate / 1000, (endDate % 1000 * 1000000).toInt())

                val querySnapshot = db.collection("users").document(userId)
                    .collection("transactions")
                    .whereGreaterThanOrEqualTo("date", startTimestamp) // Query with Timestamp
                    .whereLessThanOrEqualTo("date", endTimestamp)     // Query with Timestamp
                    // Keep ordering by date on client side for flexibility
                    .get()
                    .await() // Use await() for cleaner async handling

                Log.d(TAG, "loadTransactionsForPeriod: Firestore fetch successful, ${querySnapshot.size()} documents.")
                processFirestoreResults(querySnapshot)

            } catch (e: Exception) {
                Log.e(TAG, "loadTransactionsForPeriod: Error fetching from Firestore", e)
                showToast("Error loading transactions: ${e.localizedMessage}")
                clearUiData()
            } finally {
                // Hide loading indicator
            }
        }
    }

    private fun processFirestoreResults(snapshot: QuerySnapshot) {
        Log.d(TAG, "processFirestoreResults: Processing ${snapshot.size()} documents.")
        val transactions = snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null // Skip if data is null

                val typeString = data["type"] as? String
                val transactionType = try {
                    if (typeString != null) Transaction.Type.valueOf(typeString) else Transaction.Type.EXPENSE
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Invalid type '$typeString' in doc ${doc.id}, defaulting to EXPENSE.")
                    Transaction.Type.EXPENSE
                }

                // Handle Firestore Timestamp for date
                val firestoreTimestamp = data["date"] as? com.google.firebase.Timestamp
                val dateMillis = firestoreTimestamp?.toDate()?.time ?: run {
                    // Fallback if date is stored as Long (should be avoided)
                    Log.w(TAG,"Date field in doc ${doc.id} is not a Timestamp, trying Long.")
                    (data["date"] as? Long) ?: 0L
                }
                if (dateMillis == 0L) {
                    Log.w(TAG,"Could not parse date for doc ${doc.id}, skipping.")
                    return@mapNotNull null // Skip if date is invalid
                }


                Transaction(
                    id = doc.id.hashCode().toLong(), // Use Firestore ID hash
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    type = transactionType,
                    description = data["description"] as? String ?: "",
                    category = data["category"] as? String ?: "Uncategorized",
                    date = dateMillis, // Use parsed milliseconds
                    receiptImageUri = data["receiptImageUri"] as? String // Nullable
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error mapping document ${doc.id}", e)
                null // Skip documents that fail mapping
            }
        }.sortedByDescending { it.date } // Sort by date descending

        Log.i(TAG, "processFirestoreResults: Mapped ${transactions.size} transactions successfully.")

        // Update UI on the main thread safely
        activity?.runOnUiThread {
            if (!isAdded) {
                Log.w(TAG, "processFirestoreResults: Fragment not attached, skipping UI update.")
                return@runOnUiThread
            }

            Log.d(TAG, "Updating UI: RecyclerView, Balance, PieChart")
            transactionAdapter.setTransactions(transactions)
            updateBalance(transactions)

            val expenseTransactions = transactions.filter { it.type == Transaction.Type.EXPENSE }
            val expenseCategoryTotals = calculateCategoryTotals(expenseTransactions)
            updatePieChart(expenseCategoryTotals)

            // Update Budget Summary Texts (Placeholders)
            val totalSpent = expenseCategoryTotals.values.sum()
            totalSpentTextView.text = String.format(Locale.getDefault(), "R %.2f", totalSpent)
            totalBudgetTextView.text = "R ----.--" // TODO: Load actual budget
            remainingTextView.text = "R ----.--" // TODO: Calculate remaining based on budget

            Log.d(TAG, "UI Update complete.")
        }
    }


    private fun calculateCategoryTotals(expenseTransactions: List<Transaction>): Map<String, Double> {
        Log.d(TAG, "calculateCategoryTotals: Calculating for ${expenseTransactions.size} expenses.")
        if (expenseTransactions.isEmpty()) return emptyMap()

        // Group by category, sum amounts, filter zero totals, and sort
        val totals = expenseTransactions
            .groupBy { it.category.trim().ifBlank { "Uncategorized" } } // Trim and handle blank categories
            .mapValues { (_, transactionsInCategory) ->
                transactionsInCategory.sumOf { it.amount }
            }
            .filterValues { it > 0 } // Only include categories with spending > 0
            .toSortedMap() // Sort categories alphabetically

        Log.d(TAG, "calculateCategoryTotals: Calculated totals for ${totals.size} categories.")
        return totals
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
                    calendar.add(Calendar.MILLISECOND, -1)
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
            // Store current range for navigation use
            currentStartDate = startDateMillis ?: 0L
            currentEndDate = endDateMillis ?: 0L
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

        // --- TODO: IMPLEMENT ACTUAL NAVIGATION ---
        // e.g., using Navigation Component:
        // val action = DashboardFragmentDirections.actionDashboardFragmentToCategoryDetailsFragment(
        //     categoryName,
        //     currentStartDate,
        //     currentEndDate
        // )
        // findNavController().navigate(action)
    }


    private fun clearUiData() {
        Log.d(TAG, "clearUiData: Clearing transaction list, chart, and balance.")
        // Use runOnUiThread safely checking fragment attachment
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            transactionAdapter.setTransactions(emptyList())
            updatePieChart(emptyMap()) // This will handle clearing the chart
            updateBalance(emptyList())
            totalSpentTextView.text = "R 0.00"
            totalBudgetTextView.text = "R ----.--"
            remainingTextView.text = "R ----.--"
        }
    }

} // === End of DashboardFragment class ===