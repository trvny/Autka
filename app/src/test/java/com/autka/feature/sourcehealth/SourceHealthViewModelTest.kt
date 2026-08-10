package com.autka.feature.sourcehealth

import com.autka.core.model.SourceHealth
import com.autka.data.repository.SourceHealthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourceHealthViewModelTest {

    @Test
    fun `initial refresh exposes sorted sources`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeRepository(
                responses = ArrayDeque(
                    listOf(
                        Result.success(
                            listOf(
                                source(id = "disabled", enabled = false),
                                source(id = "beta"),
                                source(id = "alpha"),
                            ),
                        ),
                    ),
                ),
            )

            val viewModel = SourceHealthViewModel(repository)
            advanceUntilIdle()

            assertEquals(listOf("alpha", "beta", "disabled"), viewModel.uiState.value.sources.map { it.id })
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.loadFailed)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed refresh preserves the previous snapshot`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeRepository(
                responses = ArrayDeque(
                    listOf(
                        Result.success(listOf(source(id = "alpha"))),
                        Result.failure(IllegalStateException("offline")),
                    ),
                ),
            )
            val viewModel = SourceHealthViewModel(repository)
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(listOf("alpha"), viewModel.uiState.value.sources.map { it.id })
            assertTrue(viewModel.uiState.value.loadFailed)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `concurrent refresh is coalesced`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val gate = CompletableDeferred<Unit>()
            val repository = FakeRepository(
                responses = ArrayDeque(listOf(Result.success(listOf(source(id = "alpha"))))),
                gate = gate,
            )
            val viewModel = SourceHealthViewModel(repository)
            runCurrent()

            viewModel.refresh()
            runCurrent()

            assertEquals(1, repository.callCount)
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, repository.callCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeRepository(
        private val responses: ArrayDeque<Result<List<SourceHealth>>>,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : SourceHealthRepository {
        var callCount = 0

        override suspend fun getSources(): List<SourceHealth> {
            callCount += 1
            gate?.await()
            return responses.removeFirst().getOrThrow()
        }
    }

    private companion object {
        fun source(id: String, enabled: Boolean = true) = SourceHealth(
            id = id,
            displayName = id,
            enabled = enabled,
            offerCount = 0,
            lastCompletedAtEpochMs = null,
            lastCompletedOk = null,
            lastOffersUpserted = null,
        )
    }
}
