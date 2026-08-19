package com.autka.data.remote.backend

import com.autka.core.model.SourceHealth
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SourcesResponse(
    val sources: List<SourceHealthDto> = emptyList(),
)

@Serializable
data class SourceHealthDto(
    val id: String,
    val displayName: String,
    val enabled: Boolean,
    val offerCount: Int? = null,
    val lastCompletedAtEpochMs: Long? = null,
    val lastCompletedOk: Boolean? = null,
    val lastOffersUpserted: Int? = null,
)

object SourceHealthDecoder {
    private val json = Json { ignoreUnknownKeys = true }

    fun decodeJson(payload: String): List<SourceHealth> =
        decode(json.decodeFromString<SourcesResponse>(payload))

    fun decodeJsonOrNull(payload: String): List<SourceHealth>? =
        runCatching { decodeJson(payload) }.getOrNull()

    fun decode(response: SourcesResponse): List<SourceHealth> =
        response.sources.map(SourceHealthDto::toModel)
}

private fun SourceHealthDto.toModel(): SourceHealth = SourceHealth(
    id = id,
    displayName = displayName,
    enabled = enabled,
    offerCount = offerCount,
    lastCompletedAtEpochMs = lastCompletedAtEpochMs,
    lastCompletedOk = lastCompletedOk,
    lastOffersUpserted = lastOffersUpserted,
)
