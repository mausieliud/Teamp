package com.example.logic2

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.logic2.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

data class Expense(val id: Int, val description: String, val amount: Double, val category: String, val date: LocalDate = LocalDate.now())

class BudgetTracker(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private var totalBudget: Double = 0.0
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate = LocalDate.now()
    private var allocationPerDay: Double = 0.0
    private var totalRemainingBudget: Double = 0.0
    private val expenses = mutableListOf<Expense>() // Keep expenses in memory for faster access

    init {
        loadBudget()
        loadExpenses()
    }

    private fun loadBudget() {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.BUDGET_TABLE_NAME,
            arrayOf(
                DatabaseHelper.BUDGET_COLUMN_TOTAL_BUDGET,
                DatabaseHelper.BUDGET_COLUMN_START_DATE,
                DatabaseHelper.BUDGET_COLUMN_END_DATE,
                DatabaseHelper.BUDGET_COLUMN_ALLOCATION_PER_DAY,
                DatabaseHelper.BUDGET_COLUMN_REMAINING_BUDGET
            ),
            null, null, null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                totalBudget = it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.BUDGET_COLUMN_TOTAL_BUDGET))
                startDate = LocalDate.parse(it.getString(it.getColumnIndexOrThrow(DatabaseHelper.BUDGET_COLUMN_START_DATE)), DateTimeFormatter.ISO_DATE)
                endDate = LocalDate.parse(it.getString(it.getColumnIndexOrThrow(DatabaseHelper.BUDGET_COLUMN_END_DATE)), DateTimeFormatter.ISO_DATE)
                allocationPerDay = it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.BUDGET_COLUMN_ALLOCATION_PER_DAY))
                totalRemainingBudget = it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.BUDGET_COLUMN_REMAINING_BUDGET))
            } else {
                // Initialize with default values if no budget in DB
                totalBudget = 0.0
                startDate = LocalDate.now()
                endDate = LocalDate.now()
                allocationPerDay = 0.0
                totalRemainingBudget = 0.0
            }
        }
        db.close()
    }

    private fun saveBudgetToDb() {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.BUDGET_TABLE_NAME, null, null) // Clear old budget data
        val values = ContentValues().apply {
            put(DatabaseHelper.BUDGET_COLUMN_TOTAL_BUDGET, totalBudget)
            put(DatabaseHelper.BUDGET_COLUMN_START_DATE, startDate.format(DateTimeFormatter.ISO_DATE))
            put(DatabaseHelper.BUDGET_COLUMN_END_DATE, endDate.format(DateTimeFormatter.ISO_DATE))
            put(DatabaseHelper.BUDGET_COLUMN_ALLOCATION_PER_DAY, allocationPerDay)
            put(DatabaseHelper.BUDGET_COLUMN_REMAINING_BUDGET, totalRemainingBudget)
        }
        db.insert(DatabaseHelper.BUDGET_TABLE_NAME, null, values)
        db.close()
    }


    private fun loadExpenses() {
        expenses.clear()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.EXPENSE_TABLE_NAME,
            arrayOf(
                DatabaseHelper.EXPENSE_COLUMN_ID,
                DatabaseHelper.EXPENSE_COLUMN_DESCRIPTION,
                DatabaseHelper.EXPENSE_COLUMN_AMOUNT,
                DatabaseHelper.EXPENSE_COLUMN_CATEGORY,
                DatabaseHelper.EXPENSE_COLUMN_DATE
            ),
            null, null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.EXPENSE_COLUMN_ID))
                val description = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.EXPENSE_COLUMN_DESCRIPTION))
                val amount = it.getDouble(it.getColumnIndexOrThrow(DatabaseHelper.EXPENSE_COLUMN_AMOUNT))
                val category = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.EXPENSE_COLUMN_CATEGORY))
                val date = LocalDate.parse(it.getString(it.getColumnIndexOrThrow(DatabaseHelper.EXPENSE_COLUMN_DATE)), DateTimeFormatter.ISO_DATE)
                expenses.add(Expense(id, description, amount, category, date))
            }
        }
        db.close()
    }

    fun setBudget(amount: Double, endDate: LocalDate) {
        totalBudget = amount
        this.endDate = endDate
        startDate = LocalDate.now()
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        allocationPerDay = if (totalDays > 0) amount / totalDays else 0.0
        totalRemainingBudget = amount
        saveBudgetToDb() // Save budget to database
        recalculateDailyAllocationIfNeeded() // Recalculate based on current expenses and budget
    }

    fun addExpense(description: String, amount: Double, category: String) {
        val id = if (expenses.isEmpty()) 1 else expenses.maxOf { it.id } + 1
        val expense = Expense(id, description, amount, category)
        expenses.add(expense)
        totalRemainingBudget -= amount

        // Save expense to database
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.EXPENSE_COLUMN_ID, id)
            put(DatabaseHelper.EXPENSE_COLUMN_DESCRIPTION, description)
            put(DatabaseHelper.EXPENSE_COLUMN_AMOUNT, amount)
            put(DatabaseHelper.EXPENSE_COLUMN_CATEGORY, category)
            put(DatabaseHelper.EXPENSE_COLUMN_DATE, expense.date.format(DateTimeFormatter.ISO_DATE))
        }
        db.insert(DatabaseHelper.EXPENSE_TABLE_NAME, null, values)
        db.close()

        // Update remaining budget in database
        updateRemainingBudgetInDb()

        recalculateDailyAllocationIfNeeded() // Call this function after adding expense
    }

    private fun updateRemainingBudgetInDb() {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.BUDGET_COLUMN_REMAINING_BUDGET, totalRemainingBudget)
        }
        db.update(DatabaseHelper.BUDGET_TABLE_NAME, values, null, null) // Assuming only one budget record
        db.close()
    }


    private fun recalculateDailyAllocationIfNeeded() {
        val today = LocalDate.now()
        val dailySpent = expenses.filter { it.date == today }.sumOf { it.amount }

        if (dailySpent > allocationPerDay) {
            val overSpentAmount = dailySpent - allocationPerDay
            if (totalRemainingBudget < 0) { // To prevent negative remaining budget, consider overspent amount only within remaining budget
                return // Or handle it as per your requirement, e.g., set allocation to 0
            }

            val remainingDays = ChronoUnit.DAYS.between(today.plusDays(1), endDate).toInt() + 1 // Days from tomorrow onwards
            if (remainingDays > 0) {
                allocationPerDay = if (totalRemainingBudget > 0) totalRemainingBudget / remainingDays else 0.0
            } else {
                allocationPerDay = 0.0 // No more days to allocate to
            }
            saveBudgetToDb() // Save updated allocation to DB
        }
    }

    fun getRemainingDailyAllocation(date: LocalDate = LocalDate.now()): Double {
        val dailySpent = expenses.filter { it.date == date }.sumOf { it.amount }
        return allocationPerDay - dailySpent
    }

    fun getExpensesList(): String {
        loadExpenses() // Refresh expenses from DB every time to ensure latest data
        return expenses.joinToString("\n") { "${it.description} - $${it.amount} (${it.category}) on ${it.date}" }
    }

    fun getBudgetSummary(): String {
        loadBudget() // Refresh budget data from DB to ensure latest data
        return "Total Budget: $$totalBudget\n" +
                "Daily Allocation: $$allocationPerDay\n" +
                "Remaining for today: $${getRemainingDailyAllocation()}\n" +
                "Remaining Amount: $${getTotalRemainingBudget()}"
    }

    // Public getter function to access totalRemainingBudget
    fun getTotalRemainingBudget(): Double {
        loadBudget() // Refresh remaining budget from DB
        return totalRemainingBudget
    }
}


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "BudgetDB"
        private const val DATABASE_VERSION = 1

        const val EXPENSE_TABLE_NAME = "expenses"
        const val EXPENSE_COLUMN_ID = "id"
        const val EXPENSE_COLUMN_DESCRIPTION = "description"
        const val EXPENSE_COLUMN_AMOUNT = "amount"
        const val EXPENSE_COLUMN_CATEGORY = "category"
        const val EXPENSE_COLUMN_DATE = "date"

        const val BUDGET_TABLE_NAME = "budget"
        const val BUDGET_COLUMN_TOTAL_BUDGET = "total_budget"
        const val BUDGET_COLUMN_START_DATE = "start_date"
        const val BUDGET_COLUMN_END_DATE = "end_date"
        const val BUDGET_COLUMN_ALLOCATION_PER_DAY = "allocation_per_day"
        const val BUDGET_COLUMN_REMAINING_BUDGET = "remaining_budget"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createExpenseTableQuery = """
            CREATE TABLE $EXPENSE_TABLE_NAME (
                $EXPENSE_COLUMN_ID INTEGER PRIMARY KEY,
                $EXPENSE_COLUMN_DESCRIPTION TEXT,
                $EXPENSE_COLUMN_AMOUNT REAL,
                $EXPENSE_COLUMN_CATEGORY TEXT,
                $EXPENSE_COLUMN_DATE TEXT
            )
        """.trimIndent()

        val createBudgetTableQuery = """
            CREATE TABLE $BUDGET_TABLE_NAME (
                $BUDGET_COLUMN_TOTAL_BUDGET REAL,
                $BUDGET_COLUMN_START_DATE TEXT,
                $BUDGET_COLUMN_END_DATE TEXT,
                $BUDGET_COLUMN_ALLOCATION_PER_DAY REAL,
                $BUDGET_COLUMN_REMAINING_BUDGET REAL
            )
        """.trimIndent() // Only one budget record

        db?.execSQL(createExpenseTableQuery)
        db?.execSQL(createBudgetTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $EXPENSE_TABLE_NAME")
        db?.execSQL("DROP TABLE IF EXISTS $BUDGET_TABLE_NAME")
        onCreate(db)
    }
}


