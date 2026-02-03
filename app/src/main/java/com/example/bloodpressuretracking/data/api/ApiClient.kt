package com.example.bloodpressuretracking.data.api

interface ApiClient {
    suspend fun postRecord(
        data: BloodPressureSubmitRequest,
        authToken: String
    ): Result<Unit>

    suspend fun fetchRecordsFromS3(): Result<String>
}

data class BloodPressureSubmitRequest(
    val max: Int,
    val min: Int,
    val bpm: Int
)
