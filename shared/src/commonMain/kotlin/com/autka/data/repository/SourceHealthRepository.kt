package com.autka.data.repository

import com.autka.core.model.SourceHealth

interface SourceHealthRepository {
    suspend fun getSources(): List<SourceHealth>
    fun cachedSources(): List<SourceHealth>
}