class MainActivity : AppCompatActivity() {

    private lateinit var tracker: BudgetTracker
    private var selectedEndDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tracker = BudgetTracker(this) // Initialize BudgetTracker with context

        val budgetInput: EditText = findViewById(R.id.budgetInput)
        val endDateButton: Button = findViewById(R.id.endDateButton)
        val descriptionInput: EditText = findViewById(R.id.descriptionInput)
        val amountInput: EditText = findViewById(R.id.amountInput)
        val categoryInput: EditText = findViewById(R.id.categoryInput)
        val budgetTextView: TextView = findViewById(R.id.budgetTextView)
        val expensesTextView: TextView = findViewById(R.id.expensesTextView)
        val addExpenseButton: Button = findViewById(R.id.addExpenseButton)
        val setBudgetButton: Button = findViewById(R.id.setBudgetButton)
        val endBudgetButton: Button = findViewById(R.id.endBudgetButton) // Find the new button

        budgetTextView.text = tracker.getBudgetSummary() // Initial summary display
        expensesTextView.text = tracker.getExpensesList() // Initial expenses display


        endDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                selectedEndDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                endDateButton.text = "End Date: $selectedEndDate"
            }, year, month, day)

            datePicker.show()
        }

        setBudgetButton.setOnClickListener {
            val budget = budgetInput.text.toString().toDoubleOrNull()
            if (budget != null && selectedEndDate != null) {
                tracker.setBudget(budget, selectedEndDate!!)
                budgetTextView.text = tracker.getBudgetSummary()
            } else {
                Toast.makeText(this, "Please enter a valid budget and pick an end date", Toast.LENGTH_SHORT).show()
            }
        }

        addExpenseButton.setOnClickListener {
            val description = descriptionInput.text.toString()
            val amount = amountInput.text.toString().toDoubleOrNull()
            val category = categoryInput.text.toString()

            if (description.isNotEmpty() && amount != null && category.isNotEmpty()) {
                tracker.addExpense(description, amount, category)
                expensesTextView.text = tracker.getExpensesList()
                budgetTextView.text = tracker.getBudgetSummary()
                Toast.makeText(this, "Expense added!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }

        endBudgetButton.setOnClickListener {  // ADD THIS setOnClickListener for endBudgetButton
            val remainingAmount = tracker.getTotalRemainingBudget() // Call the getter function!
            val message = "You saved: $$remainingAmount" // Create the message
            Toast.makeText(this, message, Toast.LENGTH_LONG).show() // Show the Toast
        }

        val exitButton: Button = findViewById(R.id.exitButton) // Find the Exit Button
        exitButton.setOnClickListener {
            finishAffinity() // Exit the app
        }
    }
}