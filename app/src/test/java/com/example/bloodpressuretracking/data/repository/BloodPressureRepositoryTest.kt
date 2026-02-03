package com.example.bloodpressuretracking.data.repository

import com.example.bloodpressuretracking.data.api.ApiClient
import com.example.bloodpressuretracking.data.api.BloodPressureSubmitRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BloodPressureRepositoryTest {

    private lateinit var bloodPressureRepository: BloodPressureRepository
    private lateinit var fakeApiClient: FakeApiClient
    private lateinit var fakeAuthRepository: FakeAuthRepository

    @Before
    fun setup() {
        fakeApiClient = FakeApiClient()
        fakeAuthRepository = FakeAuthRepository()
        bloodPressureRepository = BloodPressureRepositoryImpl(fakeApiClient, fakeAuthRepository)
    }

    // データ送信テスト

    @Test
    fun `submitRecord returns success when API call succeeds`() = runTest {
        fakeAuthRepository.setIdToken("valid-token")
        fakeApiClient.setPostRecordSuccess(true)

        val result = bloodPressureRepository.submitRecord(120, 80, 70)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `submitRecord sends correct data to API`() = runTest {
        fakeAuthRepository.setIdToken("valid-token")
        fakeApiClient.setPostRecordSuccess(true)

        bloodPressureRepository.submitRecord(130, 85, 72)

        val lastRequest = fakeApiClient.lastPostRequest
        assertEquals(130, lastRequest?.max)
        assertEquals(85, lastRequest?.min)
        assertEquals(72, lastRequest?.bpm)
    }

    @Test
    fun `submitRecord includes auth token in request`() = runTest {
        fakeAuthRepository.setIdToken("my-auth-token")
        fakeApiClient.setPostRecordSuccess(true)

        bloodPressureRepository.submitRecord(120, 80, 70)

        assertEquals("my-auth-token", fakeApiClient.lastAuthToken)
    }

    @Test
    fun `submitRecord returns failure when API call fails`() = runTest {
        fakeAuthRepository.setIdToken("valid-token")
        fakeApiClient.setPostRecordSuccess(false)
        fakeApiClient.setPostRecordError("Server error")

        val result = bloodPressureRepository.submitRecord(120, 80, 70)

        assertTrue(result.isFailure)
        assertEquals("Server error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `submitRecord returns failure when no auth token available`() = runTest {
        fakeAuthRepository.setIdToken(null)

        val result = bloodPressureRepository.submitRecord(120, 80, 70)

        assertTrue(result.isFailure)
        assertEquals("認証が必要です", result.exceptionOrNull()?.message)
    }

    // データ取得テスト

    @Test
    fun `fetchRecords returns parsed records when S3 call succeeds`() = runTest {
        val s3Data = """
            2024-01-15 08:30,120,80,70
            2024-01-16 09:00,125,82,72
        """.trimIndent()
        fakeApiClient.setFetchRecordsSuccess(s3Data)

        val result = bloodPressureRepository.fetchRecords()

        assertTrue(result.isSuccess)
        val records = result.getOrNull()!!
        assertEquals(2, records.size)
        assertEquals(120, records[0].systolic)
        assertEquals(80, records[0].diastolic)
        assertEquals(70, records[0].pulse)
        assertEquals("2024-01-15 08:30", records[0].recordedAt)
    }

    @Test
    fun `fetchRecords returns empty list when S3 returns empty data`() = runTest {
        fakeApiClient.setFetchRecordsSuccess("")

        val result = bloodPressureRepository.fetchRecords()

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `fetchRecords returns failure when S3 call fails`() = runTest {
        fakeApiClient.setFetchRecordsError("Network error")

        val result = bloodPressureRepository.fetchRecords()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fetchRecords handles malformed data gracefully`() = runTest {
        val malformedData = """
            2024-01-15 08:30,120,80,70
            invalid line
            2024-01-16 09:00,125,82,72
        """.trimIndent()
        fakeApiClient.setFetchRecordsSuccess(malformedData)

        val result = bloodPressureRepository.fetchRecords()

        assertTrue(result.isSuccess)
        // 不正な行はスキップして、有効なレコードのみ返す
        val records = result.getOrNull()!!
        assertEquals(2, records.size)
    }
}

/**
 * Fake implementation of ApiClient for testing
 */
class FakeApiClient : ApiClient {
    private var postRecordSuccess = true
    private var postRecordError: String? = null
    private var fetchRecordsData: String? = null
    private var fetchRecordsError: String? = null

    var lastPostRequest: BloodPressureSubmitRequest? = null
        private set
    var lastAuthToken: String? = null
        private set

    fun setPostRecordSuccess(success: Boolean) {
        postRecordSuccess = success
        if (success) postRecordError = null
    }

    fun setPostRecordError(error: String) {
        postRecordError = error
        postRecordSuccess = false
    }

    fun setFetchRecordsSuccess(data: String) {
        fetchRecordsData = data
        fetchRecordsError = null
    }

    fun setFetchRecordsError(error: String) {
        fetchRecordsError = error
        fetchRecordsData = null
    }

    override suspend fun postRecord(data: BloodPressureSubmitRequest, authToken: String): Result<Unit> {
        lastPostRequest = data
        lastAuthToken = authToken
        return if (postRecordSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(postRecordError ?: "Unknown error"))
        }
    }

    override suspend fun fetchRecordsFromS3(): Result<String> {
        return if (fetchRecordsError != null) {
            Result.failure(Exception(fetchRecordsError))
        } else {
            Result.success(fetchRecordsData ?: "")
        }
    }
}

/**
 * Fake implementation of AuthRepository for testing
 */
class FakeAuthRepository : AuthRepository {
    private var idToken: String? = null
    private var sessionValid = false
    private var signInSuccess = true
    private var signOutSuccess = true

    fun setIdToken(token: String?) {
        idToken = token
    }

    fun setSessionValid(valid: Boolean) {
        sessionValid = valid
    }

    override suspend fun checkSession(): AuthResult<Boolean> {
        return AuthResult.Success(sessionValid)
    }

    override suspend fun signIn(username: String, password: String): AuthResult<Unit> {
        return if (signInSuccess) AuthResult.Success(Unit) else AuthResult.Error("Sign in failed")
    }

    override suspend fun getIdToken(): String? {
        return idToken
    }

    override suspend fun signOut(): AuthResult<Unit> {
        return if (signOutSuccess) AuthResult.Success(Unit) else AuthResult.Error("Sign out failed")
    }
}
