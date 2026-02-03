package com.example.bloodpressuretracking.ui.recordlist

import com.example.bloodpressuretracking.data.repository.BloodPressureRecord
import com.example.bloodpressuretracking.data.repository.BloodPressureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordListViewModelTest {

    private lateinit var viewModel: RecordListViewModel
    private lateinit var fakeBloodPressureRepository: FakeBloodPressureRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeBloodPressureRepository = FakeBloodPressureRepository()
        viewModel = RecordListViewModel(fakeBloodPressureRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 初期状態テスト

    @Test
    fun `initial state has empty records and not loading`() = runTest {
        val state = viewModel.uiState.first()

        assertTrue(state.records.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    // データ取得テスト

    @Test
    fun `fetchRecords sets isLoading to true during fetch`() = runTest {
        val records = listOf(
            BloodPressureRecord(120, 80, 70, "2024-01-15 08:30")
        )
        fakeBloodPressureRepository.setRecords(records)

        viewModel.fetchRecords()

        val state = viewModel.uiState.first()
        assertTrue(state.isLoading)
    }

    @Test
    fun `fetchRecords updates records on success`() = runTest {
        val records = listOf(
            BloodPressureRecord(120, 80, 70, "2024-01-15 08:30"),
            BloodPressureRecord(125, 82, 72, "2024-01-16 09:00")
        )
        fakeBloodPressureRepository.setRecords(records)

        viewModel.fetchRecords()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertEquals(2, state.records.size)
        assertEquals(120, state.records[0].systolic)
        assertEquals(80, state.records[0].diastolic)
        assertEquals(70, state.records[0].pulse)
        assertNull(state.errorMessage)
    }

    @Test
    fun `fetchRecords sets empty list when no records`() = runTest {
        fakeBloodPressureRepository.setRecords(emptyList())

        viewModel.fetchRecords()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertTrue(state.records.isEmpty())
        assertNull(state.errorMessage)
    }

    // エラーハンドリングテスト

    @Test
    fun `fetchRecords sets errorMessage on failure`() = runTest {
        fakeBloodPressureRepository.setFetchError("ネットワークに接続できません")

        viewModel.fetchRecords()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertTrue(state.records.isEmpty())
        assertEquals("ネットワークに接続できません", state.errorMessage)
    }

    // エラークリアテスト

    @Test
    fun `clearError clears error message`() = runTest {
        fakeBloodPressureRepository.setFetchError("エラー")
        viewModel.fetchRecords()
        advanceUntilIdle()

        viewModel.clearError()

        val state = viewModel.uiState.first()
        assertNull(state.errorMessage)
    }

    // リフレッシュテスト

    @Test
    fun `fetchRecords clears previous error on retry`() = runTest {
        // First fetch fails
        fakeBloodPressureRepository.setFetchError("エラー")
        viewModel.fetchRecords()
        advanceUntilIdle()

        // Second fetch succeeds
        fakeBloodPressureRepository.setRecords(
            listOf(BloodPressureRecord(120, 80, 70, "2024-01-15"))
        )
        viewModel.fetchRecords()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertNull(state.errorMessage)
        assertEquals(1, state.records.size)
    }
}

/**
 * Fake implementation of BloodPressureRepository for testing
 */
class FakeBloodPressureRepository : BloodPressureRepository {
    private var records: List<BloodPressureRecord> = emptyList()
    private var fetchError: String? = null

    fun setRecords(list: List<BloodPressureRecord>) {
        records = list
        fetchError = null
    }

    fun setFetchError(error: String) {
        fetchError = error
    }

    override suspend fun submitRecord(systolic: Int, diastolic: Int, pulse: Int): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun fetchRecords(): Result<List<BloodPressureRecord>> {
        return if (fetchError != null) {
            Result.failure(Exception(fetchError))
        } else {
            Result.success(records)
        }
    }
}
