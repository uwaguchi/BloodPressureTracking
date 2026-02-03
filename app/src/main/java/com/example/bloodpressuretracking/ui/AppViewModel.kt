package com.example.bloodpressuretracking.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodpressuretracking.data.repository.AuthRepository
import com.example.bloodpressuretracking.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-level ViewModel for session management and navigation.
 * Checks session on startup and manages authentication state.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    /**
     * Check if there's a valid session on app startup.
     */
    private fun checkSession() {
        viewModelScope.launch {
            when (val result = authRepository.checkSession()) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            isAuthenticated = result.data
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            isAuthenticated = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Called when login is successful.
     * Updates state to navigate to main screen.
     */
    fun onLoginSuccess() {
        _uiState.update {
            it.copy(isAuthenticated = true)
        }
    }
}
