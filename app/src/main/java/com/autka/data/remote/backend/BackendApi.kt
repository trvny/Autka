package com.autka.data.remote.backend

import com.autka.core.model.ImportService
import com.autka.core.model.Region
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Client for the Autka aggregation backend. */
interface BackendApi {
    @GET("offers")
    suspend fun offers(
        @Query("query") query: String? = null,
        @Query("make") make: String? = null,
        @Query("model") model: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("minYear") minYear: Int? = null,
        @Query("maxYear") maxYear: Int? = null,
        @Query("maxMileageKm") maxMileageKm: Int? = null,
        @Query("fuelTypes") fuelTypes: String? = null,
        @Query("transmissions") transmissions: String? = null,
        @Query("regions") regions: String? = null,
        @Query("sources") sources: String? = null,
        @Query("sort") sort: String? = null,
        @Query("complete") complete: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): OffersResponse

    @GET("sources")
    suspend fun sources(): SourcesResponse

    /**
     * Authoritative import/sourcing-company directory (served at GET /import-services).
     * Optional [region] narrows to companies importing FROM that region. The app uses
     * this to override its compiled-in seed; a failed call is swallowed by the
     * repository so the offline-first seed remains.
     */
    @GET("import-services")
    suspend fun importServices(
        @Query("region") region: String? = null,
    ): ImportServicesResponse
}

@Serializable
data class ImportServicesResponse(val services: List<ImportServiceDto> = emptyList())

@Serializable
data class ImportServiceDto(
    val id: String,
    val displayName: String,
    val origin: String,
    val url: String,
    val calculatorUrl: String? = null,
    val note: String? = null,
)

/** Map a backend DTO to the domain model; an unknown [origin] string defaults to EUROPE. */
fun ImportServiceDto.toModel(): ImportService = ImportService(
    id = id,
    displayName = displayName,
    origin = runCatching { Region.valueOf(origin) }.getOrDefault(Region.EUROPE),
    url = url,
    calculatorUrl = calculatorUrl,
    note = note,
)
