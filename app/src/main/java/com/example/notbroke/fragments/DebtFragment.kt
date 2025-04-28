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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.notbroke.R
import com.example.notbroke.models.Debt
import com.example.notbroke.models.DebtStrategy
import com.example.notbroke.models.DebtStrategyType
import com.example.notbroke.models.UserPreferences
import com.example.notbroke.services.AuthService
import com.example.notbroke.repositories.RepositoryFactory
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
    private lateinit var repositoryFactory: RepositoryFactory
    private val debtRepository by lazy { repositoryFactory.debtRepository }
    private val userPreferencesRepository by lazy { repositoryFactory.userPreferencesRepository }
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
        
        // Initialize repository factory
        repositoryFactory = RepositoryFactory.getInstance(requireContext())
        
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
        debtsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        debtsRecyclerView.adapter = debtAdapter
    }
    
    private fun setupStrategySpinner() {
        val strategies = DebtStrategyType.values().map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, strategies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        strategySpinner.adapter = adapter
        
        strategySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStrategy = DebtStrategyType.values()[position]
                updateStrategy(selectedStrategy)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupAddDebtButton() {
        addDebtButton.setOnClickListener { showAddDebtDialog() }
        addDebtFab.setOnClickListener { showAddDebtDialog() }
    }
    
    private fun loadUserPreferences() {
        val userId = authService.getCurrentUserId() ?: return
        
        lifecycleScope.launch {
            try {
                userPreferencesRepository.observePreferences(userId).collectLatest { preferences ->
                    preferences?.let {
                        currentStrategy = it.selectedDebtStrategy
                        val spinnerPosition = DebtStrategyType.values().indexOf(currentStrategy)
                        if (spinnerPosition >= 0) {
                            strategySpinner.setSelection(spinnerPosition)
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading preferences: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeDebts() {
        val userId = authService.getCurrentUserId() ?: return
        
        lifecycleScope.launch {
            try {
                debtRepository.getAllDebts(userId).collectLatest { debtList ->
                    debts.clear()
                    debts.addAll(debtList)
                    
                    if (debts.isEmpty()) {
                        noDebtsTextView.visibility = View.VISIBLE
                        debtsRecyclerView.visibility = View.GONE
                    } else {
                        noDebtsTextView.visibility = View.GONE
                        debtsRecyclerView.visibility = View.VISIBLE
                        debtAdapter.submitList(debts)
                    }
                    
                    updateDebtSummary()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading debts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateDebtSummary() {
        if (debts.isEmpty()) {
            progressBar.progress = 0
            progressPercentageTextView.text = "0%"
            dateEstimateTextView.text = "No debts"
            totalDebtTextView.text = currencyFormatter.format(0.0)
            paidOffTextView.text = currencyFormatter.format(0.0)
            monthlyPaymentTextView.text = currencyFormatter.format(0.0)
            return
        }
        
        val totalDebt = debts.sumOf { it.totalAmount }
        val totalPaid = debts.sumOf { it.amountPaid }
        val progressPercentage = (totalPaid / totalDebt * 100).toInt()
        
        progressBar.progress = progressPercentage
        progressPercentageTextView.text = "$progressPercentage%"
        totalDebtTextView.text = currencyFormatter.format(totalDebt)
        paidOffTextView.text = currencyFormatter.format(totalPaid)
        
        val monthlyPayment = debts.sumOf { it.monthlyPayment }
        monthlyPaymentTextView.text = currencyFormatter.format(monthlyPayment)
        
        // Calculate estimated payoff date based on current strategy
        val estimatedDate = debtStrategy.calculateEstimatedPayoffDate(debts, currentStrategy)
        dateEstimateTextView.text = "Estimated: ${dateFormatter.format(estimatedDate)}"
    }
    
    private fun updateStrategy(strategy: DebtStrategyType) {
        currentStrategy = strategy
        val userId = authService.getCurrentUserId() ?: return
        
        lifecycleScope.launch {
            try {
                val preferences = UserPreferences(userId, strategy)
                userPreferencesRepository.savePreferences(preferences)
                
                // Update strategy description
                strategyDescriptionTextView.text = when (strategy) {
                    DebtStrategyType.AVALANCHE -> "Pay off debts with the highest interest rates first"
                    DebtStrategyType.SNOWBALL -> "Pay off debts with the smallest balances first"
                    DebtStrategyType.DEBT_CONSOLIDATION -> "Combine multiple debts into a single loan with a lower interest rate"
                    DebtStrategyType.HIGHEST_INTEREST_FIRST -> "Similar to Avalanche, focus on highest interest debts first"
                    DebtStrategyType.BALANCE_PROPORTION -> "Distribute extra payments proportionally based on remaining balances"
                    DebtStrategyType.DEBT_STACKING -> "Stack payments on one debt at a time until it's paid off"
                }
                
                // Recalculate estimated payoff date
                updateDebtSummary()
            } catch (e: Exception) {
                Toast.makeText(context, "Error updating strategy: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAddDebtDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_debt, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.debtNameEditText)
        val amountEditText = dialogView.findViewById<TextInputEditText>(R.id.debtAmountEditText)
        val interestRateEditText = dialogView.findViewById<TextInputEditText>(R.id.interestRateEditText)
        val monthlyPaymentEditText = dialogView.findViewById<TextInputEditText>(R.id.monthlyPaymentEditText)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Debt")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val name = nameEditText.text.toString()
                val amount = amountEditText.text.toString().toDoubleOrNull()
                val interestRate = interestRateEditText.text.toString().toDoubleOrNull()
                val monthlyPayment = monthlyPaymentEditText.text.toString().toDoubleOrNull()
                
                if (name.isBlank() || amount == null || interestRate == null || monthlyPayment == null) {
                    Toast.makeText(context, "Please fill all fields with valid values", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                addDebt(name, amount, interestRate, monthlyPayment)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addDebt(name: String, amount: Double, interestRate: Double, monthlyPayment: Double) {
        val userId = authService.getCurrentUserId() ?: return
        val debt = Debt(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            name = name,
            totalAmount = amount,
            amountPaid = 0.0,
            interestRate = interestRate,
            monthlyPayment = monthlyPayment,
            creationDate = System.currentTimeMillis(),
            lastPaymentDate = null
        )
        
        lifecycleScope.launch {
            try {
                val result = debtRepository.addDebt(debt)
                result.onSuccess {
                    Toast.makeText(context, "Debt added successfully", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Failed to add debt: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteDebt(debt: Debt) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Debt")
            .setMessage("Are you sure you want to delete this debt?")
            .setPositiveButton("Delete") { dialog, _ ->
                lifecycleScope.launch {
                    try {
                        val result = debtRepository.deleteDebt(debt.id)
                        result.onSuccess {
                            Toast.makeText(context, "Debt deleted successfully", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, "Failed to delete debt: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showPaymentDialog(debt: Debt) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_make_payment, null)
        val paymentAmountEditText = dialogView.findViewById<TextInputEditText>(R.id.paymentAmountEditText)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Make Payment")
            .setView(dialogView)
            .setPositiveButton("Pay") { dialog, _ ->
                val paymentAmount = paymentAmountEditText.text.toString().toDoubleOrNull()
                
                if (paymentAmount == null || paymentAmount <= 0) {
                    Toast.makeText(context, "Please enter a valid payment amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                makePayment(debt, paymentAmount)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun makePayment(debt: Debt, paymentAmount: Double) {
        val updatedDebt = debt.copy(
            amountPaid = debt.amountPaid + paymentAmount,
            lastPaymentDate = System.currentTimeMillis()
        )
        
        lifecycleScope.launch {
            try {
                val result = debtRepository.updateDebt(updatedDebt)
                result.onSuccess {
                    Toast.makeText(context, "Payment recorded successfully", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Failed to record payment: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance() = DebtFragment()
    }
} 
