package com.example.bloodpressuretracking.data.repository

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using AWS Amplify Cognito.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val amplifyAuthWrapper: AmplifyAuthWrapper
) : AuthRepository {

    override suspend fun checkSession(): AuthResult<Boolean> {
        return when (val result = amplifyAuthWrapper.fetchAuthSession()) {
            is AuthSessionResult.Success -> AuthResult.Success(result.isSignedIn)
            is AuthSessionResult.Error -> AuthResult.Error(result.message)
        }
    }

    override suspend fun signIn(username: String, password: String): AuthResult<Unit> {
        return when (val result = amplifyAuthWrapper.signIn(username, password)) {
            is SignInResult.Success -> AuthResult.Success(Unit)
            is SignInResult.Error -> AuthResult.Error(result.message)
        }
    }

    override suspend fun getIdToken(): String? {
        return amplifyAuthWrapper.getIdToken()
    }

    override suspend fun signOut(): AuthResult<Unit> {
        return when (val result = amplifyAuthWrapper.signOut()) {
            is SignOutResult.Success -> AuthResult.Success(Unit)
            is SignOutResult.Error -> AuthResult.Error(result.message)
        }
    }
}
