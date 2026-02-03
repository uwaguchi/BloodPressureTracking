package com.example.bloodpressuretracking.ui.recordlist

import com.example.bloodpressuretracking.data.repository.BloodPressureRecord

/**
 * UI state for the Record List screen.
 */
data class RecordListUiState(
    val records: List<BloodPressureRecord> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
