package com.example.logic3

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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
                $EXPENSE_COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $EXPENSE_COLUMN_DESCRIPTION TEXT,
                $EXPENSE_COLUMN_AMOUNT REAL,
                $EXPENSE_COLUMN_CATEGORY TEXT,
                $EXPENSE_COLUMN_DATE TEXT
            )
        """.trimIndent() // Changed id to AUTOINCREMENT

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