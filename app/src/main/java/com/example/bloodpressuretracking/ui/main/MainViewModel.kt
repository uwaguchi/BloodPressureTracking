package com.example.bloodpressuretracking.ui.main

import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodpressuretracking.data.ocr.OcrFailureReason
import com.example.bloodpressuretracking.data.ocr.OcrRepository
import com.example.bloodpressuretracking.data.ocr.OcrResult
import com.example.bloodpressuretracking.data.repository.BloodPressureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CameraAction { RequestPermission, LaunchCamera }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bloodPressureRepository: BloodPressureRepository,
    private val ocrRepository: OcrRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        val hasCameraFeature = appContext.packageManager
            .hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
        _uiState.update { it.copy(cameraAvailable = hasCameraFeature) }
    }

    fun onSystolicChanged(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.update { it.copy(systolic = filtered, isOcrFilled = false) }
    }

    fun onDiastolicChanged(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.update { it.copy(diastolic = filtered, isOcrFilled = false) }
    }

    fun onPulseChanged(value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.update { it.copy(pulse = filtered, isOcrFilled = false) }
    }

    fun onSubmitClick() {
        val currentState = _uiState.value

        if (currentState.systolic.isBlank()) {
            _uiState.update { it.copy(resultMessage = "最高血圧を入力してください", isError = true) }
            return
        }
        if (currentState.diastolic.isBlank()) {
            _uiState.update { it.copy(resultMessage = "最低血圧を入力してください", isError = true) }
            return
        }
        if (currentState.pulse.isBlank()) {
            _uiState.update { it.copy(resultMessage = "脈拍を入力してください", isError = true) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, resultMessage = null, isError = false) }

        viewModelScope.launch {
            val result = bloodPressureRepository.submitRecord(
                systolic = currentState.systolic.toInt(),
                diastolic = currentState.diastolic.toInt(),
                pulse = currentState.pulse.toInt()
            )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            resultMessage = "登録が完了しました",
                            isError = false,
                            systolic = "",
                            diastolic = "",
                            pulse = "",
                            isOcrFilled = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            resultMessage = error.message ?: "エラーが発生しました",
                            isError = true
                        )
                    }
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(resultMessage = null, isError = false) }
    }

    // --- カメラ機能 ---

    fun onCameraButtonClick(hasPermission: Boolean): CameraAction {
        return if (hasPermission) CameraAction.LaunchCamera else CameraAction.RequestPermission
    }

    fun prepareCaptureUri(): Uri {
        val cacheDir = File(appContext.cacheDir, "ocr").also { it.mkdirs() }
        val imageFile = File(cacheDir, "temp_capture.jpg")
        return FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            imageFile
        )
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(
                resultMessage = "カメラ権限が必要です。設定から許可するか、手動で入力してください。",
                isError = true
            )
        }
    }

    fun onImageCaptured(uri: Uri?) {
        uri ?: return  // null はキャンセル（要件 1.5）

        _uiState.update { it.copy(isAnalyzing = true, showOcrRetryDialog = false, ocrErrorMessage = null) }

        viewModelScope.launch {
            when (val result = ocrRepository.analyzeImage(uri)) {
                is OcrResult.Success -> {
                    val values = result.values
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            systolic = values.systolic.toString(),
                            diastolic = values.diastolic.toString(),
                            pulse = values.pulse.toString(),
                            isOcrFilled = true
                        )
                    }
                }
                is OcrResult.Failure -> {
                    val base = when (result.reason) {
                        OcrFailureReason.NO_TEXT_DETECTED ->
                            "文字を検出できませんでした。\n明るい場所で血圧計の画面全体が映るよう撮影してください。"
                        OcrFailureReason.VALUES_OUT_OF_RANGE ->
                            "数値が正常範囲外です。\n血圧計の画面が鮮明に映るよう撮影してください。"
                        OcrFailureReason.INVALID_COMBINATION ->
                            "血圧値の組み合わせが無効です。\n再度撮影してください。"
                        OcrFailureReason.INSUFFICIENT_VALUES ->
                            "3つの数値を認識できませんでした。\n血圧計の画面全体が映るよう撮影してください。"
                    }
                    val rawInfo = if (result.rawText.isNullOrBlank()) {
                        "\n\n[OCR検出テキスト: なし]"
                    } else {
                        "\n\n[OCR検出テキスト]\n${result.rawText}"
                    }
                    val message = base + rawInfo
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            showOcrRetryDialog = true,
                            ocrErrorMessage = message
                        )
                    }
                }
            }
        }
    }

    fun onOcrRetry() {
        _uiState.update { it.copy(showOcrRetryDialog = false, ocrErrorMessage = null) }
    }

    fun onOcrDismiss() {
        _uiState.update { it.copy(showOcrRetryDialog = false, ocrErrorMessage = null) }
    }
}
