package com.example.bloodpressuretracking.ui.login

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
 * ViewModel for the Login screen.
 * Handles user input and authentication logic.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Update username in state.
     */
    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    /**
     * Update password in state.
     */
    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    /**
     * Handle login button click.
     * Validates input and attempts authentication.
     */
    fun onLoginClick() {
        val currentState = _uiState.value

        // Validate input
        if (currentState.username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ユーザー名を入力してください") }
            return
        }
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "パスワードを入力してください") }
            return
        }

        // Start login
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = authRepository.signIn(currentState.username, currentState.password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccess = true,
                            errorMessage = null
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccess = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
