package com.example.bloodpressuretracking.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClientImpl @Inject constructor(
    private val okHttpClient: OkHttpClient
) : ApiClient {

    companion object {
        private const val API_BASE_URL = "https://3eno4n6y1d.execute-api.ap-northeast-1.amazonaws.com/prod/api"
        private const val ADD_RECORD_ENDPOINT = "$API_BASE_URL/add-blood-pressure-record"
        private const val S3_URL = "https://s3-ap-northeast-1.amazonaws.com/uwaguchi/blood-pressure/blood-pressure.txt"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    override suspend fun postRecord(
        data: BloodPressureSubmitRequest,
        authToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("max", data.max)
                put("min", data.min)
                put("bpm", data.bpm)
            }.toString()

            val request = Request.Builder()
                .url(ADD_RECORD_ENDPOINT)
                .header("Authorization", "Bearer $authToken")
                .header("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(ApiException("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchRecordsFromS3(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(S3_URL)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Result.success(body)
            } else {
                Result.failure(ApiException("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ApiException(message: String) : Exception(message)
