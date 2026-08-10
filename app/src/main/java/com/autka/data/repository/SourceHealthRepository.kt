package com.autka.data.repository

import com.autka.core.model.SourceHealth
import com.autka.data.remote.backend.BackendApi
import com.autka.data.remote.backend.SourceHealthDto
import javax.inject.Inject
import javax.inject.Singleton

interface SourceHealthRepository {
    suspend fun getSources(): List<SourceHealth>
}

@Singleton
class DefaultSourceHealthRepository @Inject constructor(
    private val api: BackendApi,
) : SourceHealthRepository {
    override suspend fun getSources(): List<SourceHealth> =
        api.sources().sources.map(SourceHealthDto::toModel)
}

private fun SourceHealthDto.toModel() = SourceHealth(
    id = id,
    displayName = displayName,
    enabled = enabled,
    offerCount = offerCount,
    lastCompletedAtEpochMs = lastCompletedAtEpochMs,
    lastCompletedOk = lastCompletedOk,
    lastOffersUpserted = lastOffersUpserted,
)
