package com.example.notbroke.fragments

import android.app.Dialog
import android.os.Bundle
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
    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDebtBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repositoryFactory = RepositoryFactory.getInstance(requireContext())
        initializeViews(view)
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
        val userId = authService.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                userPreferencesRepository.observePreferences(userId).collectLatest { preferences ->
                    preferences?.let {
                        currentStrategy = it.selectedDebtStrategy
                        strategySpinner.setSelection(DebtStrategyType.values().indexOf(currentStrategy))
                    }
                }
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Error loading preferences: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeDebts() {
        val userId = authService.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                loadingIndicator.visibility = View.VISIBLE
                debtRepository.getAllDebts(userId).collectLatest { list ->
                    debts.clear()
                    debts.addAll(list)

                    if (!isAdded || _binding == null) return@collectLatest

                    noDebtsTextView.visibility = if (debts.isEmpty()) View.VISIBLE else View.GONE
                    debtsRecyclerView.visibility = if (debts.isEmpty()) View.GONE else View.VISIBLE
                    debtAdapter.submitList(debts.toList())
                    updateDebtSummary()
                    loadingIndicator.visibility = View.GONE
                }
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Error loading debts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDebtSummary() {
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
    }

    private fun updateStrategy(strategy: DebtStrategyType) {
        currentStrategy = strategy
        val userId = authService.getCurrentUserId() ?: return

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
                updateDebtSummary()
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Error updating strategy: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddDebtDialog() {
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

        cancelButton.setOnClickListener { dialog.dismiss() }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val amount = amountEditText.text.toString().toDoubleOrNull()
            val rate = interestRateEditText.text.toString().toDoubleOrNull()
            val monthly = monthlyPaymentEditText.text.toString().toDoubleOrNull()
            val paid = amountPaidEditText.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isBlank() || amount == null || rate == null || monthly == null) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addDebt(name, amount, rate, monthly, paid)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addDebt(name: String, amount: Double, interestRate: Double, monthlyPayment: Double, amountPaid: Double = 0.0) {
        val userId = authService.getCurrentUserId() ?: return

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

        lifecycleScope.launch {
            val result = debtRepository.addDebt(debt)
            result.onSuccess {
                if (isAdded) Toast.makeText(requireContext(), "Debt added", Toast.LENGTH_SHORT).show()
            }.onFailure {
                if (isAdded) Toast.makeText(requireContext(), "Failed to add debt: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteDebt(debt: Debt) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Debt")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { dialog, _ ->
                lifecycleScope.launch {
                    val result = debtRepository.deleteDebt(debt.id)
                    result.onSuccess {
                        if (isAdded) Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        if (isAdded) Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStrategyBasedPaymentDialog() {
        if (debts.isEmpty()) {
            Toast.makeText(requireContext(), "No debts to pay", Toast.LENGTH_SHORT).show()
            return
        }

        val optimalDebt = when (currentStrategy) {
            DebtStrategyType.AVALANCHE, DebtStrategyType.HIGHEST_INTEREST_FIRST -> debts.maxByOrNull { it.interestRate }
            DebtStrategyType.SNOWBALL -> debts.minByOrNull { it.getRemainingBalance() }
            DebtStrategyType.DEBT_CONSOLIDATION, DebtStrategyType.BALANCE_PROPORTION -> debts.firstOrNull()
            DebtStrategyType.DEBT_STACKING -> debts.minByOrNull { it.getMonthsRemaining() }
        } ?: debts.first()

        showPaymentDialog(optimalDebt)
    }

    private fun showPaymentDialog(debt: Debt) {
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

        cancelButton.setOnClickListener { dialog.dismiss() }
        payButton.setOnClickListener {
            val payment = amountEdit.text.toString().toDoubleOrNull()
            if (payment == null || payment <= 0) {
                Toast.makeText(requireContext(), "Enter valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            makePayment(debt, payment)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun makePayment(debt: Debt, paymentAmount: Double) {
        val userId = authService.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                loadingIndicator.visibility = View.VISIBLE
                val newDebt = debt.copy(
                    amountPaid = debt.amountPaid + paymentAmount,
                    lastPaymentDate = System.currentTimeMillis()
                )
                val result = debtRepository.updateDebt(newDebt)
                result.onSuccess {
                    if (isAdded) Toast.makeText(requireContext(), "Payment recorded", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    if (isAdded) Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
                loadingIndicator.visibility = View.GONE
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
