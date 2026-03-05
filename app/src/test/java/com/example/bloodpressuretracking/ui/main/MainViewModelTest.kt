package com.example.bloodpressuretracking.ui.main

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.example.bloodpressuretracking.data.ocr.BloodPressureValues
import com.example.bloodpressuretracking.data.ocr.OcrFailureReason
import com.example.bloodpressuretracking.data.ocr.OcrRepository
import com.example.bloodpressuretracking.data.ocr.OcrResult
import com.example.bloodpressuretracking.data.repository.BloodPressureRecord
import com.example.bloodpressuretracking.data.repository.BloodPressureRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
    private lateinit var fakeOcrRepository: FakeOcrRepository
    private val testDispatcher = StandardTestDispatcher()

    private val mockPackageManager: PackageManager = mockk(relaxed = true) {
        every { hasSystemFeature(any()) } returns true
    }
    private val mockContext: Context = mockk(relaxed = true) {
        every { packageManager } returns mockPackageManager
        every { packageName } returns "com.example.bloodpressuretracking"
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeBloodPressureRepository = FakeBloodPressureRepository()
        fakeOcrRepository = FakeOcrRepository()
        viewModel = MainViewModel(fakeBloodPressureRepository, fakeOcrRepository, mockContext)
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
        assertFalse(state.isAnalyzing)
        assertFalse(state.isOcrFilled)
        assertNull(state.ocrErrorMessage)
        assertFalse(state.showOcrRetryDialog)
        assertTrue(state.cameraAvailable)
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

    // OCR入力フラグのクリアテスト

    @Test
    fun `onSystolicChanged clears isOcrFilled`() = runTest {
        fakeOcrRepository.setResult(OcrResult.Success(BloodPressureValues(120, 80, 70)))
        viewModel.onImageCaptured(mockk())
        advanceUntilIdle()

        viewModel.onSystolicChanged("130")

        val state = viewModel.uiState.first()
        assertFalse(state.isOcrFilled)
    }

    @Test
    fun `onDiastolicChanged clears isOcrFilled`() = runTest {
        fakeOcrRepository.setResult(OcrResult.Success(BloodPressureValues(120, 80, 70)))
        viewModel.onImageCaptured(mockk())
        advanceUntilIdle()

        viewModel.onDiastolicChanged("90")

        val state = viewModel.uiState.first()
        assertFalse(state.isOcrFilled)
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

    // カメラアクションテスト

    @Test
    fun `onCameraButtonClick returns LaunchCamera when permission granted`() {
        val action = viewModel.onCameraButtonClick(hasPermission = true)
        assertEquals(CameraAction.LaunchCamera, action)
    }

    @Test
    fun `onCameraButtonClick returns RequestPermission when permission not granted`() {
        val action = viewModel.onCameraButtonClick(hasPermission = false)
        assertEquals(CameraAction.RequestPermission, action)
    }

    @Test
    fun `onPermissionDenied shows error message`() = runTest {
        viewModel.onPermissionDenied()

        val state = viewModel.uiState.first()
        assertTrue(state.isError)
        assertTrue(state.resultMessage?.contains("カメラ権限") == true)
    }

    // OCR処理テスト

    @Test
    fun `onImageCaptured with null uri does not change state`() = runTest {
        viewModel.onImageCaptured(null)

        val state = viewModel.uiState.first()
        assertFalse(state.isAnalyzing)
        assertFalse(state.isOcrFilled)
    }

    @Test
    fun `onImageCaptured sets isAnalyzing true during processing`() = runTest {
        fakeOcrRepository.setResult(OcrResult.Success(BloodPressureValues(120, 80, 70)))

        viewModel.onImageCaptured(mockk())

        val state = viewModel.uiState.first()
        assertTrue(state.isAnalyzing)
    }

    @Test
    fun `onImageCaptured fills form fields on OCR success`() = runTest {
        fakeOcrRepository.setResult(OcrResult.Success(BloodPressureValues(120, 80, 70)))

        viewModel.onImageCaptured(mockk())
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isAnalyzing)
        assertEquals("120", state.systolic)
        assertEquals("80", state.diastolic)
        assertEquals("70", state.pulse)
        assertTrue(state.isOcrFilled)
    }

    @Test
    fun `onImageCaptured shows retry dialog on OCR failure`() = runTest {
        fakeOcrRepository.setResult(OcrResult.Failure(OcrFailureReason.NO_TEXT_DETECTED))

        viewModel.onImageCaptured(mockk())
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isAnalyzing)
        assertTrue(state.showOcrRetryDialog)
        assertTrue(state.ocrErrorMessage?.isNotEmpty() == true)
        assertFalse(state.isOcrFilled)
    }

    @Test
    fun `onOcrRetry clears retry dialog`() = runTest {
        fakeOcrRepository.setResult(OcrResult.Failure(OcrFailureReason.INSUFFICIENT_VALUES))
        viewModel.onImageCaptured(mockk())
        advanceUntilIdle()

        viewModel.onOcrRetry()

        val state = viewModel.uiState.first()
        assertFalse(state.showOcrRetryDialog)
        assertNull(state.ocrErrorMessage)
    }

    @Test
    fun `onOcrDismiss clears retry dialog`() = runTest {
        fakeOcrRepository.setResult(OcrResult.Failure(OcrFailureReason.INSUFFICIENT_VALUES))
        viewModel.onImageCaptured(mockk())
        advanceUntilIdle()

        viewModel.onOcrDismiss()

        val state = viewModel.uiState.first()
        assertFalse(state.showOcrRetryDialog)
        assertNull(state.ocrErrorMessage)
    }
}

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

class FakeOcrRepository : OcrRepository {
    private var result: OcrResult = OcrResult.Failure(OcrFailureReason.NO_TEXT_DETECTED)

    fun setResult(ocrResult: OcrResult) {
        result = ocrResult
    }

    override suspend fun analyzeImage(uri: Uri): OcrResult = result
}
