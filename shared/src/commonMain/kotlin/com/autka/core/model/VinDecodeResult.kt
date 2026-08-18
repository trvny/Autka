package com.autka.core.model

data class VinDecodeResult(
    val vin: String,
    val make: String? = null,
    val model: String? = null,
    val modelYear: String? = null,
    val trim: String? = null,
    val series: String? = null,
    val vehicleType: String? = null,
    val bodyClass: String? = null,
    val fuelType: String? = null,
    val electrificationLevel: String? = null,
    val displacementLiters: String? = null,
    val engineCylinders: String? = null,
    val engineHp: String? = null,
    val driveType: String? = null,
    val transmissionStyle: String? = null,
    val plantCountry: String? = null,
    val plantCity: String? = null,
    val decoderWarning: String? = null,
)
