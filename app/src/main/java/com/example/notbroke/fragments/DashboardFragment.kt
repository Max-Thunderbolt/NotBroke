package com.example.notbroke.fragments

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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import android.content.res.ColorStateList
import com.example.notbroke.R
import android.widget.Button
import android.app.Dialog
import com.google.android.material.button.MaterialButton
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.ImageView
import android.widget.AdapterView
import android.widget.EditText
import android.widget.AutoCompleteTextView
import androidx.lifecycle.lifecycleScope
import com.example.notbroke.utils.CategorizationUtils
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import com.example.notbroke.repositories.RepositoryFactory
import com.example.notbroke.repositories.TransactionRepository
import com.example.notbroke.services.FirestoreService
import com.example.notbroke.services.AuthService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CancellationException

/**
 * DashboardFragment displays the user's financial overview including transactions,
 * balance, and expense breakdown by category.
 */
class DashboardFragment : Fragment(), TransactionAdapter.OnItemClickListener {
    companion object {
        private const val TAG = "DashboardFragment"
        
        fun newInstance(): DashboardFragment {
            Log.d(TAG, "Creating new instance requested.")
            return DashboardFragment()
        }
    }

    // ===== UI Components =====
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

    // ===== Receipt Image Handling =====
    private var currentPhotoPath: String? = null
    private var selectedImageUri: Uri? = null
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var currentDialog: Dialog? = null

    // ===== Repositories and Services =====
    private lateinit var repositoryFactory: RepositoryFactory
    private val transactionRepository by lazy { repositoryFactory.transactionRepository }
    private val authService = AuthService.getInstance()

    // ===== Date Range Tracking =====
    private var currentStartDate: Long = 0L
    private var currentEndDate: Long = 0L

    // ===== Lifecycle Methods =====
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeActivityResultLaunchers()
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
            observeTransactions()

