package com.autka.data.repository

import com.autka.data.remote.vin.VpicApi
import com.autka.data.remote.vin.VpicResponse
import com.autka.data.remote.vin.VpicResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VinDecoderRepositoryTest {

    @Test
    fun `decoder maps useful vPIC values and drops blanks`() = runTest {
        val repository = DefaultVinDecoderRepository(
            FakeApi(
                VpicResponse(
                    results = listOf(
                        VpicResult(
                            vin = "1M8GDM9AXKP042788",
                            make = "MCI",
                            model = "102EL3 Intercity",
                            modelYear = "1989",
                            trim = "   ",
                            vehicleType = "BUS",
                            fuelTypePrimary = "Diesel",
                            engineCylinders = "6",
                            plantCountry = "UNITED STATES (USA)",
                            errorCode = "0",
                            errorText = "0 - VIN decoded clean.",
                        ),
                    ),
                ),
            ),
        )

        val decoded = repository.decode("1M8GDM9AXKP042788")

        assertEquals("1M8GDM9AXKP042788", decoded.vin)
        assertEquals("MCI", decoded.make)
        assertEquals("1989", decoded.modelYear)
        assertEquals("BUS", decoded.vehicleType)
        assertEquals("Diesel", decoded.fuelType)
        assertEquals("6", decoded.engineCylinders)
        assertNull(decoded.trim)
        assertNull(decoded.decoderWarning)
    }

    @Test
    fun `composite zero decoder codes stay clean`() = runTest {
        val repository = DefaultVinDecoderRepository(
            FakeApi(
                VpicResponse(
                    results = listOf(
                        VpicResult(
                            vin = "1M8GDM9AXKP042788",
                            errorCode = "0, 0",
                            errorText = "0 - VIN decoded clean.",
                        ),
                    ),
                ),
            ),
        )

        val decoded = repository.decode("1M8GDM9AXKP042788")

        assertNull(decoded.decoderWarning)
    }

    @Test
    fun `decoder keeps NHTSA warning alongside decoded values`() = runTest {
        val repository = DefaultVinDecoderRepository(
            FakeApi(
                VpicResponse(
                    results = listOf(
                        VpicResult(
                            vin = "1M8GDM9AXKP042788",
                            make = "MCI",
                            errorCode = "8",
                            errorText = "8 - No detailed data was available.",
                        ),
                    ),
                ),
            ),
        )

        val decoded = repository.decode("1M8GDM9AXKP042788")

        assertEquals("MCI", decoded.make)
        assertEquals("8 - No detailed data was available.", decoded.decoderWarning)
    }

    @Test
    fun `empty vPIC response fails instead of inventing a result`() = runTest {
        val repository = DefaultVinDecoderRepository(FakeApi(VpicResponse()))

        val result = runCatching { repository.decode("1M8GDM9AXKP042788") }

        assertTrue(result.isFailure)
    }

    private class FakeApi(private val response: VpicResponse) : VpicApi {
        override suspend fun decodeVin(vin: String, format: String): VpicResponse = response
    }
}
