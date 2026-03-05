package com.example.bloodpressuretracking.ui.main

/**
 * UI state for the Main screen (blood pressure input).
 */
data class MainUiState(
    val systolic: String = "",      // 最高血圧
    val diastolic: String = "",     // 最低血圧
    val pulse: String = "",         // 脈拍
    val isSubmitting: Boolean = false,
    val resultMessage: String? = null,
    val isError: Boolean = false,
    // OCR関連フィールド
    val isAnalyzing: Boolean = false,           // OCR処理中 (要件 2.2, 6.4)
    val isOcrFilled: Boolean = false,           // OCR自動入力済み (要件 4.1)
    val ocrErrorMessage: String? = null,        // OCRエラーメッセージ (要件 5.1)
    val showOcrRetryDialog: Boolean = false,    // 再撮影ダイアログ (要件 5.2)
    val cameraAvailable: Boolean = true         // カメラ利用可否 (要件 5.4, 6.2)
)
