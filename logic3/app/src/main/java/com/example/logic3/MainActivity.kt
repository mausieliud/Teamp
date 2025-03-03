@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.logic3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import android.app.DatePickerDialog
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BudgetTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BudgetTrackerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTrackerApp() {
    val context = LocalContext.current
    val tracker = remember { BudgetTracker(context) }

    // State variables
    var budgetAmount by remember { mutableStateOf("") }
    var selectedEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var expenseDescription by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("") }

    // Tab state
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Dashboard", "Add Expense", "Budget Setup", "Reports")

    // For refreshing expense list and budget summary
    var refreshTrigger by remember { mutableStateOf(0) }
    val refreshData = {
        refreshTrigger++
        Unit
    }

    // Snackbar for user feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope() // **Get a coroutine scope**

    // Parse budget summary and expense list
    val budgetSummary = remember(refreshTrigger) { tracker.getBudgetSummary().split("\n") }
    val expensesList = remember(refreshTrigger) {
        val expenseStringList = tracker.getExpensesList().split("\n")
        expenseStringList.mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null // Skip blank lines
            val parts = line.split(" - ", limit = 2)
            val description = parts.getOrNull(0) ?: ""
            val restParts = parts.getOrNull(1)?.split(" ", "(", ")", "on ") ?: emptyList()
            val amount = restParts.firstOrNull { it.startsWith("$") }?.substring(1)?.toDoubleOrNull() ?: 0.0
            val category = restParts.firstOrNull { it.endsWith(")") }?.removeSuffix(")")?.removePrefix("(") ?: ""
            val date = restParts.lastOrNull()?.trim() ?: LocalDate.now().toString()
            Expense(0, description, amount, category, LocalDate.parse(date)) // ID is not parsed from string, assuming auto-increment by DB
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Add SnackbarHost
        topBar = {
            TopAppBar(
                title = { Text("Budget Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = when(index) {
                                    0 -> Icons.Default.Dashboard
                                    1 -> Icons.Default.Add
                                    2 -> Icons.Default.Settings
                                    else -> Icons.Default.Assessment
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(budgetSummary, expensesList, tracker, refreshData)
                1 -> AddExpenseScreen(
                    expenseDescription = expenseDescription,
                    onDescriptionChange = { expenseDescription = it },
                    expenseAmount = expenseAmount,
                    onAmountChange = { expenseAmount = it },
                    expenseCategory = expenseCategory,
                    onCategoryChange = { expenseCategory = it },
                    onAddExpense = {
                        val amount = expenseAmount.toDoubleOrNull()
                        if (expenseDescription.isNotEmpty() && amount != null && expenseCategory.isNotEmpty()) {
                            tracker.addExpense(expenseDescription, amount, expenseCategory)
                            expenseDescription = ""
                            expenseAmount = ""
                            expenseCategory = ""
                            refreshData()
                            // Show Snackbar for confirmation
                            coroutineScope.launch{ // **Launch coroutine here**
                                snackbarHostState.showSnackbar("Expense added successfully!")
                            }
                        } else {
                            coroutineScope.launch { // **Launch coroutine here**
                                snackbarHostState.showSnackbar("Invalid expense details. Please check.")
                            }
                        }
                    }
                )
                2 -> {
                    // Create the date picker within the composable function call for the correct context
                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val formattedDate = selectedEndDate?.format(dateFormatter) ?: "Select Date"

                    BudgetSetupScreen(
                        budgetAmount = budgetAmount,
                        onBudgetAmountChange = { budgetAmount = it },
                        selectedEndDate = selectedEndDate,
                        displayDate = formattedDate,
                        onShowDatePicker = {
                            // Create the date picker on demand when the button is clicked
                            val calendar = Calendar.getInstance()

                            // Set the calendar to the selected date if one exists, otherwise use current date
                            selectedEndDate?.let {
                                calendar.set(it.year, it.monthValue - 1, it.dayOfMonth)
                            }

                            val datePickerDialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedEndDate = LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )

                            datePickerDialog.show()
                        },
                        onSetBudget = {
                            val amount = budgetAmount.toDoubleOrNull()
                            if (amount != null && selectedEndDate != null) {
                                tracker.setBudget(amount, selectedEndDate!!)
                                refreshData()
                                coroutineScope.launch { // **Launch coroutine here**
                                    snackbarHostState.showSnackbar("Budget set successfully!")
                                }
                            } else {
                                coroutineScope.launch { // **Launch coroutine here**
                                    snackbarHostState.showSnackbar("Invalid budget amount or end date.")
                                }
                            }
                        },
                        onEndBudget = {
                            val remainingAmount = tracker.getTotalRemainingBudget()
                            coroutineScope.launch { // **Launch coroutine here**
                                snackbarHostState.showSnackbar("Budget ended. You have $${"%.2f".format(remainingAmount)} remaining.")
                            }
                            refreshData()
                        }
                    )
                }
                3 -> ReportsScreen(expensesList)
            }
        }
    }
}

