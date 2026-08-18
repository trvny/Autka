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
    @SerialName("VIN") val vin: String = "",
    @SerialName("Make") val make: String = "",
    @SerialName("Model") val model: String = "",
    @SerialName("ModelYear") val modelYear: String = "",
    @SerialName("Trim") val trim: String = "",
    @SerialName("Series") val series: String = "",
    @SerialName("VehicleType") val vehicleType: String = "",
    @SerialName("BodyClass") val bodyClass: String = "",
    @SerialName("FuelTypePrimary") val fuelTypePrimary: String = "",
    @SerialName("ElectrificationLevel") val electrificationLevel: String = "",
    @SerialName("DisplacementL") val displacementLiters: String = "",
    @SerialName("EngineCylinders") val engineCylinders: String = "",
    @SerialName("EngineHP") val engineHp: String = "",
    @SerialName("DriveType") val driveType: String = "",
    @SerialName("TransmissionStyle") val transmissionStyle: String = "",
    @SerialName("PlantCountry") val plantCountry: String = "",
    @SerialName("PlantCity") val plantCity: String = "",
    @SerialName("ErrorCode") val errorCode: String = "",
    @SerialName("ErrorText") val errorText: String = "",
)
