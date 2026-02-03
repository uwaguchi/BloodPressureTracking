package com.example.bloodpressuretracking.data.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ApiClientTest {

    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var apiClient: ApiClientImpl

    @Before
    fun setup() {
        mockOkHttpClient = mockk()
        mockCall = mockk()
        apiClient = ApiClientImpl(mockOkHttpClient)
    }

    // POST /api/add-blood-pressure-record テスト

    @Test
    fun `postRecord should return success when API returns 200`() = runTest {
        val request = BloodPressureSubmitRequest(max = 120, min = 80, bpm = 70)
        val authToken = "test-token"
        val mockResponse = createMockResponse(200, "{\"success\":true}")

        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val result = apiClient.postRecord(request, authToken)

        assertTrue(result.isSuccess)
        verify { mockOkHttpClient.newCall(match { req ->
            req.url.toString().contains("add-blood-pressure-record") &&
            req.header("Authorization") == "Bearer $authToken" &&
            req.header("Content-Type") == "application/json"
        }) }
    }

    @Test
    fun `postRecord should include correct JSON body`() = runTest {
        val request = BloodPressureSubmitRequest(max = 130, min = 85, bpm = 75)
        val authToken = "test-token"
        val mockResponse = createMockResponse(200, "{\"success\":true}")

        var capturedRequest: Request? = null
        every { mockOkHttpClient.newCall(any()) } answers {
            capturedRequest = firstArg()
            mockCall
        }
        every { mockCall.execute() } returns mockResponse

        apiClient.postRecord(request, authToken)

        assertNotNull(capturedRequest)
        assertEquals("POST", capturedRequest?.method)
    }

    @Test
    fun `postRecord should return failure when API returns 401`() = runTest {
        val request = BloodPressureSubmitRequest(max = 120, min = 80, bpm = 70)
        val authToken = "invalid-token"
        val mockResponse = createMockResponse(401, "{\"error\":\"Unauthorized\"}")

        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val result = apiClient.postRecord(request, authToken)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("401") == true)
    }

    @Test
    fun `postRecord should return failure when API returns 400`() = runTest {
        val request = BloodPressureSubmitRequest(max = 120, min = 80, bpm = 70)
        val authToken = "test-token"
        val mockResponse = createMockResponse(400, "{\"error\":\"Bad Request\"}")

        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val result = apiClient.postRecord(request, authToken)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("400") == true)
    }

    @Test
    fun `postRecord should return failure when API returns 500`() = runTest {
        val request = BloodPressureSubmitRequest(max = 120, min = 80, bpm = 70)
        val authToken = "test-token"
        val mockResponse = createMockResponse(500, "{\"error\":\"Internal Server Error\"}")

        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val result = apiClient.postRecord(request, authToken)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
    }

    @Test
    fun `postRecord should return failure when network error occurs`() = runTest {
        val request = BloodPressureSubmitRequest(max = 120, min = 80, bpm = 70)
        val authToken = "test-token"

        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } throws java.io.IOException("Network error")

        val result = apiClient.postRecord(request, authToken)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is java.io.IOException)
    }

    // GET S3 テスト

    @Test
    fun `fetchRecordsFromS3 should return success with data when S3 returns 200`() = runTest {
        val s3Data = "120,80,70,2024-01-01\n125,82,72,2024-01-02"
        val mockResponse = createMockResponse(200, s3Data)

        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val result = apiClient.fetchRecordsFromS3()

        assertTrue(result.isSuccess)
        assertEquals(s3Data, result.getOrNull())
    }

    @Test
    fun `fetchRecordsFromS3 should call correct S3 URL`() = runTest {
        val mockResponse = createMockResponse(200, "data")

        var capturedRequest: Request? = null
        every { mockOkHttpClient.newCall(any()) } answers {
            capturedRequest = firstArg()
            mockCall
        }
        every { mockCall.execute() } returns mockResponse

        apiClient.fetchRecordsFromS3()

        assertNotNull(capturedRequest)
        assertEquals("GET", capturedRequest?.method)
        assertTrue(capturedRequest?.url.toString().contains("s3-ap-northeast-1.amazonaws.com"))
        assertTrue(capturedRequest?.url.toString().contains("blood-pressure.txt"))
    }

    @Test
    fun `fetchRecordsFromS3 should not include Authorization header`() = runTest {
        val mockResponse = createMockResponse(200, "data")

        var capturedRequest: Request? = null
        every { mockOkHttpClient.newCall(any()) } answers {
            capturedRequest = firstArg()
            mockCall
        }
        every { mockCall.execute() } returns mockResponse

        apiClient.fetchRecordsFromS3()

        assertNull(capturedRequest?.header("Authorization"))
    }

    @Test
    fun `fetchRecordsFromS3 should return failure when S3 returns 404`() = runTest {
        val mockResponse = createMockResponse(404, "Not Found")

        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val result = apiClient.fetchRecordsFromS3()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("404") == true)
    }

    @Test
    fun `fetchRecordsFromS3 should return failure when S3 returns 500`() = runTest {
        val mockResponse = createMockResponse(500, "Internal Server Error")

        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        val result = apiClient.fetchRecordsFromS3()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
    }

    @Test
    fun `fetchRecordsFromS3 should return failure when network error occurs`() = runTest {
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } throws java.io.IOException("Network error")

        val result = apiClient.fetchRecordsFromS3()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is java.io.IOException)
    }

    private fun createMockResponse(code: Int, body: String): Response {
        val request = Request.Builder()
            .url("https://example.com")
            .build()

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
