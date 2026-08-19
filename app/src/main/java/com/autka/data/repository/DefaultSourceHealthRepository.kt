package com.autka.data.repository

import com.autka.core.model.SourceHealth
import com.autka.data.remote.backend.BackendApi
import com.autka.data.remote.backend.SourceHealthDecoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSourceHealthRepository @Inject constructor(
    private val api: BackendApi,
) : SourceHealthRepository {
    @Volatile
    private var cache: CachedSourceHealth? = null

    override suspend fun getSources(): List<SourceHealth> =
        SourceHealthDecoder.decode(api.sources()).also { sources ->
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
