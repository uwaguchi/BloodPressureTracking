package com.example.bloodpressuretracking.ui

/**
 * UI state for the App-level navigation.
 */
data class AppUiState(
    val isCheckingSession: Boolean = true,
    val isAuthenticated: Boolean = false
)
