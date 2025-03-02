package com.example.logic3

import java.time.LocalDate

data class Expense(
    val id: Int,
    val description: String,
    val amount: Double,
    val category: String,
    val date: LocalDate = LocalDate.now()
)
