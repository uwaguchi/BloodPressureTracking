package com.example.bloodpressuretracking.ui

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private lateinit var viewModel: AppViewModel
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 初期状態テスト

    @Test
    fun `initial state is checking session`() = runTest {
        viewModel = AppViewModel(fakeAuthRepository)

        val state = viewModel.uiState.first()

        assertTrue(state.isCheckingSession)
        assertFalse(state.isAuthenticated)
    }

    // セッション確認テスト

    @Test
    fun `checkSession sets isAuthenticated to true when session is valid`() = runTest {
        fakeAuthRepository.setSessionValid(true)
        viewModel = AppViewModel(fakeAuthRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isCheckingSession)
        assertTrue(state.isAuthenticated)
    }

    @Test
    fun `checkSession sets isAuthenticated to false when session is invalid`() = runTest {
        fakeAuthRepository.setSessionValid(false)
        viewModel = AppViewModel(fakeAuthRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isCheckingSession)
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun `checkSession sets isAuthenticated to false on error`() = runTest {
        fakeAuthRepository.setSessionError("Session check failed")
        viewModel = AppViewModel(fakeAuthRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isCheckingSession)
        assertFalse(state.isAuthenticated)
    }

    // ログイン完了通知テスト

    @Test
    fun `onLoginSuccess sets isAuthenticated to true`() = runTest {
        fakeAuthRepository.setSessionValid(false)
        viewModel = AppViewModel(fakeAuthRepository)
        advanceUntilIdle()

        viewModel.onLoginSuccess()

        val state = viewModel.uiState.first()
        assertTrue(state.isAuthenticated)
    }
}

/**
 * Fake implementation of AuthRepository for testing
 */
class FakeAuthRepository : AuthRepository {
    private var sessionValid = false
    private var sessionError: String? = null

    fun setSessionValid(valid: Boolean) {
        sessionValid = valid
        sessionError = null
    }

    fun setSessionError(error: String) {
        sessionError = error
    }

    override suspend fun checkSession(): AuthResult<Boolean> {
        sessionError?.let { return AuthResult.Error(it) }
        return AuthResult.Success(sessionValid)
    }

    override suspend fun signIn(username: String, password: String): AuthResult<Unit> {
        return AuthResult.Success(Unit)
    }

    override suspend fun getIdToken(): String? {
        return if (sessionValid) "token" else null
    }

    override suspend fun signOut(): AuthResult<Unit> {
        return AuthResult.Success(Unit)
    }
}
