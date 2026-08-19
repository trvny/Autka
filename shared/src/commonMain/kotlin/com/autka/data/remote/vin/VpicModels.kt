package com.autka.data.remote.vin

import com.autka.core.model.VinDecodeResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

object VpicDecoder {
    private val json = Json { ignoreUnknownKeys = true }

    fun decodeJson(payload: String, fallbackVin: String): VinDecodeResult =
        decode(json.decodeFromString<VpicResponse>(payload), fallbackVin)

    fun decode(response: VpicResponse, fallbackVin: String): VinDecodeResult {
        val decoded = response.results.firstOrNull()
            ?: error("NHTSA vPIC returned no VIN result")
        return decoded.toModel(fallbackVin)
    }
}

private fun VpicResult.toModel(fallbackVin: String): VinDecodeResult = VinDecodeResult(
    vin = vin.clean() ?: fallbackVin,
    make = make.clean(),
    model = model.clean(),
    modelYear = modelYear.clean(),
    trim = trim.clean(),
    series = series.clean(),
    vehicleType = vehicleType.clean(),
    bodyClass = bodyClass.clean(),
    fuelType = fuelTypePrimary.clean(),
    electrificationLevel = electrificationLevel.clean(),
    displacementLiters = displacementLiters.clean(),
    engineCylinders = engineCylinders.clean(),
    engineHp = engineHp.clean(),
    driveType = driveType.clean(),
    transmissionStyle = transmissionStyle.clean(),
    plantCountry = plantCountry.clean(),
    plantCity = plantCity.clean(),
    decoderWarning = if (errorCode.isCleanVpicCode()) null else errorText.clean(),
)

private fun String?.clean(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.isCleanVpicCode(): Boolean =
    clean()
        ?.split(',')
        ?.map(String::trim)
        ?.takeIf { it.isNotEmpty() }
        ?.all { it == "0" } == true
