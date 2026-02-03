package com.example.bloodpressuretracking.data.repository

/**
 * Repository interface for authentication operations.
 * Handles AWS Cognito authentication via Amplify.
 */
interface AuthRepository {
    /**
     * Check if there's a valid authenticated session.
     * @return AuthResult<Boolean> - Success(true) if session is valid, Success(false) if not, Error on failure
     */
    suspend fun checkSession(): AuthResult<Boolean>

    /**
     * Sign in with username and password.
     * @param username User's username
     * @param password User's password
     * @return AuthResult<Unit> - Success on successful sign in, Error on failure
     */
    suspend fun signIn(username: String, password: String): AuthResult<Unit>

    /**
     * Get the current ID token for API authorization.
     * @return String? - The ID token if available, null otherwise
     */
    suspend fun getIdToken(): String?

    /**
     * Sign out the current user.
     * @return AuthResult<Unit> - Success on successful sign out, Error on failure
     */
    suspend fun signOut(): AuthResult<Unit>
}

/**
 * Sealed class representing authentication operation results.
 */
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}
