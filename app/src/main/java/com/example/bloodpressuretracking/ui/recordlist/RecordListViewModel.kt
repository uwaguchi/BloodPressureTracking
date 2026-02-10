package com.example.bloodpressuretracking.ui.recordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodpressuretracking.data.repository.BloodPressureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Record List screen.
 * Handles fetching and displaying blood pressure records.
 */
@HiltViewModel
class RecordListViewModel @Inject constructor(
    private val bloodPressureRepository: BloodPressureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordListUiState())
    val uiState: StateFlow<RecordListUiState> = _uiState.asStateFlow()

    init {
        fetchRecords()
    }

    /**
     * Fetch blood pressure records from repository.
     */
    fun fetchRecords() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = bloodPressureRepository.fetchRecords()

            result.fold(
                onSuccess = { records ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            records = records,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            records = emptyList(),
                            errorMessage = error.message ?: "データの取得に失敗しました"
                        )
                    }
                }
            )
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
