package com.example.notbroke.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.notbroke.R
import com.example.notbroke.models.Debt
import com.example.notbroke.models.DebtStrategy
import com.example.notbroke.models.DebtStrategyType
import com.example.notbroke.models.UserPreferences
import com.example.notbroke.services.AuthService
import com.example.notbroke.services.FirestoreService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import android.widget.AdapterView
import com.example.notbroke.adapters.DebtAdapter
import com.example.notbroke.databinding.FragmentDebtBinding

class DebtFragment : Fragment() {
    private var _binding: FragmentDebtBinding? = null
    private val binding get() = _binding!!
    
    // Views
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentageTextView: TextView
    private lateinit var dateEstimateTextView: TextView
    private lateinit var totalDebtTextView: TextView
    private lateinit var paidOffTextView: TextView
    private lateinit var monthlyPaymentTextView: TextView
    private lateinit var debtsRecyclerView: RecyclerView
    private lateinit var noDebtsTextView: TextView
    private lateinit var addDebtButton: Button
    private lateinit var addDebtFab: FloatingActionButton
    private lateinit var strategySpinner: Spinner
    private lateinit var strategyDescriptionTextView: TextView
    private lateinit var loadingIndicator: ProgressBar
    
    // Data
    private var debts: MutableList<Debt> = mutableListOf()
    private lateinit var debtAdapter: DebtAdapter
    private val firestoreService = FirestoreService.getInstance()
    private val authService = AuthService.getInstance()
    private val debtStrategy = DebtStrategy()
    private var currentStrategy: DebtStrategyType = DebtStrategyType.AVALANCHE
    
    // Formatting
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebtBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        initializeViews(view)
        
