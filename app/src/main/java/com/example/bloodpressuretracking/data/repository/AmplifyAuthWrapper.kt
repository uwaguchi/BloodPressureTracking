package com.example.bloodpressuretracking.data.repository

/**
 * Wrapper interface for Amplify Auth operations.
 * Allows for easy testing by abstracting Amplify SDK calls.
 */
interface AmplifyAuthWrapper {
    /**
     * Fetch the current authentication session.
     * @return AuthSessionResult indicating if session is valid
     */
    suspend fun fetchAuthSession(): AuthSessionResult

    /**
     * Sign in with username and password.
     * @param username User's username
     * @param password User's password
     * @return SignInResult indicating success or failure
     */
    suspend fun signIn(username: String, password: String): SignInResult

    /**
     * Get the current ID token.
     * @return The ID token string if available, null otherwise
     */
    suspend fun getIdToken(): String?

    /**
     * Sign out the current user.
     * @return SignOutResult indicating success or failure
     */
    suspend fun signOut(): SignOutResult
}

/**
 * Result of fetching auth session.
 */
sealed class AuthSessionResult {
    data class Success(val isSignedIn: Boolean) : AuthSessionResult()
    data class Error(val message: String) : AuthSessionResult()
}

/**
 * Result of sign in operation.
 */
sealed class SignInResult {
    data object Success : SignInResult()
    data class Error(val message: String) : SignInResult()
}

/**
 * Result of sign out operation.
 */
sealed class SignOutResult {
    data object Success : SignOutResult()
    data class Error(val message: String) : SignOutResult()
}
