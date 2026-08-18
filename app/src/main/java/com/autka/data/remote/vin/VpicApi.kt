package com.autka.data.remote.vin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

@Serializable
data class VpicResponse(
    @SerialName("Results") val results: List<VpicResult> = emptyList(),
)

@Serializable
data class VpicResult(
    @SerialName("VIN") val vin: String? = null,
    @SerialName("Make") val make: String? = null,
    @SerialName("Model") val model: String? = null,
    @SerialName("ModelYear") val modelYear: String? = null,
    @SerialName("Trim") val trim: String? = null,
    @SerialName("Series") val series: String? = null,
    @SerialName("VehicleType") val vehicleType: String? = null,
    @SerialName("BodyClass") val bodyClass: String? = null,
    @SerialName("FuelTypePrimary") val fuelTypePrimary: String? = null,
    @SerialName("ElectrificationLevel") val electrificationLevel: String? = null,
    @SerialName("DisplacementL") val displacementLiters: String? = null,
    @SerialName("EngineCylinders") val engineCylinders: String? = null,
    @SerialName("EngineHP") val engineHp: String? = null,
    @SerialName("DriveType") val driveType: String? = null,
    @SerialName("TransmissionStyle") val transmissionStyle: String? = null,
    @SerialName("PlantCountry") val plantCountry: String? = null,
    @SerialName("PlantCity") val plantCity: String? = null,
    @SerialName("ErrorCode") val errorCode: String? = null,
    @SerialName("ErrorText") val errorText: String? = null,
)