        // Setup UI components
        setupRecyclerView()
        setupStrategySpinner()
        setupAddDebtButton()
        loadUserPreferences()
        observeDebts()
    }
    
    private fun initializeViews(view: View) {
        progressBar = view.findViewById(R.id.debtProgressBar)
        progressPercentageTextView = view.findViewById(R.id.progressPercentageTextView)
        dateEstimateTextView = view.findViewById(R.id.dateEstimateTextView)
        totalDebtTextView = view.findViewById(R.id.totalDebtTextView)
        paidOffTextView = view.findViewById(R.id.paidOffTextView)
        monthlyPaymentTextView = view.findViewById(R.id.monthlyPaymentTextView)
        debtsRecyclerView = view.findViewById(R.id.debtsRecyclerView)
        noDebtsTextView = view.findViewById(R.id.noDebtsTextView)
        addDebtButton = view.findViewById(R.id.addDebtButton)
        addDebtFab = view.findViewById(R.id.addDebtFab)
        strategySpinner = view.findViewById(R.id.strategySpinner)
        strategyDescriptionTextView = view.findViewById(R.id.strategyDescriptionTextView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
    }
    
    private fun setupRecyclerView() {
        debtAdapter = DebtAdapter(
            onDeleteClick = { debt -> deleteDebt(debt) },
            onPaymentClick = { debt -> showPaymentDialog(debt) }
        )
        debtsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = debtAdapter
        }
    }
    
    private fun setupStrategySpinner() {
        val strategies = DebtStrategyType.values().map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            strategies
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        strategySpinner.adapter = adapter
        strategySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStrategy = DebtStrategyType.values()[position]
                if (selectedStrategy != currentStrategy) {
                    currentStrategy = selectedStrategy
                    updateStrategyDescription()
                    applyCurrentStrategy()
                    saveUserPreferences()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupAddDebtButton() {
        addDebtButton.setOnClickListener {
            showAddDebtDialog()
        }
        
        addDebtFab.setOnClickListener {
            showAddDebtDialog()
        }
    }
    
    private fun loadUserPreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = authService.getCurrentUserId()
            firestoreService.getUserPreferences(userId).onSuccess { preferences ->
                currentStrategy = preferences.selectedDebtStrategy
                strategySpinner.setSelection(currentStrategy.ordinal)
                updateStrategyDescription()
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Failed to load preferences: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun saveUserPreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = authService.getCurrentUserId()
            val preferences = UserPreferences(
                userId = userId,
                selectedDebtStrategy = currentStrategy
            )
            
            firestoreService.updateUserPreferences(preferences).onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Failed to save preferences: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun observeDebts() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = authService.getCurrentUserId()
            firestoreService.observeDebts(userId).collectLatest { debts ->
                debtAdapter.submitList(debts)
                applyCurrentStrategy()
            }
        }
        
        // Observe debt statistics
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = authService.getCurrentUserId()
                firestoreService.observeDebtStatistics(userId).collectLatest { statistics ->
                    updateStatistics(statistics)
                }
            } catch (e: Exception) {
                showToast("Error loading statistics: ${e.message}")
            }
        }
    }
    
    private fun updateStatistics(statistics: FirestoreService.DebtStatistics) {
        // Update summary statistics
        totalDebtTextView.text = formatCurrency(statistics.totalDebt)
        paidOffTextView.text = formatCurrency(statistics.totalPaid)
        monthlyPaymentTextView.text = formatCurrency(statistics.totalMonthlyPayment)
        
        // Calculate overall progress percentage
        val overallProgress = if (statistics.totalDebt > 0) {
            ((statistics.totalPaid / statistics.totalDebt) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        
        // Update progress bar and percentage
        progressBar.progress = overallProgress
        progressPercentageTextView.text = "$overallProgress%"
        
        // Update visibility of empty state
        if (statistics.debtCount == 0) {
            noDebtsTextView.visibility = View.VISIBLE
            debtsRecyclerView.visibility = View.GONE
        } else {
            noDebtsTextView.visibility = View.GONE
            debtsRecyclerView.visibility = View.VISIBLE
        }
        
        // Calculate estimated debt-free date
        calculateDebtFreeDate()
    }
    
    private fun updateStrategyDescription() {
        val description = debtStrategy.getStrategyDescription(currentStrategy)
        strategyDescriptionTextView.text = description
        
        // Show consolidation savings if applicable
        if (currentStrategy == DebtStrategyType.DEBT_CONSOLIDATION && debts.isNotEmpty()) {
            val savings = debtStrategy.calculateConsolidationSavings(debts)
            showToast("Consolidation would save approximately ${formatCurrency(savings)} in interest")
        }
    }
    
    private fun applyCurrentStrategy() {
        val currentList = debtAdapter.currentList.toMutableList()
        val sortedList = debtStrategy.applyStrategy(currentList, currentStrategy)
        debtAdapter.submitList(sortedList)
    }
    
    private fun showAddDebtDialog() {
        // Create and configure the dialog
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_debt)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Initialize dialog views
        val debtNameEditText: TextInputEditText = dialog.findViewById(R.id.debtNameEditText)
        val debtAmountEditText: TextInputEditText = dialog.findViewById(R.id.debtAmountEditText)
        val interestRateEditText: TextInputEditText = dialog.findViewById(R.id.interestRateEditText)
        val monthlyPaymentEditText: TextInputEditText = dialog.findViewById(R.id.monthlyPaymentEditText)
        val amountPaidEditText: TextInputEditText = dialog.findViewById(R.id.amountPaidEditText)
        val cancelButton: MaterialButton = dialog.findViewById(R.id.cancelButton)
        val saveButton: MaterialButton = dialog.findViewById(R.id.saveButton)
        
        // Set up default values
        amountPaidEditText.setText("0")
        
        // Set up button click listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        saveButton.setOnClickListener {
            // Validate inputs
            val debtName = debtNameEditText.text.toString().trim()
            val debtAmountStr = debtAmountEditText.text.toString().trim()
            val interestRateStr = interestRateEditText.text.toString().trim()
            val monthlyPaymentStr = monthlyPaymentEditText.text.toString().trim()
            val amountPaidStr = amountPaidEditText.text.toString().trim()
            
            if (validateInputs(
                    debtName, 
                    debtAmountStr, 
                    interestRateStr, 
                    monthlyPaymentStr, 
                    amountPaidStr
                )
            ) {
                // Create new debt
                val debtAmount = debtAmountStr.toDouble()
                val interestRate = interestRateStr.toDouble()
                val monthlyPayment = monthlyPaymentStr.toDouble()
                val amountPaid = amountPaidStr.toDouble()
                
                // Ensure amount paid isn't greater than total
                val validAmountPaid = amountPaid.coerceAtMost(debtAmount)
                
                // Get current user ID from AuthService
                val userId = authService.getCurrentUserId()
                
                val newDebt = Debt(
                    userId = userId,
                    name = debtName,
                    totalAmount = debtAmount,
                    amountPaid = validAmountPaid,
                    interestRate = interestRate,
                    monthlyPayment = monthlyPayment
                )
                
                // Save to Firestore
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        // Show loading indicator
                        loadingIndicator.visibility = View.VISIBLE
                        
                        val result = firestoreService.createDebt(newDebt)
                        result.onSuccess { debtId ->
                            // Add to local list and update UI
                            debts.add(newDebt)
                            
                            // Apply current strategy
                            applyCurrentStrategy()
                            
                            debtAdapter.notifyItemInserted(debts.size - 1)
                            updateUI()
                            
                            // Show success message and dismiss dialog
                            showToast("Added debt: ${newDebt.name}")
                            dialog.dismiss()
                        }.onFailure { exception ->
                            // Log the full exception for debugging
                            android.util.Log.e("DebtFragment", "Error adding debt", exception)
                            
                            // Show detailed error message
                            val errorMessage = "Error adding debt: ${exception.message}\n" +
                                    "Cause: ${exception.cause?.message ?: "Unknown"}"
                            showToast(errorMessage)
                        }
                    } catch (e: Exception) {
                        // Log the full exception for debugging
                        android.util.Log.e("DebtFragment", "Unexpected error", e)
                        
                        // Show detailed error message
                        val errorMessage = "Unexpected error: ${e.message}\n" +
                                "Cause: ${e.cause?.message ?: "Unknown"}"
                        showToast(errorMessage)
                    } finally {
                        // Hide loading indicator
                        loadingIndicator.visibility = View.GONE
                    }
                }
            }
        }
        
        dialog.show()
    }
    
    private fun validateInputs(
        name: String,
        amount: String,
        interestRate: String,
        monthlyPayment: String,
        amountPaid: String
    ): Boolean {
        // Check for empty fields
        if (name.isEmpty()) {
            showToast("Please enter a debt name")
            return false
        }
        
        if (amount.isEmpty()) {
            showToast("Please enter the total debt amount")
            return false
        }
        
        if (interestRate.isEmpty()) {
            showToast("Please enter the interest rate")
            return false
        }
        
        if (monthlyPayment.isEmpty()) {
            showToast("Please enter the monthly payment")
            return false
        }
        
        // Validate numeric inputs
        try {
            val amountValue = amount.toDouble()
            val interestRateValue = interestRate.toDouble()
            val monthlyPaymentValue = monthlyPayment.toDouble()
            val amountPaidValue = if (amountPaid.isEmpty()) 0.0 else amountPaid.toDouble()
            
            // Additional validation
            if (amountValue <= 0) {
                showToast("Debt amount must be greater than zero")
                return false
            }
            
            if (interestRateValue < 0) {
                showToast("Interest rate cannot be negative")
                return false
            }
            
            if (monthlyPaymentValue <= 0) {
                showToast("Monthly payment must be greater than zero")
                return false
            }
            
            if (amountPaidValue < 0) {
                showToast("Amount paid cannot be negative")
                return false
            }
            
            if (amountPaidValue > amountValue) {
                showToast("Amount paid cannot exceed total debt amount")
                return false
            }
            
            // Monthly payment validation for reasonable debt repayment
            val monthlyInterest = interestRateValue / 100.0 / 12.0
            val remainingAmount = amountValue - amountPaidValue
            
            if (monthlyPaymentValue <= remainingAmount * monthlyInterest && interestRateValue > 0) {
                showToast("Monthly payment is too low to reduce the debt principal")
                return false
            }
            
        } catch (e: NumberFormatException) {
            showToast("Please enter valid numeric values")
            return false
        }
        
        return true
    }
    
    private fun updateUI() {
        if (debts.isEmpty()) {
            noDebtsTextView.visibility = View.VISIBLE
            debtsRecyclerView.visibility = View.GONE
        } else {
            noDebtsTextView.visibility = View.GONE
            debtsRecyclerView.visibility = View.VISIBLE
        }
        
        // Calculate summary stats
        val totalDebt = debts.sumOf { it.totalAmount }
        val totalPaid = debts.sumOf { it.amountPaid }
        val totalMonthlyPayment = debts.sumOf { it.monthlyPayment }
        
        // Calculate overall progress percentage
        val overallProgress = if (totalDebt > 0) {
            ((totalPaid / totalDebt) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        
        // Update progress bar and percentage
        progressBar.progress = overallProgress
        progressPercentageTextView.text = "$overallProgress%"
        
        // Update summary statistics
        totalDebtTextView.text = formatCurrency(totalDebt)
        paidOffTextView.text = formatCurrency(totalPaid)
        monthlyPaymentTextView.text = formatCurrency(totalMonthlyPayment)
        
        // Calculate estimated debt-free date
        calculateDebtFreeDate()
    }
    
    private fun calculateDebtFreeDate() {
        if (debts.isEmpty()) {
            dateEstimateTextView.text = "Add debts to see estimated completion date"
            return
        }
        
        // Find the debt that will take the longest to pay off
        val longestTimeDebt = debts.maxByOrNull { it.getMonthsRemaining() }
        
        if (longestTimeDebt != null) {
            val projectedDate = longestTimeDebt.getProjectedPayoffDate()
            dateEstimateTextView.text = "Estimated Debt-Free Date: ${formatDate(projectedDate)}"
        } else {
            dateEstimateTextView.text = "Estimated Debt-Free Date: N/A"
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }
    
    private fun formatDate(date: Date): String {
        return dateFormatter.format(date)
    }
    
    private fun showPaymentDialog(debt: Debt) {
        // Create and configure the dialog
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_debt_payment)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Initialize dialog views
        val debtNameTextView = dialog.findViewById<TextView>(R.id.debtNameTextView)
        val remainingBalanceTextView = dialog.findViewById<TextView>(R.id.remainingBalanceTextView)
        val paymentProgressBar = dialog.findViewById<ProgressBar>(R.id.paymentProgressBar)
        val paymentProgressTextView = dialog.findViewById<TextView>(R.id.paymentProgressTextView)
        val paymentAmountEditText = dialog.findViewById<TextInputEditText>(R.id.paymentAmountEditText)
        val minimumPaymentButton = dialog.findViewById<MaterialButton>(R.id.minimumPaymentButton)
        val regularPaymentButton = dialog.findViewById<MaterialButton>(R.id.regularPaymentButton)
        val fullPaymentButton = dialog.findViewById<MaterialButton>(R.id.fullPaymentButton)
        val cancelPaymentButton = dialog.findViewById<MaterialButton>(R.id.cancelPaymentButton)
        val makePaymentButton = dialog.findViewById<MaterialButton>(R.id.makePaymentButton)
        
        // Set up debt information
        debtNameTextView.text = debt.name
        val remainingBalance = debt.getRemainingBalance()
        remainingBalanceTextView.text = formatCurrency(remainingBalance)
        
        // Set up progress bar and text
        val progressPercentage = debt.getProgressPercentage()
        paymentProgressBar.progress = progressPercentage
        paymentProgressTextView.text = "${progressPercentage}% paid (${formatCurrency(debt.amountPaid)} / ${formatCurrency(debt.totalAmount)})"
        
        // Set up payment amount buttons
        minimumPaymentButton.setOnClickListener {
            paymentAmountEditText.setText(debt.monthlyPayment.toString())
        }
        
        regularPaymentButton.setOnClickListener {
            val regularPayment = debt.monthlyPayment * 1.5 // 50% more than minimum
            paymentAmountEditText.setText(regularPayment.toString())
        }
        
        fullPaymentButton.setOnClickListener {
            paymentAmountEditText.setText(remainingBalance.toString())
        }
        
        // Set up cancel button
        cancelPaymentButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Set up make payment button
        makePaymentButton.setOnClickListener {
            val paymentAmountStr = paymentAmountEditText.text.toString().trim()
            
            if (paymentAmountStr.isEmpty()) {
                showToast("Please enter a payment amount")
                return@setOnClickListener
            }
            
            val paymentAmount = paymentAmountStr.toDoubleOrNull()
            if (paymentAmount == null || paymentAmount <= 0) {
                showToast("Please enter a valid payment amount")
                return@setOnClickListener
            }
            
            if (paymentAmount > remainingBalance) {
                showToast("Payment amount cannot exceed remaining balance")
                return@setOnClickListener
            }
            
            // Show loading indicator
            loadingIndicator.visibility = View.VISIBLE
            
            // Make the payment
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Update the debt with the new payment
                    val updatedDebt = debt.copy(
                        amountPaid = debt.amountPaid + paymentAmount,
                        lastPaymentDate = System.currentTimeMillis()
                    )
                    
                    // Save to Firestore
                    firestoreService.updateDebt(updatedDebt.id, updatedDebt).onSuccess {
                        // Show success message and dismiss dialog
                        showToast("Payment of ${formatCurrency(paymentAmount)} made successfully")
                        dialog.dismiss()
                    }.onFailure { error ->
                        showToast("Failed to make payment: ${error.message}")
                    }
                } catch (e: Exception) {
                    showToast("Error making payment: ${e.message}")
                } finally {
                    // Hide loading indicator
                    loadingIndicator.visibility = View.GONE
                }
            }
        }
        
        dialog.show()
    }
    
    private fun deleteDebt(debt: Debt) {
        viewLifecycleOwner.lifecycleScope.launch {
            firestoreService.deleteDebt(debt.id).onFailure { error ->
                showToast("Failed to delete debt: ${error.message}")
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    companion object {
        fun newInstance(): DebtFragment {
            return DebtFragment()
        }
    }
} 
