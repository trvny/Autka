package com.autka.data.remote.vin

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VpicApi {
    @GET("vehicles/DecodeVinValues/{vin}")
    suspend fun decodeVin(
        @Path("vin") vin: String,
        @Query("format") format: String = "json",
    ): VpicResponse
}
