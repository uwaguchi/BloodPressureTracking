package com.example.bloodpressuretracking.ui.main

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
 * ViewModel for the Main screen.
 * Handles blood pressure data input and submission.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val bloodPressureRepository: BloodPressureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * Update systolic (最高血圧) in state.
     * Only accepts numeric input.
     */
    fun onSystolicChanged(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.update { it.copy(systolic = filtered) }
    }

    /**
     * Update diastolic (最低血圧) in state.
     * Only accepts numeric input.
     */
    fun onDiastolicChanged(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.update { it.copy(diastolic = filtered) }
    }

    /**
     * Update pulse (脈拍) in state.
     * Only accepts numeric input.
     */
    fun onPulseChanged(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.update { it.copy(pulse = filtered) }
    }

    /**
     * Handle submit button click.
     * Validates input and submits to repository.
     */
    fun onSubmitClick() {
        val currentState = _uiState.value

        // Validate input
        if (currentState.systolic.isBlank()) {
            _uiState.update {
                it.copy(resultMessage = "最高血圧を入力してください", isError = true)
            }
            return
        }
        if (currentState.diastolic.isBlank()) {
            _uiState.update {
                it.copy(resultMessage = "最低血圧を入力してください", isError = true)
            }
            return
        }
        if (currentState.pulse.isBlank()) {
            _uiState.update {
                it.copy(resultMessage = "脈拍を入力してください", isError = true)
            }
            return
        }

        // Start submission
        _uiState.update { it.copy(isSubmitting = true, resultMessage = null, isError = false) }

        viewModelScope.launch {
            val result = bloodPressureRepository.submitRecord(
                systolic = currentState.systolic.toInt(),
                diastolic = currentState.diastolic.toInt(),
                pulse = currentState.pulse.toInt()
            )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            resultMessage = "登録が完了しました",
                            isError = false,
                            systolic = "",
                            diastolic = "",
                            pulse = ""
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            resultMessage = error.message ?: "エラーが発生しました",
                            isError = true
                        )
                    }
                }
            )
        }
    }

    /**
     * Clear result message.
     */
    fun clearMessage() {
        _uiState.update { it.copy(resultMessage = null, isError = false) }
    }
}
