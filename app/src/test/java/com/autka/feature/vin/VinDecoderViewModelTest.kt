package com.autka.feature.vin

import com.autka.core.model.VinDecodeResult
import com.autka.data.repository.VinDecoderRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VinDecoderViewModelTest {

    @Test
    fun `VIN input is normalized and capped at 17 characters`() {
        assertEquals("1M8GDM9AXKP042788", normalizeVinInput("1m8g-dm9a xkp042788-extra"))
        assertEquals("1M8GDM9AXKP042788", normalizeVinInput("Ł1m8g-dm9a xkp042788"))
    }

    @Test
    fun `VIN validation rejects wrong length and forbidden letters`() {
        assertFalse(isValidVin("1M8GDM9AXKP04278"))
        assertFalse(isValidVin("1M8GDM9AXIP042788"))
        assertTrue(isValidVin("1M8GDM9AXKP042788"))
    }

    @Test
    fun `invalid VIN blocks network decode`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeRepository()
            val viewModel = VinDecoderViewModel(repository)
            viewModel.onVinChange("ABC")

            viewModel.decode()
            advanceUntilIdle()

            assertEquals(0, repository.callCount)
            assertTrue(viewModel.uiState.value.validationError)
            assertNull(viewModel.uiState.value.result)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `valid VIN decodes and exposes result`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val expected = VinDecodeResult(vin = "1M8GDM9AXKP042788", make = "MCI")
            val repository = FakeRepository(result = Result.success(expected))
            val viewModel = VinDecoderViewModel(repository)
            viewModel.onVinChange("1M8GDM9AXKP042788")

            viewModel.decode()
            advanceUntilIdle()

            assertEquals(1, repository.callCount)
            assertEquals(expected, viewModel.uiState.value.result)
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.loadFailed)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `editing VIN cancels stale decode result`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val gate = CompletableDeferred<Unit>()
            val repository = FakeRepository(
                result = Result.success(VinDecodeResult(vin = "1M8GDM9AXKP042788", make = "OLD")),
                gate = gate,
            )
            val viewModel = VinDecoderViewModel(repository)
            viewModel.onVinChange("1M8GDM9AXKP042788")

            viewModel.decode()
            runCurrent()
            viewModel.onVinChange("1HGCM82633A004352")
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, repository.callCount)
            assertEquals("1HGCM82633A004352", viewModel.uiState.value.vin)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.result)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `decode failure becomes retryable error`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeRepository(result = Result.failure(IllegalStateException("offline")))
            val viewModel = VinDecoderViewModel(repository)
            viewModel.onVinChange("1M8GDM9AXKP042788")

            viewModel.decode()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.loadFailed)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.result)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `concurrent decode requests are coalesced`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val gate = CompletableDeferred<Unit>()
            val repository = FakeRepository(gate = gate)
            val viewModel = VinDecoderViewModel(repository)
            viewModel.onVinChange("1M8GDM9AXKP042788")

            viewModel.decode()
            runCurrent()
            viewModel.decode()
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
        private val result: Result<VinDecodeResult> = Result.success(
            VinDecodeResult(vin = "1M8GDM9AXKP042788"),
        ),
        private val gate: CompletableDeferred<Unit>? = null,
    ) : VinDecoderRepository {
        var callCount = 0

        override suspend fun decode(vin: String): VinDecodeResult {
            callCount += 1
            gate?.await()
            return result.getOrThrow()
        }
    }
}
