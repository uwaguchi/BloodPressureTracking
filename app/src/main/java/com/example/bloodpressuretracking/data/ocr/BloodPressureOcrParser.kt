package com.example.bloodpressuretracking.data.ocr

import javax.inject.Inject

class BloodPressureOcrParser @Inject constructor() {

    companion object {
        const val SYSTOLIC_MIN = 60;  const val SYSTOLIC_MAX = 300
        const val DIASTOLIC_MIN = 30; const val DIASTOLIC_MAX = 200
        const val PULSE_MIN = 20;     const val PULSE_MAX = 300
    }

    fun parse(text: String): OcrResult {
        if (text.isBlank()) return OcrResult.Failure(OcrFailureReason.NO_TEXT_DETECTED)

        // Strategy 1: slash pattern "systolic/diastolic"
        val slashMatch = Regex("""(\d+)/(\d+)""").find(text)
        if (slashMatch != null) {
            val systolic = slashMatch.groupValues[1].toIntOrNull()
                ?: return OcrResult.Failure(OcrFailureReason.INSUFFICIENT_VALUES)
            val diastolic = slashMatch.groupValues[2].toIntOrNull()
                ?: return OcrResult.Failure(OcrFailureReason.INSUFFICIENT_VALUES)

            val textWithoutSlash = text.removeRange(slashMatch.range)
            val remainingNumbers = Regex("""\d+""").findAll(textWithoutSlash)
                .mapNotNull { it.value.toIntOrNull() }
                .toList()

            if (remainingNumbers.isEmpty()) return OcrResult.Failure(OcrFailureReason.INSUFFICIENT_VALUES)

            // Prefer number in pulse range, otherwise take first
            val pulse = remainingNumbers.firstOrNull { it in PULSE_MIN..PULSE_MAX }
                ?: remainingNumbers.first()

            return validateAndCreate(systolic, diastolic, pulse)
        }

        // Strategy 2: take first 3 numbers by position
        val allNumbers = Regex("""\d+""").findAll(text)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()

        if (allNumbers.size < 3) return OcrResult.Failure(OcrFailureReason.INSUFFICIENT_VALUES)

        return validateAndCreate(allNumbers[0], allNumbers[1], allNumbers[2])
    }

    private fun validateAndCreate(systolic: Int, diastolic: Int, pulse: Int): OcrResult {
        if (systolic < SYSTOLIC_MIN || systolic > SYSTOLIC_MAX) {
            return OcrResult.Failure(OcrFailureReason.VALUES_OUT_OF_RANGE)
        }
        if (diastolic < DIASTOLIC_MIN || diastolic > DIASTOLIC_MAX) {
            return OcrResult.Failure(OcrFailureReason.VALUES_OUT_OF_RANGE)
        }
        if (pulse < PULSE_MIN || pulse > PULSE_MAX) {
            return OcrResult.Failure(OcrFailureReason.VALUES_OUT_OF_RANGE)
        }
        if (systolic <= diastolic) {
            return OcrResult.Failure(OcrFailureReason.INVALID_COMBINATION)
        }
        return OcrResult.Success(BloodPressureValues(systolic, diastolic, pulse))
    }
}
