package com.example.bloodpressuretracking.data.repository

import com.example.bloodpressuretracking.data.api.ApiClient
import com.example.bloodpressuretracking.data.api.BloodPressureSubmitRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BloodPressureRepository.
 * Handles blood pressure data submission and retrieval.
 */
@Singleton
class BloodPressureRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val authRepository: AuthRepository
) : BloodPressureRepository {

    override suspend fun submitRecord(
        systolic: Int,
        diastolic: Int,
        pulse: Int
    ): Result<Unit> {
        // Get auth token
        val token = authRepository.getIdToken()
            ?: return Result.failure(BloodPressureException("認証が必要です"))

        // Create request
        val request = BloodPressureSubmitRequest(
            max = systolic,
            min = diastolic,
            bpm = pulse
        )

        // Submit to API
        return apiClient.postRecord(request, token)
    }

    override suspend fun fetchRecords(): Result<List<BloodPressureRecord>> {
        return apiClient.fetchRecordsFromS3().map { rawData ->
            parseRecords(rawData)
        }
    }

    /**
     * Parse raw text data from S3 into a list of BloodPressureRecord.
     * Expected format: "datetime,systolic,diastolic,pulse" per line
     * Invalid lines are skipped.
     */
    private fun parseRecords(rawData: String): List<BloodPressureRecord> {
        if (rawData.isBlank()) return emptyList()

        return rawData.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> parseLine(line) }
    }

    /**
     * Parse a single line into a BloodPressureRecord.
     * Expected format: date[TAB]time[TAB]systolic[TAB]diastolic[TAB]pulse
     * Example: 2023/01/01	09:24:20	113	78	84
     * @return BloodPressureRecord if parsing succeeds, null otherwise
     */
    private fun parseLine(line: String): BloodPressureRecord? {
        return try {
            val parts = line.split("\t")
            if (parts.size < 5) return null

            val date = parts[0].trim()
            val time = parts[1].trim()
            val systolic = parts[2].trim().toInt()
            val diastolic = parts[3].trim().toInt()
            val pulse = parts[4].trim().toInt()

            BloodPressureRecord(
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
                recordedAt = "$date $time"
            )
        } catch (e: Exception) {
            null // Skip malformed lines
        }
    }
}

/**
 * Exception for blood pressure related errors.
 */
class BloodPressureException(message: String) : Exception(message)
