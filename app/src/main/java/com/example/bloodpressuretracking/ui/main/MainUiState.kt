package com.example.bloodpressuretracking.ui.main

/**
 * UI state for the Main screen (blood pressure input).
 */
data class MainUiState(
    val systolic: String = "",      // 最高血圧
    val diastolic: String = "",     // 最低血圧
    val pulse: String = "",         // 脈拍
    val isSubmitting: Boolean = false,
    val resultMessage: String? = null,
    val isError: Boolean = false
)
