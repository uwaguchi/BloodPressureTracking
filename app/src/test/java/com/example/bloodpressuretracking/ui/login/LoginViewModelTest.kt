package com.example.bloodpressuretracking.ui.login

import com.example.bloodpressuretracking.data.repository.AuthRepository
import com.example.bloodpressuretracking.data.repository.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private lateinit var viewModel: LoginViewModel
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = LoginViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 初期状態テスト

    @Test
    fun `initial state has empty username and password`() = runTest {
        val state = viewModel.uiState.first()

        assertEquals("", state.username)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isLoginSuccess)
    }

    // ユーザー名更新テスト

    @Test
    fun `onUsernameChanged updates username in state`() = runTest {
        viewModel.onUsernameChanged("testuser")

        val state = viewModel.uiState.first()
        assertEquals("testuser", state.username)
    }

    // パスワード更新テスト

    @Test
    fun `onPasswordChanged updates password in state`() = runTest {
        viewModel.onPasswordChanged("password123")

        val state = viewModel.uiState.first()
        assertEquals("password123", state.password)
    }

    // ログイン成功テスト

    @Test
    fun `onLoginClick sets isLoading to true during login`() = runTest {
        viewModel.onUsernameChanged("testuser")
        viewModel.onPasswordChanged("password123")
        fakeAuthRepository.setSignInSuccess(true)

        viewModel.onLoginClick()

        // Loading state should be true before completing
        val loadingState = viewModel.uiState.first()
        assertTrue(loadingState.isLoading)
    }

    @Test
    fun `onLoginClick sets isLoginSuccess to true when login succeeds`() = runTest {
        viewModel.onUsernameChanged("testuser")
        viewModel.onPasswordChanged("password123")
        fakeAuthRepository.setSignInSuccess(true)

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state.isLoginSuccess)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    // ログイン失敗テスト

    @Test
    fun `onLoginClick sets errorMessage when login fails`() = runTest {
        viewModel.onUsernameChanged("testuser")
        viewModel.onPasswordChanged("wrongpass")
        fakeAuthRepository.setSignInError("ユーザー名またはパスワードが正しくありません")

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isLoginSuccess)
        assertFalse(state.isLoading)
        assertEquals("ユーザー名またはパスワードが正しくありません", state.errorMessage)
    }

    // 入力バリデーションテスト

    @Test
    fun `onLoginClick sets error when username is empty`() = runTest {
        viewModel.onPasswordChanged("password123")

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("ユーザー名を入力してください", state.errorMessage)
    }

    @Test
    fun `onLoginClick sets error when password is empty`() = runTest {
        viewModel.onUsernameChanged("testuser")

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("パスワードを入力してください", state.errorMessage)
    }

    // エラークリアテスト

    @Test
    fun `clearError clears error message`() = runTest {
        viewModel.onUsernameChanged("testuser")
        viewModel.onLoginClick()
        advanceUntilIdle()

        viewModel.clearError()

        val state = viewModel.uiState.first()
        assertNull(state.errorMessage)
    }
}

/**
 * Fake implementation of AuthRepository for testing
 */
class FakeAuthRepository : AuthRepository {
    private var signInSuccess = true
    private var signInError: String? = null
    private var sessionValid = false
    private var idToken: String? = null

    fun setSignInSuccess(success: Boolean) {
        signInSuccess = success
        if (success) signInError = null
    }

    fun setSignInError(error: String) {
        signInError = error
        signInSuccess = false
    }

    fun setSessionValid(valid: Boolean) {
        sessionValid = valid
    }

    fun setIdToken(token: String?) {
        idToken = token
    }

    override suspend fun checkSession(): AuthResult<Boolean> {
        return AuthResult.Success(sessionValid)
    }

    override suspend fun signIn(username: String, password: String): AuthResult<Unit> {
        return if (signInSuccess && signInError == null) {
            AuthResult.Success(Unit)
        } else {
            AuthResult.Error(signInError ?: "Sign in failed")
        }
    }

    override suspend fun getIdToken(): String? {
        return idToken
    }

    override suspend fun signOut(): AuthResult<Unit> {
        return AuthResult.Success(Unit)
    }
}
