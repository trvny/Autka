package com.autka.core.model

const val MAX_IMPORT_PRESET_NAME_LENGTH = 40

data class ImportAssumptionPreset(
    val id: String,
    val name: String,
    val shippingUsd: Double,
    val customsDutyRate: Double,
    val vatRate: Double,
)
