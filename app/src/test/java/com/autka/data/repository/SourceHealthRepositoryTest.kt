package com.autka.data.repository

import com.autka.data.remote.backend.BackendApi
import com.autka.data.remote.backend.ImportServicesResponse
import com.autka.data.remote.backend.OffersResponse
import com.autka.data.remote.backend.SourceHealthDto
import com.autka.data.remote.backend.SourcesResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceHealthRepositoryTest {

    @Test
    fun `successful source fetch becomes the in-memory cache`() = runTest {
        val repository = DefaultSourceHealthRepository(FakeApi())

        assertTrue(repository.cachedSources().isEmpty())
        val fetched = repository.getSources()

        assertEquals(fetched, repository.cachedSources())
        assertEquals(listOf("alpha"), repository.cachedSources().map { it.id })
    }

    @Test
    fun `failed source refresh keeps the last successful cache`() = runTest {
        val api = FakeApi()
        val repository = DefaultSourceHealthRepository(api)
        repository.getSources()
        api.fail = true

        val result = runCatching { repository.getSources() }

        assertTrue(result.isFailure)
        assertEquals(listOf("alpha"), repository.cachedSources().map { it.id })
    }

    private class FakeApi : BackendApi {
        var fail = false

        override suspend fun sources(): SourcesResponse {
            if (fail) error("offline")
            return SourcesResponse(
                sources = listOf(
                    SourceHealthDto(
                        id = "alpha",
                        displayName = "Alpha",
                        enabled = true,
                        offerCount = 12,
                    ),
                ),
            )
        }

        override suspend fun offers(
            query: String?,
            make: String?,
            model: String?,
            minPrice: Double?,
            maxPrice: Double?,
            minYear: Int?,
            maxYear: Int?,
            maxMileageKm: Int?,
            fuelTypes: String?,
            transmissions: String?,
            regions: String?,
            sources: String?,
            sort: String?,
            complete: Boolean?,
            limit: Int?,
            offset: Int?,
        ): OffersResponse = OffersResponse(emptyList(), 0)

        override suspend fun importServices(region: String?): ImportServicesResponse =
            ImportServicesResponse()
    }
}
