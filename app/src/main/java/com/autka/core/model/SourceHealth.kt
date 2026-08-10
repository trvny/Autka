package com.autka.core.model

data class SourceHealth(
    val id: String,
    val displayName: String,
    val enabled: Boolean,
    val offerCount: Int?,
    val lastCompletedAtEpochMs: Long?,
    val lastCompletedOk: Boolean?,
    val lastOffersUpserted: Int?,
)