@Composable
fun DashboardScreen(
    budgetSummary: List<String>,
    expenses: List<Expense>,
    tracker: BudgetTracker,
    refreshData: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Budget Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Budget Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                budgetSummary.forEach { line ->
                    val parts = line.split(": ")
                    if (parts.size == 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = parts[0], fontWeight = FontWeight.Medium)
                            Text(text = parts[1], fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Recent Expenses
        Text(
            text = "Recent Expenses",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses yet",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(expenses.take(5)) { expense ->
                    ExpenseItem(expense)
                }

                if (expenses.size > 5) {
                    item {
                        TextButton(
                            onClick = { /* Navigate to full list */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text("View All Expenses")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryChip(expense.category)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = expense.date.format(DateTimeFormatter.ISO_DATE),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = "$${expense.amount}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun CategoryChip(category: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = category,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    expenseDescription: String,
    onDescriptionChange: (String) -> Unit,
    expenseAmount: String,
    onAmountChange: (String) -> Unit,
    expenseCategory: String,
    onCategoryChange: (String) -> Unit,
    onAddExpense: () -> Unit
) {
    val categoryOptions = listOf("Food", "Transportation", "Entertainment", "Utilities", "Shopping", "Other")
    var showCategoryDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Add New Expense",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = expenseDescription,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = "Description") }
        )

        OutlinedTextField(
            value = expenseAmount,
            onValueChange = onAmountChange,
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = "Amount") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            )
        )

        Box {
            OutlinedTextField(
                value = expenseCategory,
                onValueChange = onCategoryChange,
                label = { Text("Category") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCategoryDropdown = true },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = "Category") },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Select Category",
                        modifier = Modifier.clickable { showCategoryDropdown = true }
                    )
                },
                readOnly = true
            )

            DropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                categoryOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onCategoryChange(option)
                            showCategoryDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAddExpense,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = expenseDescription.isNotEmpty() &&
                    expenseAmount.isNotEmpty() &&
                    expenseCategory.isNotEmpty() &&
                    expenseAmount.toDoubleOrNull() != null
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Expense")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Expense")
        }
    }
}

@Composable
fun BudgetSetupScreen(
    budgetAmount: String,
    onBudgetAmountChange: (String) -> Unit,
    selectedEndDate: LocalDate?,
    displayDate: String,
    onShowDatePicker: () -> Unit,
    onSetBudget: () -> Unit,
    onEndBudget: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Budget Setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = budgetAmount,
            onValueChange = onBudgetAmountChange,
            label = { Text("Budget Amount") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = "Budget") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            )
        )

        // Date selection field - modified to show formatted date and be more visibly clickable
        OutlinedTextField(
            value = displayDate,
            onValueChange = { /* Readonly field, handled by date picker */ },
            label = { Text("End Date") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onShowDatePicker),
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = "End Date") },
            trailingIcon = {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Select Date",
                    modifier = Modifier.clickable(onClick = onShowDatePicker)
                )
            },
            readOnly = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledTextColor = LocalContentColor.current,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSetBudget,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = budgetAmount.isNotEmpty() &&
                    budgetAmount.toDoubleOrNull() != null &&
                    selectedEndDate != null
        ) {
            Icon(Icons.Default.SaveAlt, contentDescription = "Set Budget")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Set Budget")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onEndBudget,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = "End Budget")
            Spacer(modifier = Modifier.width(8.dp))
            Text("End Budget & Show Savings")
        }
    }
}

@Composable
fun ReportsScreen(expenses: List<Expense>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Expense Reports",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Category Summary
        val categoryTotals = expenses
            .groupBy { it.category }
            .mapValues { it.value.sumOf { expense -> expense.amount } }
            .toList()
            .sortedByDescending { it.second }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Category Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (categoryTotals.isEmpty()) {
                    Text(
                        text = "No expense data available",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    categoryTotals.forEach { (category, total) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryChip(category)
                            Text(
                                text = "$${total}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Daily Spending
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Recent Daily Spending",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val dailyTotals = expenses
                    .groupBy { it.date }
                    .mapValues { it.value.sumOf { expense -> expense.amount } }
                    .toList()
                    .sortedByDescending { it.first }
                    .take(7)

                if (dailyTotals.isEmpty()) {
                    Text(
                        text = "No daily spending data available",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    dailyTotals.forEach { (date, total) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = date.format(DateTimeFormatter.ISO_DATE))
                            Text(
                                text = "$${total}",
                                fontWeight = FontWeight.Bold,
                                color = if (total > 0) MaterialTheme.colorScheme.error else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // All Expenses Button
        Button(
            onClick = { /* Navigate to all expenses */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.List, contentDescription = "All Expenses")
            Spacer(modifier = Modifier.width(8.dp))
            Text("View All Expenses")
        }
    }
}

// Theme definition (simplified for this example)
@Composable
fun BudgetTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = Typography(),
        content = content
    )
}
