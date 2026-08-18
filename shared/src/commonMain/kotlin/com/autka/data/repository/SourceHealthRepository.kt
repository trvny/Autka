package com.autka.data.repository

import com.autka.core.model.SourceHealth

const val SOURCE_HEALTH_CACHE_TTL_MS: Long = 5 * 60 * 1000L

/** Platform-neutral source-health access and cache semantics. */
interface SourceHealthRepository {
    suspend fun getSources(): List<SourceHealth>

    /** Returns the last successful snapshot while it is fresh, otherwise an empty list. */
    fun cachedSources(): List<SourceHealth>
}

/** Shared freshness rule so platform implementations do not diverge. */
fun isSourceHealthCacheFresh(storedAtEpochMs: Long, nowEpochMs: Long): Boolean {
    val ageMs = nowEpochMs - storedAtEpochMs
    return ageMs in 0..SOURCE_HEALTH_CACHE_TTL_MS
}
