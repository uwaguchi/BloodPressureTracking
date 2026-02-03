package com.example.bloodpressuretracking.data.repository

/**
 * Repository interface for blood pressure data operations.
 * Handles submitting records to API and fetching records from S3.
 */
interface BloodPressureRepository {
    /**
     * Submit a blood pressure record to the API.
     * @param systolic Systolic blood pressure (最高血圧)
     * @param diastolic Diastolic blood pressure (最低血圧)
     * @param pulse Pulse rate (脈拍)
     * @return Result<Unit> - Success on successful submission, Failure on error
     */
    suspend fun submitRecord(
        systolic: Int,
        diastolic: Int,
        pulse: Int
    ): Result<Unit>

    /**
     * Fetch all blood pressure records from S3.
     * @return Result<List<BloodPressureRecord>> - List of records on success, Failure on error
     */
    suspend fun fetchRecords(): Result<List<BloodPressureRecord>>
}

/**
 * Data class representing a blood pressure record.
 */
data class BloodPressureRecord(
    val systolic: Int,      // 最高血圧 (max)
    val diastolic: Int,     // 最低血圧 (min)
    val pulse: Int,         // 脈拍 (bpm)
    val recordedAt: String? = null // 記録日時（あれば）
)
