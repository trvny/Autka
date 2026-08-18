package com.autka.data.repository

import com.autka.core.model.VinDecodeResult
import com.autka.data.remote.vin.VpicApi
import com.autka.data.remote.vin.VpicResult
import javax.inject.Inject
import javax.inject.Singleton

interface VinDecoderRepository {
    suspend fun decode(vin: String): VinDecodeResult
}

@Singleton
class DefaultVinDecoderRepository @Inject constructor(
    private val api: VpicApi,
) : VinDecoderRepository {
    override suspend fun decode(vin: String): VinDecodeResult {
        val decoded = api.decodeVin(vin).results.firstOrNull()
            ?: error("NHTSA vPIC returned no VIN result")
        return decoded.toModel(fallbackVin = vin)
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
