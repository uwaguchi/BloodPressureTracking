package com.example.bloodpressuretracking.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var fakeAmplifyAuthWrapper: FakeAmplifyAuthWrapper

    @Before
    fun setup() {
        fakeAmplifyAuthWrapper = FakeAmplifyAuthWrapper()
        authRepository = AuthRepositoryImpl(fakeAmplifyAuthWrapper)
    }

    // セッション確認テスト

    @Test
    fun `checkSession returns Success true when session is valid`() = runTest {
        fakeAmplifyAuthWrapper.setSessionValid(true)

        val result = authRepository.checkSession()

        assertTrue(result is AuthResult.Success)
        assertEquals(true, (result as AuthResult.Success).data)
    }

    @Test
    fun `checkSession returns Success false when session is invalid`() = runTest {
        fakeAmplifyAuthWrapper.setSessionValid(false)

        val result = authRepository.checkSession()

        assertTrue(result is AuthResult.Success)
        assertEquals(false, (result as AuthResult.Success).data)
    }

    @Test
    fun `checkSession returns Error when exception occurs`() = runTest {
        fakeAmplifyAuthWrapper.setSessionError("Session check failed")

        val result = authRepository.checkSession()

        assertTrue(result is AuthResult.Error)
        assertEquals("Session check failed", (result as AuthResult.Error).message)
    }

    // サインインテスト

    @Test
    fun `signIn returns Success when credentials are valid`() = runTest {
        fakeAmplifyAuthWrapper.setSignInSuccess(true)

        val result = authRepository.signIn("testuser", "password123")

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun `signIn returns Error when credentials are invalid`() = runTest {
        fakeAmplifyAuthWrapper.setSignInSuccess(false)
        fakeAmplifyAuthWrapper.setSignInError("Invalid username or password")

        val result = authRepository.signIn("wronguser", "wrongpass")

        assertTrue(result is AuthResult.Error)
        assertEquals("Invalid username or password", (result as AuthResult.Error).message)
    }

    @Test
    fun `signIn returns Error when network error occurs`() = runTest {
        fakeAmplifyAuthWrapper.setSignInError("Network error")

        val result = authRepository.signIn("testuser", "password123")

        assertTrue(result is AuthResult.Error)
        assertEquals("Network error", (result as AuthResult.Error).message)
    }

    // IDトークン取得テスト

    @Test
    fun `getIdToken returns token when session is valid`() = runTest {
        fakeAmplifyAuthWrapper.setIdToken("valid-id-token-123")

        val token = authRepository.getIdToken()

        assertNotNull(token)
        assertEquals("valid-id-token-123", token)
    }

    @Test
    fun `getIdToken returns null when session is invalid`() = runTest {
        fakeAmplifyAuthWrapper.setIdToken(null)

        val token = authRepository.getIdToken()

        assertNull(token)
    }

    // サインアウトテスト

    @Test
    fun `signOut returns Success when signout succeeds`() = runTest {
        fakeAmplifyAuthWrapper.setSignOutSuccess(true)

        val result = authRepository.signOut()

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun `signOut returns Error when signout fails`() = runTest {
        fakeAmplifyAuthWrapper.setSignOutSuccess(false)
        fakeAmplifyAuthWrapper.setSignOutError("SignOut failed")

        val result = authRepository.signOut()

        assertTrue(result is AuthResult.Error)
        assertEquals("SignOut failed", (result as AuthResult.Error).message)
    }
}

/**
 * Fake implementation of AmplifyAuthWrapper for testing
 */
class FakeAmplifyAuthWrapper : AmplifyAuthWrapper {
    private var sessionValid: Boolean = false
    private var sessionError: String? = null
    private var signInSuccess: Boolean = false
    private var signInError: String? = null
    private var idToken: String? = null
    private var signOutSuccess: Boolean = true
    private var signOutError: String? = null

    fun setSessionValid(valid: Boolean) {
        sessionValid = valid
        sessionError = null
    }

    fun setSessionError(error: String) {
        sessionError = error
    }

    fun setSignInSuccess(success: Boolean) {
        signInSuccess = success
        if (success) signInError = null
    }

    fun setSignInError(error: String) {
        signInError = error
        signInSuccess = false
    }

    fun setIdToken(token: String?) {
        idToken = token
    }

    fun setSignOutSuccess(success: Boolean) {
        signOutSuccess = success
        if (success) signOutError = null
    }

    fun setSignOutError(error: String) {
        signOutError = error
        signOutSuccess = false
    }

    override suspend fun fetchAuthSession(): AuthSessionResult {
        sessionError?.let { return AuthSessionResult.Error(it) }
        return AuthSessionResult.Success(sessionValid)
    }

    override suspend fun signIn(username: String, password: String): SignInResult {
        signInError?.let { return SignInResult.Error(it) }
        return if (signInSuccess) SignInResult.Success else SignInResult.Error("Sign in failed")
    }

    override suspend fun getIdToken(): String? {
        return idToken
    }

    override suspend fun signOut(): SignOutResult {
        signOutError?.let { return SignOutResult.Error(it) }
        return if (signOutSuccess) SignOutResult.Success else SignOutResult.Error("Sign out failed")
    }
}
