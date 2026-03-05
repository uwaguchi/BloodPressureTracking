package com.example.bloodpressuretracking.data.ocr

sealed class OcrResult {
    data class Success(val values: BloodPressureValues) : OcrResult()
    data class Failure(val reason: OcrFailureReason, val rawText: String? = null) : OcrResult()
}

data class BloodPressureValues(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int
)

enum class OcrFailureReason {
    NO_TEXT_DETECTED,
    VALUES_OUT_OF_RANGE,
    INVALID_COMBINATION,
    INSUFFICIENT_VALUES
}
