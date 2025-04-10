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
import com.github.mikephil.charting.data.PieEntry
import android.content.res.ColorStateList
import com.example.notbroke.R
import com.example.notbroke.TestData
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.app.Dialog
import com.google.android.material.button.MaterialButton
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.ImageView

class DashboardFragment : Fragment() {
    private val TAG = "DashboardFragment"
    
    // Views
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var pieChart: PieChart
    private lateinit var periodSpinner: Spinner
    private lateinit var totalBudgetTextView: TextView
    private lateinit var totalSpentTextView: TextView
    private lateinit var remainingTextView: TextView
    private lateinit var balanceTextView: TextView
    //private lateinit var addIncomeButton: FloatingActionButton
    //private lateinit var addExpenseButton: FloatingActionButton
    private lateinit var balanceIncomeButton: MaterialButton
    private lateinit var balanceExpenseButton: MaterialButton

    // Receipt image handling
    private var currentPhotoPath: String? = null
    private var selectedImageUri: Uri? = null
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    
    // Reference to the current dialog
    private var currentDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Activity Result Launchers
        initializeActivityResultLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Inflating dashboard fragment")
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up dashboard fragment views")
        
        try {
            // Initialize views
            initializeViews(view)
            
            // Setup FAB click listeners
            setupFabListeners()
            
            // Setup RecyclerView for transactions
            setupTransactionsRecyclerView()
            
            // Load sample transactions
            loadSampleTransactions()
            
            // Setup budget components
            setupPieChart()
            setupPeriodSpinner()
            loadBudgetData()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up dashboard fragment", e)
            showToast("Error: ${e.message}")
        }
    }
    
    private fun initializeViews(view: View) {
        try {
            // Try to find views in the fragment layout
            pieChart = view.findViewById(R.id.pieChart)
            periodSpinner = view.findViewById(R.id.periodSpinner)
            totalBudgetTextView = view.findViewById(R.id.totalBudgetTextView)
            totalSpentTextView = view.findViewById(R.id.totalSpentTextView)
            remainingTextView = view.findViewById(R.id.remainingTextView)
            balanceTextView = view.findViewById(R.id.balanceTextView)
            transactionsRecyclerView = view.findViewById(R.id.transactionsRecyclerView)
            //addIncomeButton = view.findViewById(R.id.addIncomeButton)
            //addExpenseButton = view.findViewById(R.id.addExpenseButton)
            balanceIncomeButton = view.findViewById(R.id.balanceIncomeButton)
            balanceExpenseButton = view.findViewById(R.id.balanceExpenseButton)
            
            Log.d(TAG, "Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            throw e
        }
    }
    
    private fun setupTransactionsRecyclerView() {
        try {
            Log.d(TAG, "Setting up transactions RecyclerView")
            
            // Create and set the adapter
            transactionAdapter = TransactionAdapter()
            
            // Configure the RecyclerView
            transactionsRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = transactionAdapter
            }
            
            Log.d(TAG, "Transactions RecyclerView setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up transactions RecyclerView", e)
            showToast("Error setting up transactions: ${e.message}")
        }
    }

    private fun loadSampleTransactions() {
        try {
            Log.d(TAG, "Starting to load sample transactions from TestData")
            
            // Get transactions from TestData utility class
            val transactions = TestData.getSampleTransactions()
            
            // Check if we have transactions
            if (transactions.isEmpty()) {
                Log.w(TAG, "Warning: No sample transactions were created")
            } else {
                Log.d(TAG, "Loaded ${transactions.size} sample transactions")
            }
            
            // Display transactions - check for null adapter
            if (::transactionAdapter.isInitialized) {
                transactionAdapter.setTransactions(transactions)
                Log.d(TAG, "Set transactions on adapter")
            } else {
                Log.e(TAG, "TransactionAdapter not initialized!")
            }
            
            // Update balance
            updateBalance(transactions)
            
            Log.d(TAG, "Sample transactions loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sample transactions", e)
            showToast("Could not load transaction data: ${e.message}")
        }
    }
    
    private fun updateBalance(transactions: List<Transaction>) {
        var balance = 0.0
        for (transaction in transactions) {
            if (transaction.type == Transaction.Type.INCOME) {
                balance += transaction.amount
            } else {
                balance -= transaction.amount
            }
        }
        
        balanceTextView.text = "R%.2f".format(balance)
    }

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setExtraOffsets(20f, 20f, 60f, 20f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 35f
            transparentCircleRadius = 38f
            setDrawCenterText(false)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            
            // Set chart animation
            animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
            
            // Configure legend
            legend.apply {
                isEnabled = true
                textColor = Color.WHITE
                textSize = 14f
                xEntrySpace = 8f
                yEntrySpace = 6f
                formSize = 10f
                form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.CENTER
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                yOffset = 0f
                xOffset = 10f
            }
            
            setDrawEntryLabels(false)
            minOffset = 15f
        }
    }
    
    private fun setupPeriodSpinner() {
        val periods = arrayOf("This Month", "Last Month", "This Year", "Custom")
        
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periods
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(Color.parseColor("#FFD700"))
                    textSize = 16f
                    setPadding(8, 8, 8, 8)
                }
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    setPadding(16, 16, 16, 16)
                    setBackgroundColor(Color.parseColor("#1E1E1E"))
                }
                return view
            }
        }
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = adapter
        
        // Set the background tint of the spinner
        periodSpinner.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
    }
    
    private fun loadBudgetData() {
        try {
            Log.d(TAG, "Loading budget data")
            // Create sample data directly here for testing
            val budgetCategories = listOf(
                BudgetCategory("Rent", 5000.0, 4000.0),
                BudgetCategory("Groceries", 2000.0, 1500.0),
                BudgetCategory("Transport", 1000.0, 800.0),
                BudgetCategory("Entertainment", 1500.0, 1200.0),
                BudgetCategory("Utilities", 1200.0, 1000.0)
            )
            
            // Calculate totals
            var totalBudget = 0.0
            var totalSpent = 0.0
            
            for (category in budgetCategories) {
                totalBudget += category.budgetAmount
                totalSpent += category.spentAmount
            }
            
            val remaining = totalBudget - totalSpent
            
            // Update UI
            totalBudgetTextView.text = "R%.2f".format(totalBudget)
            totalSpentTextView.text = "R%.2f".format(totalSpent)
            remainingTextView.text = "R%.2f".format(remaining)
            
            // Update pie chart with the categories
            updatePieChart(budgetCategories)
            Log.d(TAG, "Budget data loaded and chart updated")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading budget data", e)
            e.printStackTrace()
        }
    }
    
    private fun updatePieChart(categories: List<BudgetCategory>) {
        try {
            Log.d(TAG, "Updating pie chart with ${categories.size} categories")
            val entries = ArrayList<PieEntry>()
            val colors = ArrayList<Int>()
            
            // Add entries for each category with spent amount
            categories.forEach { category ->
                Log.d(TAG, "Processing category: ${category.name}, spent: ${category.spentAmount}")
                if (category.spentAmount > 0) {
                    entries.add(PieEntry(category.spentAmount.toFloat(), category.name))
                    colors.add(when (category.name.lowercase()) {
                        "rent" -> Color.parseColor("#F44336")
                        "groceries" -> Color.parseColor("#4CAF50")
                        "transport" -> Color.parseColor("#2196F3")
                        "entertainment" -> Color.parseColor("#E91E63")
                        "utilities" -> Color.parseColor("#9C27B0")
                        else -> Color.parseColor("#607D8B")
                    })
                }
            }
            
            Log.d(TAG, "Created ${entries.size} pie entries")
            
            if (entries.isNotEmpty()) {
                val dataSet = PieDataSet(entries, "")
                dataSet.apply {
                    this.colors = colors
                    setDrawValues(false)
                    sliceSpace = 1f
                    selectionShift = 2f
                }
                
                val data = PieData(dataSet)
                pieChart.data = data
                
                // Force layout and refresh
                pieChart.post {
                    pieChart.invalidate()
                    Log.d(TAG, "Pie chart refreshed")
                }
            } else {
                Log.w(TAG, "No entries for pie chart")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating pie chart", e)
            e.printStackTrace()
        }
    }

    private fun setupFabListeners() {
        // addIncomeButton.setOnClickListener {
        //     showTransactionDialog(Transaction.Type.INCOME)
        // }
        
        // addExpenseButton.setOnClickListener {
        //     showTransactionDialog(Transaction.Type.EXPENSE)
        // }
        
        balanceIncomeButton.setOnClickListener {
            showTransactionDialog(Transaction.Type.INCOME)
        }
        
        balanceExpenseButton.setOnClickListener {
            showTransactionDialog(Transaction.Type.EXPENSE)
        }
    }

    private fun showTransactionDialog(type: Transaction.Type) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_transaction)
        
        // Store reference to current dialog
        currentDialog = dialog
        
        // Set dialog width to match parent with margins
        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        
        // Initialize dialog views
        val titleTextView = dialog.findViewById<TextView>(R.id.dialogTitleTextView)
        val amountEditText = dialog.findViewById<TextInputEditText>(R.id.amountEditText)
        val descriptionEditText = dialog.findViewById<TextInputEditText>(R.id.descriptionEditText)
        val categoryAutoComplete = dialog.findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete)
        val takePictureButton = dialog.findViewById<Button>(R.id.takePictureButton)
        val chooseImageButton = dialog.findViewById<Button>(R.id.chooseImageButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val addButton = dialog.findViewById<Button>(R.id.addButton)
        
        // Reset image selection state
        selectedImageUri = null
        dialog.findViewById<ImageView>(R.id.receiptImagePreview)?.setImageResource(android.R.drawable.ic_menu_gallery)
        
        // Set dialog title based on type
        titleTextView.text = if (type == Transaction.Type.INCOME) "Add Income" else "Add Expense"
        
        // Setup category dropdown
        val categories = if (type == Transaction.Type.INCOME) {
            arrayOf("Salary", "Investments", "Side Hustle", "Gift", "Other")
        } else {
            arrayOf("Rent", "Groceries", "Transport", "Entertainment", "Utilities", "Other")
        }
        
        // Create a custom adapter with dark background and light text
        val categoryAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#1E1E1E"))
                }
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#1E1E1E"))
                    setPadding(16, 16, 16, 16)
                }
                return view
            }
        }
        
        categoryAutoComplete.setAdapter(categoryAdapter)
        
        // Set dropdown background
        categoryAutoComplete.setDropDownBackgroundResource(android.R.color.background_dark)
        
        // Setup image capture and gallery buttons
        takePictureButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                dispatchTakePictureIntent()
            } else {
                // Request camera permission
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        
        chooseImageButton.setOnClickListener {
            openGallery()
        }
        
        // Setup button click listeners for cancel/add
        cancelButton.setOnClickListener {
            dialog.dismiss()
            currentDialog = null
        }
        
        addButton.setOnClickListener {
            val amount = amountEditText.text.toString().toDoubleOrNull()
            val description = descriptionEditText.text.toString()
            val category = categoryAutoComplete.text.toString()
            
            if (amount == null || amount <= 0) {
                showToast("Please enter a valid amount")
                return@setOnClickListener
            }
            
            if (description.isBlank()) {
                showToast("Please enter a description")
                return@setOnClickListener
            }
            
            if (category.isBlank()) {
                showToast("Please select a category")
                return@setOnClickListener
            }
            
            // Create and add the new transaction
            val transaction = Transaction(
                id = System.currentTimeMillis(),
                type = type,
                amount = amount, // Always store positive amount
                description = description,
                category = category,
                date = System.currentTimeMillis(),
                receiptImageUri = selectedImageUri?.toString()
            )
            
            // Add to adapter
            val currentTransactions = transactionAdapter.getTransactions().toMutableList()
            currentTransactions.add(0, transaction)
            transactionAdapter.setTransactions(currentTransactions)
            
            // Update balance
            updateBalance(currentTransactions)
            
            // Close dialog
            dialog.dismiss()
            currentDialog = null
            
            // Show success message
            showToast("${if (type == Transaction.Type.INCOME) "Income" else "Expense"} added successfully")
        }
        
        dialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance(): DashboardFragment {
            Log.d("DashboardFragment", "Creating new instance of DashboardFragment")
            return DashboardFragment()
        }
    }
    
    // BudgetCategory data class
    data class BudgetCategory(
        val name: String,
        val budgetAmount: Double,
        val spentAmount: Double
    )

    override fun onDestroyView() {
        super.onDestroyView()
        currentDialog = null
    }

    private fun initializeActivityResultLaunchers() {
        // Camera result launcher
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                currentPhotoPath?.let { path ->
                    val bitmap = BitmapFactory.decodeFile(path)
                    currentDialog?.findViewById<ImageView>(R.id.receiptImagePreview)?.let { imageView ->
                        imageView.setImageBitmap(bitmap)
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    selectedImageUri = Uri.fromFile(File(path))
                }
            }
        }
        
        // Gallery result launcher
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        currentDialog?.findViewById<ImageView>(R.id.receiptImagePreview)?.let { imageView ->
                            imageView.setImageBitmap(bitmap)
                            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        selectedImageUri = uri
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading image from gallery", e)
                        showToast("Failed to load image")
                    }
                }
            }
        }
        
        // Permission request launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission granted, proceed with camera or gallery
                dispatchTakePictureIntent()
            } else {
                // Permission denied
                showToast("Camera permission is required to take pictures")
            }
        }
    }
    
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Error creating image file", ex)
                    showToast("Error creating image file")
                    null
                }
                
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.notbroke.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureLauncher.launch(takePictureIntent)
                }
            } ?: run {
                showToast("No camera app found")
            }
        }
    }
    
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }
    
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
} 