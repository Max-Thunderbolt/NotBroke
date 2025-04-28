package com.example.notbroke.models

import java.util.Date

data class NetWorthEntry(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val date: Date = Date()
)