            Log.d(TAG, "onViewCreated: Setup complete.")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error initializing views", e)
            showToast("Error: Could not load dashboard components.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during onViewCreated setup", e)
            showToast("Error setting up dashboard: ${e.message}")
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: Cleaning up dialog reference.")
        currentDialog?.dismiss()
        currentDialog = null
        super.onDestroyView()
    }

    // ===== UI Initialization Methods =====
    private fun initializeViews(view: View) {
        try {
            transactionsRecyclerView = view.findViewById(R.id.transactionsRecyclerView)
            pieChart = view.findViewById(R.id.pieChart)
            periodSpinner = view.findViewById(R.id.periodSpinner)
            totalBudgetTextView = view.findViewById(R.id.totalBudgetTextView)
            totalSpentTextView = view.findViewById(R.id.totalSpentTextView)
            remainingTextView = view.findViewById(R.id.remainingTextView)
            balanceTextView = view.findViewById(R.id.balanceTextView)
            balanceIncomeButton = view.findViewById(R.id.balanceIncomeButton)
            balanceExpenseButton = view.findViewById(R.id.balanceExpenseButton)
            Log.d(TAG, "initializeViews: Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            throw IllegalStateException("Could not initialize essential views in DashboardFragment", e)
        }
    }

    private fun setupTransactionsRecyclerView() {
        Log.d(TAG, "setupTransactionsRecyclerView: Setting up...")
        transactionAdapter = TransactionAdapter()
        transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
        Log.d(TAG, "setupTransactionsRecyclerView: Setup complete.")
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
    }

    // ===== Chart Setup and Configuration =====
    private fun setupPieChart() {
        Log.d(TAG, "setupPieChart: Configuring pie chart.")
        pieChart.apply {
            description.isEnabled = false
            setExtraOffsets(5f, 5f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Total\nR 0.00"
            setCenterTextSize(16f)
            setCenterTextColor(Color.WHITE)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            legend.apply {
                isEnabled = true
                textColor = Color.WHITE
                textSize = 10f
                formSize = 8f
                form = Legend.LegendForm.CIRCLE
                verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                isWordWrapEnabled = true
                yOffset = 0f
                xOffset = 10f
            }

            setDrawEntryLabels(false)
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
            override fun onValueSelected(e: Entry?, h: Highlight?) {
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
        val periods = listOf("This Month", "Last Month", "This Year")

        context?.let { ctx ->
            val adapter = ArrayAdapter(ctx, R.layout.spinner_selected_item, periods)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            periodSpinner.adapter = adapter

            periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedPeriod = periods[position]
                    Log.i(TAG, "Period selected via spinner: $selectedPeriod")
                    val (startDate, endDate) = getDateRangeForPeriod(selectedPeriod)
                    if (startDate != null && endDate != null) {
                        currentStartDate = startDate
                        currentEndDate = endDate
                        Log.d(TAG, "setupPeriodSpinner: Fetching transactions for period: $selectedPeriod")
                        observeTransactions() 
                    } else {
                        currentStartDate = 0L
                        currentEndDate = 0L
                        clearUiData()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) { /* No action needed */ }
            }
            periodSpinner.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
            Log.d(TAG, "setupPeriodSpinner: Setup complete.")
        } ?: Log.e(TAG, "setupPeriodSpinner: Context is null, cannot setup spinner.")
    }

    // ===== Activity Result Launchers =====
    private fun initializeActivityResultLaunchers() {
        Log.d(TAG, "initializeActivityResultLaunchers: Setting up.")
        
        // Camera result launcher
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleCameraResult()
            } else {
                Log.w(TAG, "Camera Result NOT OK, Code: ${result.resultCode}")
                currentPhotoPath?.let { path -> File(path).delete() }
                currentPhotoPath = null
            }
        }
        
        // Gallery result launcher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleGalleryResult(result.data?.data)
            } else {
                Log.w(TAG, "Gallery Result NOT OK, Code: ${result.resultCode}")
            }
        }
        
        // Permission request launcher
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d(TAG, "Camera Permission Granted.")
                dispatchTakePictureIntent()
            } else {
                Log.w(TAG, "Camera Permission Denied.")
                showToast("Camera permission is required to take pictures")
            }
        }
    }
    
    private fun handleCameraResult() {
        currentPhotoPath?.let { path ->
            val file = File(path)
            if(file.exists() && file.length() > 0) {
                Log.d(TAG, "Camera Result OK, File exists: $path, Size: ${file.length()}")
                selectedImageUri = try {
                    FileProvider.getUriForFile(requireContext(), "com.example.notbroke.fileprovider", file)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Error getting URI for file: $path", e)
                    null
                }

                if (selectedImageUri != null) {
                    currentDialog?.findViewById<ImageView>(R.id.receiptImagePreview)?.let { imageView ->
                        loadBitmapIntoImageView(selectedImageUri, imageView)
                    } ?: Log.w(TAG, "Receipt preview ImageView not found in current dialog.")
                } else {
                    showToast("Failed to get image URI.")
                }
            } else {
                Log.e(TAG, "Camera Result OK, but file not found or empty at: $path")
                showToast("Failed to save picture")
                currentPhotoPath = null
            }
        } ?: run {
            Log.e(TAG, "Camera Result OK, but currentPhotoPath is null")
            showToast("Failed to get picture path")
        }
    }
    
    private fun handleGalleryResult(uri: Uri?) {
        uri?.let {
            Log.d(TAG, "Gallery Result OK, URI: $uri")
            selectedImageUri = it
            currentDialog?.findViewById<ImageView>(R.id.receiptImagePreview)?.let { imageView ->
                loadBitmapIntoImageView(selectedImageUri, imageView)
            } ?: Log.w(TAG, "Receipt preview ImageView not found in current dialog.")
        } ?: run {
            Log.w(TAG, "Gallery Result OK, but URI is null")
            showToast("Failed to get image from gallery")
        }
    }

    // ===== Image Handling Methods =====
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
                    currentPhotoPath = null
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

    // ===== Dialog Methods =====
    private fun showTransactionDialog(type: Transaction.Type) {
        Log.d(TAG, "showTransactionDialog: Showing dialog for type: ${type.name}")
        if (currentDialog?.isShowing == true) {
            Log.w(TAG, "showTransactionDialog: Dialog already showing.")
            return
        }

        val context = requireContext() ?: return

        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_add_transaction)
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
        val takePictureButton = dialog.findViewById<Button>(R.id.takePictureButton)
        val chooseImageButton = dialog.findViewById<Button>(R.id.chooseImageButton)
        val receiptImageView = dialog.findViewById<ImageView>(R.id.receiptImagePreview)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val addButton = dialog.findViewById<Button>(R.id.addButton)
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
        receiptImageView?.setImageResource(android.R.drawable.ic_menu_gallery)

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

            // Determine Category
            val category: String
            category = if (type == Transaction.Type.INCOME) {
                "Income"
            } else {
                CategorizationUtils.suggestCategory(description) ?: "Other"
            }
            Log.d(TAG,"Determined category: $category for type: ${type.name}, description: '$description'")

            val transaction = Transaction(
                type = type,
                amount = amount,
                description = description,
                category = category,
                date = date,
                receiptImageUri = selectedImageUri?.toString()
            )

            Log.d(TAG, "Attempting to save transaction: $transaction")
            addTransaction(transaction)

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

    private fun showEditTransactionDialog(transaction: Transaction) {
        Log.d(TAG, "showEditTransactionDialog: Showing dialog for transaction: ${transaction.firestoreId}")
        if (currentDialog?.isShowing == true) {
            Log.w(TAG, "showEditTransactionDialog: Dialog already showing.")
            return
        }

        val context = requireContext() ?: return

        val dialog = Dialog(context)
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
        val categoryAutoComplete = dialog.findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val deleteButton = dialog.findViewById<Button>(R.id.deleteButton)
        val saveButton = dialog.findViewById<Button>(R.id.saveButton)

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

        // Setup Category AutoCompleteTextView with categories
        var allCategories = if (transaction.type == Transaction.Type.INCOME) {
            CategorizationUtils.incomeCategories
        } else {
            CategorizationUtils.expenseCategories
        }
        // Move "Other" to the end if it exists
        allCategories = allCategories.toMutableList().apply {
            if (contains("Other")) {
                remove("Other")
                add("Other")
            }
        }
        val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, allCategories)

        categoryAutoComplete.setAdapter(categoryAdapter)
        categoryAutoComplete.threshold = 1
        categoryAutoComplete.setText(transaction.category, false)

        cancelButton.setOnClickListener {
            Log.d(TAG, "Edit Transaction Dialog: Cancel clicked.")
            dialog.dismiss()
            currentDialog = null
        }

        deleteButton.setOnClickListener {
            Log.d(TAG, "Edit Transaction Dialog: Delete clicked for ID: ${transaction.firestoreId}")
            deleteTransaction(transaction)
            dialog.dismiss()
            currentDialog = null
        }

        saveButton.setOnClickListener {
            Log.d(TAG, "Edit Transaction Dialog: Save clicked for ID: ${transaction.firestoreId}")
            val amountStr = amountEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()
            val selectedCategory = categoryAutoComplete.text.toString().trim()

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
            if (!allCategories.contains(selectedCategory) && selectedCategory != "Uncategorized") {
                Log.w(TAG, "Selected category '$selectedCategory' not in known categories.")
            }

            // Create an updated transaction object
            val updatedTransaction = transaction.copy(
                amount = amount,
                description = description,
                category = selectedCategory
            )

            updateTransaction(updatedTransaction)

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

    // ===== TransactionAdapter.OnItemClickListener Implementation =====
    override fun onItemClick(transaction: Transaction) {
        Log.d(TAG, "Transaction item clicked: ${transaction.description}")
        lifecycleScope.launch {
            CategorizationUtils.loadCategoriesFromDatabase(requireContext(),authService.getCurrentUserId())
            showEditTransactionDialog(transaction)
        }
    }

    // ===== Transaction Data Methods =====
    private fun addTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                transactionRepository.saveTransaction(transaction, authService.getCurrentUserId())
                Toast.makeText(context, "Transaction added successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to add transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error adding transaction", e)
            }
        }
    }

    private fun updateTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                transactionRepository.updateTransaction(transaction, authService.getCurrentUserId())
                Toast.makeText(context, "Transaction updated successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error updating transaction", e)
            }
        }
    }

    private fun deleteTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction, authService.getCurrentUserId())
                Toast.makeText(context, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error deleting transaction", e)
            }
        }
    }

    // ===== UI Update Methods =====
    private fun updateBalance(transactions: List<Transaction>) {
        var balance = 0.0
        transactions.forEach { transaction ->
            if (transaction.type == Transaction.Type.INCOME) {
                balance += transaction.amount
            } else if (transaction.type == Transaction.Type.EXPENSE) {
                balance -= transaction.amount
            }
        }
        context?.let {
            balanceTextView.text = String.format(Locale.getDefault(), "R %.2f", balance)
            Log.d(TAG, "updateBalance: Balance updated to R ${"%.2f".format(balance)}")
        } ?: Log.w(TAG, "updateBalance: Context is null, cannot format currency.")
    }

    private fun updatePieChart(transactions: List<Transaction>) {
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
            setDrawValues(true)
            yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(pieChart)
        }

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.setUsePercentValues(true)

        // Update center text
        val totalSpent = positiveEntries.values.sum()
        pieChart.centerText = String.format(Locale.getDefault(), "Total\nR %.2f", totalSpent)

        // Refresh chart
        pieChart.animateY(1000)
        pieChart.invalidate()
        Log.d(TAG, "updatePieChart: Chart updated and refreshed.")
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

    private fun clearUiData() {
        Log.d(TAG, "clearUiData: Clearing transaction list, chart, and balance.")
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            transactionAdapter.submitList(emptyList())
            updatePieChart(emptyList())
            updateBalance(emptyList())
            totalSpentTextView.text = "R 0.00"
            totalBudgetTextView.text = "R ----.--"
            remainingTextView.text = "R ----.--"
        }
    }

    // ===== Date Range Methods =====
    private fun getDateRangeForPeriod(period: String): Pair<Long?, Long?> {
        val calendar = Calendar.getInstance()
        var startDateMillis: Long?
        var endDateMillis: Long?

        try {
            // Log the current system time
            val currentTime = System.currentTimeMillis()
            Log.d(TAG, "Current system time: ${Date(currentTime)}")
            Log.d(TAG, "Calendar instance time: ${Date(calendar.timeInMillis)}")

            when (period) {
                "This Month" -> {
                    // Set to first day of current month
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    setCalendarToStartOfDay(calendar)
                    startDateMillis = calendar.timeInMillis
                    Log.d(TAG, "Start of month: ${Date(startDateMillis)}")

                    // Calculate end of month
                    calendar.add(Calendar.MONTH, 1)
                    setCalendarToStartOfDay(calendar)
                    calendar.add(Calendar.MILLISECOND, -1)
                    endDateMillis = calendar.timeInMillis
                    Log.d(TAG, "End of month: ${Date(endDateMillis)}")
                }
                "Last Month" -> {
                    // Go to start of this month
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    setCalendarToStartOfDay(calendar)
                    // Subtract 1ms to get end of last month
                    calendar.add(Calendar.MILLISECOND, -1)
                    endDateMillis = calendar.timeInMillis
                    Log.d(TAG, "End of last month: ${Date(endDateMillis)}")

                    // Calendar is now at the end of last month. Set to start of last month.
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    setCalendarToStartOfDay(calendar)
                    calendar.add(Calendar.MONTH, -1)
                    startDateMillis = calendar.timeInMillis
                    Log.d(TAG, "Start of last month: ${Date(startDateMillis)}")
                }
                "This Year" -> {
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    setCalendarToStartOfDay(calendar)
                    startDateMillis = calendar.timeInMillis
                    Log.d(TAG, "Start of year: ${Date(startDateMillis)}")

                    // End of year calculation
                    calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                    calendar.set(Calendar.DAY_OF_MONTH, 31)
                    setCalendarToEndOfDay(calendar)
                    endDateMillis = calendar.timeInMillis
                    Log.d(TAG, "End of year: ${Date(endDateMillis)}")
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
            return Pair(startDateMillis, endDateMillis)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating date range for '$period'", e)
            return Pair(null, null)
        }
    }

    private fun setCalendarToStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private fun setCalendarToEndOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
    }

    // ===== Helper Methods =====
    private fun getChartColors(categories: List<String>): List<Int> {
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
            colors.add(predefinedColors.getOrElse(index % predefinedColors.size) {
                val hue = (category.hashCode() % 360).toFloat()
                Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.8f))
            })
        }
        return colors
    }

    private fun navigateToCategoryDetails(categoryName: String) {
        if (currentStartDate == 0L || currentEndDate == 0L) {
            Log.w(TAG, "navigateToCategoryDetails: Date range not set, cannot navigate.")
            showToast("Error: Date range not available")
            return
        }
        Log.i(TAG,"Attempting navigation to details for category: '$categoryName' (Period: ${Date(currentStartDate)} - ${Date(currentEndDate)})")
        showToast("Navigate for: $categoryName")
    }

    private fun showToast(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    // ===== Data Observation Methods =====
    private fun observeTransactions() {
        val userId = try {
            authService.getCurrentUserId()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "User not authenticated", e)
            showToast("Please sign in to view transactions")
            return
        }

        Log.d(TAG, "Starting transaction observation for user: $userId")
        lifecycleScope.launch {
            try {
                // If no specific date range is set, default to "This Month" initially
                if (currentStartDate == 0L && currentEndDate == 0L) {
                    val (defaultStart, defaultEnd) = getDateRangeForPeriod("This Month")
                    if (defaultStart != null && defaultEnd != null) {
                        currentStartDate = defaultStart
                        currentEndDate = defaultEnd
                        Log.d(TAG, "Applying default 'This Month' range: ${Date(currentStartDate)} to ${Date(currentEndDate)}")
                    }
                }

                // Only observe transactions if we have a valid date range
                if (currentStartDate > 0 && currentEndDate > 0) {
                    transactionRepository.getTransactionsByDateRange(currentStartDate, currentEndDate, userId)
                        .collectLatest { transactions: List<Transaction> ->
                            Log.d(TAG, "Observed ${transactions.size} transactions for period ${Date(currentStartDate)} to ${Date(currentEndDate)}")
                            
                            // Update adapter with transactions
                            transactionAdapter.submitList(transactions)

                            // Update UI with transaction data
                            updateTransactionSummary(transactions)
                            updatePieChart(transactions)
                            Log.d(TAG, "UI updated with ${transactions.size} transactions")
                        }
                } else {
                    Log.w(TAG, "Invalid date range, clearing UI data")
                    clearUiData()
                }
            } catch (e: CancellationException) {
                Log.i(TAG, "Transaction observation coroutine cancelled (likely due to lifecycle)")
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting transactions flow", e)
                showToast("Error loading transactions: ${e.message}")
            }
        }
    }
}