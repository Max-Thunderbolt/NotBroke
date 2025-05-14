package com.example.notbroke.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log // Import Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notbroke.R
import com.example.notbroke.adapters.DebtAdapter
import com.example.notbroke.databinding.FragmentDebtBinding
import com.example.notbroke.models.*
import com.example.notbroke.repositories.RepositoryFactory
import com.example.notbroke.services.AuthService
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DebtFragment : Fragment() {

    private val TAG = "DebtFragment" // Add TAG for logging

    private var _binding: FragmentDebtBinding? = null
    private val binding get() = _binding!!

    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentageTextView: TextView
    private lateinit var dateEstimateTextView: TextView
    private lateinit var totalDebtTextView: TextView
    private lateinit var paidOffTextView: TextView
    private lateinit var monthlyPaymentTextView: TextView
    private lateinit var debtsRecyclerView: RecyclerView
    private lateinit var noDebtsTextView: TextView
    private lateinit var addDebtButton: Button
    private lateinit var strategySpinner: Spinner
    private lateinit var strategyDescriptionTextView: TextView
    private lateinit var loadingIndicator: ProgressBar

    private lateinit var repositoryFactory: RepositoryFactory
    private lateinit var debtAdapter: DebtAdapter
    private val debtRepository get() = repositoryFactory.debtRepository
    private val userPreferencesRepository get() = repositoryFactory.userPreferencesRepository
    private val authService = AuthService.getInstance()
    private val debtStrategy = DebtStrategy()
    private var currentStrategy: DebtStrategyType = DebtStrategyType.AVALANCHE

    private val debts = mutableListOf<Debt>()
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    private val dateFormatter = SimpleDateFormat("MMMM dd,yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDebtBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")
        repositoryFactory = RepositoryFactory.getInstance(requireContext())
        initializeViews(view)
        setupRecyclerView()
        setupStrategySpinner()
        setupAddDebtButton()
        loadUserPreferences()
        observeDebts() // Start observing debts
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
        strategySpinner = view.findViewById(R.id.strategySpinner)
        strategyDescriptionTextView = view.findViewById(R.id.strategyDescriptionTextView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        view.findViewById<Button>(R.id.makePaymentButton).setOnClickListener {
            showStrategyBasedPaymentDialog()
        }
    }

    private fun setupRecyclerView() {
        debtAdapter = DebtAdapter(
            onDeleteClick = { deleteDebt(it) },
            onDebtClick = { showPaymentDialog(it) }
        )
        debtsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        debtsRecyclerView.adapter = debtAdapter
    }

    private fun setupStrategySpinner() {
        val strategies = DebtStrategyType.values().map { it.name.replace("_", " ") }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, strategies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        strategySpinner.adapter = adapter

        strategySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateStrategy(DebtStrategyType.values()[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupAddDebtButton() {
        addDebtButton.setOnClickListener { showAddDebtDialog() }
    }

    private fun loadUserPreferences() {
        val userId = authService.getCurrentUserId() ?: run {
            Log.w(TAG, "loadUserPreferences: User ID is null.")
            return
        }
        Log.d(TAG, "loadUserPreferences: Loading preferences for User ID: $userId")

        lifecycleScope.launch {
            try {
                userPreferencesRepository.observePreferences(userId).collectLatest { preferences ->
                    Log.d(TAG, "loadUserPreferences: Received preferences: $preferences")
                    preferences?.let {
                        currentStrategy = it.selectedDebtStrategy
                        strategySpinner.setSelection(DebtStrategyType.values().indexOf(currentStrategy))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading preferences: ${e.message}", e)
                if (isAdded) Toast.makeText(requireContext(), "Error loading preferences: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeDebts() {
        val userId = authService.getCurrentUserId() ?: run {
            Log.w(TAG, "observeDebts: User ID is null. Cannot observe debts.")
            return
        }
        Log.d(TAG, "observeDebts: Starting observation for User ID: $userId")

        lifecycleScope.launch {
            Log.d(TAG, "observeDebts: Collector coroutine launched.")
            try {
                loadingIndicator.visibility = View.VISIBLE
                debtRepository.getAllDebts(userId).collectLatest { list ->
                    Log.i(TAG, "observeDebts: New debt list received. Count: ${list.size}. List: $list") // This is the key log we were missing
                    debts.clear()
                    debts.addAll(list)

                    if (!isAdded || _binding == null) {
                        Log.d(TAG, "observeDebts: Fragment not added or binding is null. Skipping UI update.")
                        return@collectLatest
                    }

                    noDebtsTextView.visibility = if (debts.isEmpty()) View.VISIBLE else View.GONE
                    debtsRecyclerView.visibility = if (debts.isEmpty()) View.GONE else View.VISIBLE
                    debtAdapter.submitList(debts.toList())
                    updateDebtSummary()
                    loadingIndicator.visibility = View.GONE
                    Log.d(TAG, "observeDebts: UI updated.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading debts in observeDebts: ${e.message}", e) // Log the error
                if (isAdded) Toast.makeText(requireContext(), "Error loading debts: ${e.message}", Toast.LENGTH_SHORT).show()
                loadingIndicator.visibility = View.GONE // Hide loading on error
            }
        }
    }

    private fun updateDebtSummary() {
        Log.d(TAG, "updateDebtSummary: Updating UI summary.")
        val totalDebt = debts.sumOf { it.totalAmount }
        val totalPaid = debts.sumOf { it.amountPaid }
        val percentage = if (totalDebt > 0) (totalPaid / totalDebt * 100).toInt() else 0

        progressBar.progress = percentage
        progressPercentageTextView.text = "$percentage%"
        totalDebtTextView.text = currencyFormatter.format(totalDebt)
        paidOffTextView.text = currencyFormatter.format(totalPaid)
        monthlyPaymentTextView.text = currencyFormatter.format(debts.sumOf { it.monthlyPayment })

        val estimatedDate = debtStrategy.calculateEstimatedPayoffDate(debts, currentStrategy)
        dateEstimateTextView.text = "Estimated: ${dateFormatter.format(estimatedDate)}"
        Log.d(TAG, "updateDebtSummary: UI summary updated.")
    }

    private fun updateStrategy(strategy: DebtStrategyType) {
        Log.d(TAG, "updateStrategy: Updating strategy to $strategy")
        currentStrategy = strategy
        val userId = authService.getCurrentUserId() ?: run {
            Log.w(TAG, "updateStrategy: User ID is null. Cannot save preferences.")
            return
        }

        lifecycleScope.launch {
            try {
                userPreferencesRepository.savePreferences(UserPreferences(userId, strategy))
                strategyDescriptionTextView.text = when (strategy) {
                    DebtStrategyType.AVALANCHE -> "Pay off highest interest rates first"
                    DebtStrategyType.SNOWBALL -> "Pay off smallest balances first"
                    DebtStrategyType.DEBT_CONSOLIDATION -> "Combine debts into one loan"
                    DebtStrategyType.HIGHEST_INTEREST_FIRST -> "Focus on highest interest debts"
                    DebtStrategyType.BALANCE_PROPORTION -> "Proportional payments by balance"
                    DebtStrategyType.DEBT_STACKING -> "Stack payments one at a time"
                }
                updateDebtSummary() // Recalculate summary based on new strategy
                Log.d(TAG, "updateStrategy: Strategy updated and preferences saved.")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating strategy: ${e.message}", e)
                if (isAdded) Toast.makeText(requireContext(), "Error updating strategy: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddDebtDialog() {
        Log.d(TAG, "showAddDebtDialog: Showing add debt dialog.")
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_debt, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.debtNameEditText)
        val amountEditText = dialogView.findViewById<TextInputEditText>(R.id.debtAmountEditText)
        val interestRateEditText = dialogView.findViewById<TextInputEditText>(R.id.interestRateEditText)
        val monthlyPaymentEditText = dialogView.findViewById<TextInputEditText>(R.id.monthlyPaymentEditText)
        val amountPaidEditText = dialogView.findViewById<TextInputEditText>(R.id.amountPaidEditText)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancelButton)
        val saveButton = dialogView.findViewById<MaterialButton>(R.id.saveButton)

        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        cancelButton.setOnClickListener {
            Log.d(TAG, "Add Debt Dialog: Cancel button clicked.")
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            Log.d(TAG, "Add Debt Dialog: Save button clicked.")
            val name = nameEditText.text.toString()
            val amount = amountEditText.text.toString().toDoubleOrNull()
            val rate = interestRateEditText.text.toString().toDoubleOrNull()
            val monthly = monthlyPaymentEditText.text.toString().toDoubleOrNull()
            val paid = amountPaidEditText.text.toString().toDoubleOrNull() ?: 0.0

            Log.d(TAG, "Add Debt Dialog: Raw Inputs -> Name: '$name', Amount: '$amount', Rate: '$rate', Monthly: '$monthly', Paid: '$paid'")

            if (name.isBlank() || amount == null || rate == null || monthly == null) {
                Log.d(TAG, "Add Debt Dialog: Input validation failed.")
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d(TAG, "Input validation passed. Proceeding to addDebt function.")

            addDebt(name, amount, rate, monthly, paid)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addDebt(name: String, amount: Double, interestRate: Double, monthlyPayment: Double, amountPaid: Double = 0.0) {
        val userId = authService.getCurrentUserId() ?: run {
            Log.w(TAG, "addDebt: User ID is null. Cannot add debt.")
            return
        }
        Log.d(TAG, "addDebt: Preparing to add debt for User ID: $userId")

        val debt = Debt(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name,
            totalAmount = amount,
            amountPaid = amountPaid,
            interestRate = interestRate,
            monthlyPayment = monthlyPayment,
            creationDate = System.currentTimeMillis(),
            lastPaymentDate = null
        )
        Log.i(TAG, "addDebt coroutine: Attempting to save debt: $debt")

        lifecycleScope.launch {
            val result = debtRepository.addDebt(debt)
            result.onSuccess { savedDebt ->
                Log.i(TAG, "addDebt coroutine: Debt added successfully to repository: $savedDebt") // Success log
                if (isAdded) Toast.makeText(requireContext(), "Debt added", Toast.LENGTH_SHORT).show()
            }.onFailure { exception ->
                Log.e(TAG, "addDebt coroutine: Failed to add debt to repository.", exception) // Failure log
                if (isAdded) Toast.makeText(requireContext(), "Failed to add debt: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteDebt(debt: Debt) {
        Log.d(TAG, "deleteDebt: Showing delete confirmation dialog for debt: ${debt.id}")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Debt")
            .setMessage("Are you sure you want to delete '${debt.name}'?")
            .setPositiveButton("Delete") { dialog, _ ->
                Log.d(TAG, "deleteDebt: User confirmed deletion for debt: ${debt.id}")
                lifecycleScope.launch {
                    val result = debtRepository.deleteDebt(debt.id)
                    result.onSuccess {
                        Log.i(TAG, "deleteDebt: Debt deleted successfully from repository: ${debt.id}")
                        if (isAdded) Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Log.e(TAG, "deleteDebt: Failed to delete debt from repository: ${debt.id}", it)
                        if (isAdded) Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", { dialog, _ ->
                Log.d(TAG, "deleteDebt: User cancelled deletion for debt: ${debt.id}")
                dialog.dismiss()
            })
            .show()
    }

    private fun showStrategyBasedPaymentDialog() {
        Log.d(TAG, "showStrategyBasedPaymentDialog: Showing strategy-based payment dialog.")
        if (debts.isEmpty()) {
            Log.d(TAG, "showStrategyBasedPaymentDialog: No debts to pay.")
            Toast.makeText(requireContext(), "No debts to pay", Toast.LENGTH_SHORT).show()
            return
        }

        val optimalDebt = when (currentStrategy) {
            DebtStrategyType.AVALANCHE, DebtStrategyType.HIGHEST_INTEREST_FIRST -> debts.maxByOrNull { it.interestRate }
            DebtStrategyType.SNOWBALL -> debts.minByOrNull { it.getRemainingBalance() }
            DebtStrategyType.DEBT_CONSOLIDATION, DebtStrategyType.BALANCE_PROPORTION -> debts.firstOrNull() // Assuming first for these strategies for simplicity
            DebtStrategyType.DEBT_STACKING -> debts.minByOrNull { it.getMonthsRemaining() }
        } ?: debts.first() // Fallback to the first debt if strategy yields null

        Log.d(TAG, "showStrategyBasedPaymentDialog: Recommended debt for payment: ${optimalDebt.name}")
        showPaymentDialog(optimalDebt)
    }

    private fun showPaymentDialog(debt: Debt) {
        Log.d(TAG, "showPaymentDialog: Showing payment dialog for debt: ${debt.name}")
        val dialogView = layoutInflater.inflate(R.layout.dialog_make_payment, null)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val amountEdit = dialogView.findViewById<TextInputEditText>(R.id.paymentAmountEditText)
        val payButton = dialogView.findViewById<MaterialButton>(R.id.saveButton)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancelButton)

        dialogView.findViewById<TextView>(R.id.debtNameTextView).text = debt.name
        dialogView.findViewById<TextView>(R.id.debtBalanceTextView).text = "Remaining: ${currencyFormatter.format(debt.getRemainingBalance())}"
        dialogView.findViewById<TextView>(R.id.debtMonthlyPaymentTextView).text = "Monthly Payment: ${currencyFormatter.format(debt.monthlyPayment)}"
        dialogView.findViewById<TextView>(R.id.strategyInfoTextView).text = "Strategy: $currentStrategy"
        dialogView.findViewById<TextView>(R.id.recommendedPaymentTextView).text = "Recommended: ${currencyFormatter.format(debt.monthlyPayment)}"

        amountEdit.setText(debt.monthlyPayment.toString())

        cancelButton.setOnClickListener {
            Log.d(TAG, "Payment Dialog: Cancel button clicked for debt: ${debt.name}")
            dialog.dismiss()
        }
        payButton.setOnClickListener {
            Log.d(TAG, "Payment Dialog: Pay button clicked for debt: ${debt.name}")
            val payment = amountEdit.text.toString().toDoubleOrNull()
            if (payment == null || payment <= 0) {
                Log.d(TAG, "Payment Dialog: Invalid payment amount entered: ${amountEdit.text}")
                Toast.makeText(requireContext(), "Enter valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d(TAG, "Payment Dialog: Valid payment amount entered: $payment. Proceeding to makePayment.")
            makePayment(debt, payment)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun makePayment(debt: Debt, paymentAmount: Double) {
        val userId = authService.getCurrentUserId() ?: run {
            Log.w(TAG, "makePayment: User ID is null. Cannot record payment.")
            return
        }
        Log.d(TAG, "makePayment: Recording payment of $paymentAmount for debt ${debt.name} (ID: ${debt.id}) by User ID: $userId")

        lifecycleScope.launch {
            try {
                loadingIndicator.visibility = View.VISIBLE
                val newDebt = debt.copy(
                    amountPaid = debt.amountPaid + paymentAmount,
                    lastPaymentDate = System.currentTimeMillis()
                )
                Log.d(TAG, "makePayment: Updated debt object for payment: $newDebt")
                val result = debtRepository.updateDebt(newDebt)
                result.onSuccess {
                    Log.i(TAG, "makePayment: Debt updated successfully after payment: ${it.id}")
                    if (isAdded) Toast.makeText(requireContext(), "Payment recorded", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Log.e(TAG, "makePayment: Failed to update debt after payment: ${debt.id}", it)
                    if (isAdded) Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
                loadingIndicator.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "makePayment: Error during payment process for debt ${debt.id}: ${e.message}", e)
                if (isAdded) Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                loadingIndicator.visibility = View.GONE // Hide loading on error
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }

    companion object {
        fun newInstance() = DebtFragment()
    }
}
