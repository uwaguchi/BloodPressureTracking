package com.example.bloodpressuretracking.ui.main

import com.example.bloodpressuretracking.data.repository.BloodPressureRecord
import com.example.bloodpressuretracking.data.repository.BloodPressureRepository
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
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var fakeBloodPressureRepository: FakeBloodPressureRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeBloodPressureRepository = FakeBloodPressureRepository()
        viewModel = MainViewModel(fakeBloodPressureRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 初期状態テスト

    @Test
    fun `initial state has empty input fields`() = runTest {
        val state = viewModel.uiState.first()

        assertEquals("", state.systolic)
        assertEquals("", state.diastolic)
        assertEquals("", state.pulse)
        assertFalse(state.isSubmitting)
        assertNull(state.resultMessage)
        assertFalse(state.isError)
    }

    // 入力値更新テスト

    @Test
    fun `onSystolicChanged updates systolic in state`() = runTest {
        viewModel.onSystolicChanged("120")

        val state = viewModel.uiState.first()
        assertEquals("120", state.systolic)
    }

    @Test
    fun `onDiastolicChanged updates diastolic in state`() = runTest {
        viewModel.onDiastolicChanged("80")

        val state = viewModel.uiState.first()
        assertEquals("80", state.diastolic)
    }

    @Test
    fun `onPulseChanged updates pulse in state`() = runTest {
        viewModel.onPulseChanged("70")

        val state = viewModel.uiState.first()
        assertEquals("70", state.pulse)
    }

    // 数値以外の入力を無視するテスト

    @Test
    fun `onSystolicChanged ignores non-numeric input`() = runTest {
        viewModel.onSystolicChanged("abc")

        val state = viewModel.uiState.first()
        assertEquals("", state.systolic)
    }

    @Test
    fun `onSystolicChanged accepts only digits`() = runTest {
        viewModel.onSystolicChanged("12a3")

        val state = viewModel.uiState.first()
        assertEquals("123", state.systolic)
    }

    // 送信成功テスト

    @Test
    fun `onSubmitClick sets isSubmitting to true during submission`() = runTest {
        viewModel.onSystolicChanged("120")
        viewModel.onDiastolicChanged("80")
        viewModel.onPulseChanged("70")
        fakeBloodPressureRepository.setSubmitSuccess(true)

        viewModel.onSubmitClick()

        val state = viewModel.uiState.first()
        assertTrue(state.isSubmitting)
    }

    @Test
    fun `onSubmitClick shows success message when submission succeeds`() = runTest {
        viewModel.onSystolicChanged("120")
        viewModel.onDiastolicChanged("80")
        viewModel.onPulseChanged("70")
        fakeBloodPressureRepository.setSubmitSuccess(true)

        viewModel.onSubmitClick()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isSubmitting)
        assertFalse(state.isError)
        assertEquals("登録が完了しました", state.resultMessage)
    }

    @Test
    fun `onSubmitClick clears input fields after successful submission`() = runTest {
        viewModel.onSystolicChanged("120")
        viewModel.onDiastolicChanged("80")
        viewModel.onPulseChanged("70")
        fakeBloodPressureRepository.setSubmitSuccess(true)

        viewModel.onSubmitClick()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("", state.systolic)
        assertEquals("", state.diastolic)
        assertEquals("", state.pulse)
    }

    // 送信失敗テスト

    @Test
    fun `onSubmitClick shows error message when submission fails`() = runTest {
        viewModel.onSystolicChanged("120")
        viewModel.onDiastolicChanged("80")
        viewModel.onPulseChanged("70")
        fakeBloodPressureRepository.setSubmitError("通信エラーが発生しました")

        viewModel.onSubmitClick()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isSubmitting)
        assertTrue(state.isError)
        assertEquals("通信エラーが発生しました", state.resultMessage)
    }

    // 入力バリデーションテスト

    @Test
    fun `onSubmitClick shows error when systolic is empty`() = runTest {
        viewModel.onDiastolicChanged("80")
        viewModel.onPulseChanged("70")

        viewModel.onSubmitClick()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state.isError)
        assertEquals("最高血圧を入力してください", state.resultMessage)
    }

    @Test
    fun `onSubmitClick shows error when diastolic is empty`() = runTest {
        viewModel.onSystolicChanged("120")
        viewModel.onPulseChanged("70")

        viewModel.onSubmitClick()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state.isError)
        assertEquals("最低血圧を入力してください", state.resultMessage)
    }

    @Test
    fun `onSubmitClick shows error when pulse is empty`() = runTest {
        viewModel.onSystolicChanged("120")
        viewModel.onDiastolicChanged("80")

        viewModel.onSubmitClick()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state.isError)
        assertEquals("脈拍を入力してください", state.resultMessage)
    }

    // メッセージクリアテスト

    @Test
    fun `clearMessage clears result message`() = runTest {
        viewModel.onSystolicChanged("120")
        viewModel.onDiastolicChanged("80")
        viewModel.onPulseChanged("70")
        fakeBloodPressureRepository.setSubmitSuccess(true)
        viewModel.onSubmitClick()
        advanceUntilIdle()

        viewModel.clearMessage()

        val state = viewModel.uiState.first()
        assertNull(state.resultMessage)
    }
}

/**
 * Fake implementation of BloodPressureRepository for testing
 */
class FakeBloodPressureRepository : BloodPressureRepository {
    private var submitSuccess = true
    private var submitError: String? = null
    private var records: List<BloodPressureRecord> = emptyList()

    fun setSubmitSuccess(success: Boolean) {
        submitSuccess = success
        if (success) submitError = null
    }

    fun setSubmitError(error: String) {
        submitError = error
        submitSuccess = false
    }

    fun setRecords(list: List<BloodPressureRecord>) {
        records = list
    }

    override suspend fun submitRecord(systolic: Int, diastolic: Int, pulse: Int): Result<Unit> {
        return if (submitSuccess && submitError == null) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(submitError ?: "Unknown error"))
        }
    }

    override suspend fun fetchRecords(): Result<List<BloodPressureRecord>> {
        return Result.success(records)
    }
}
