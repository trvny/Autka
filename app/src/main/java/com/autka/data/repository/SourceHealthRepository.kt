package com.autka.data.repository

import com.autka.core.model.SourceHealth
import com.autka.data.remote.backend.BackendApi
import com.autka.data.remote.backend.SourceHealthDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSourceHealthRepository @Inject constructor(
    private val api: BackendApi,
) : SourceHealthRepository {
    @Volatile
    private var cache: CachedSourceHealth? = null

    override suspend fun getSources(): List<SourceHealth> =
        api.sources().sources.map(SourceHealthDto::toModel).also { sources ->
            cache = CachedSourceHealth(sources, System.currentTimeMillis())
        }

    override fun cachedSources(): List<SourceHealth> {
        val snapshot = cache ?: return emptyList()
        return snapshot.sources.takeIf {
            isSourceHealthCacheFresh(snapshot.storedAtEpochMs, System.currentTimeMillis())
        }.orEmpty()
    }
}

private data class CachedSourceHealth(
    val sources: List<SourceHealth>,
    val storedAtEpochMs: Long,
)

private fun SourceHealthDto.toModel() = SourceHealth(
    id = id,
    displayName = displayName,
    enabled = enabled,
    offerCount = offerCount,
    lastCompletedAtEpochMs = lastCompletedAtEpochMs,
    lastCompletedOk = lastCompletedOk,
    lastOffersUpserted = lastOffersUpserted,
)
