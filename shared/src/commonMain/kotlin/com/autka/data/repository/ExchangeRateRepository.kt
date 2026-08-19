package com.autka.data.repository

import com.autka.core.model.ExchangeRates
import kotlinx.coroutines.flow.StateFlow

interface ExchangeRateRepository {
    /** Latest rates; always has a value (seeded with offline fallback). */
    fun rates(): StateFlow<ExchangeRates>

    /** Refresh from the live source; concurrent callers share the same in-flight attempt. */
    suspend fun refresh()
}
