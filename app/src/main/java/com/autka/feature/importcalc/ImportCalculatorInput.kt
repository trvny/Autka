package com.autka.feature.importcalc

import com.autka.core.model.FuelType
import com.autka.core.model.ImportCostCalculator
import com.autka.core.model.ImportCostEstimate
import com.autka.ui.components.isIncompleteLocalizedAmount
import com.autka.ui.components.isIncompleteLocalizedPercentage
import com.autka.ui.components.parseLocalizedNonNegativeAmount
import com.autka.ui.components.parseLocalizedPercentage
import com.autka.ui.components.parsePositiveInteger
import java.text.DecimalFormatSymbols
import kotlin.math.abs

private const val VALUE_COMPARISON_EPSILON = 0.0001

internal data class ImportCalculatorFields(
    val vehiclePriceText: String,
    val shippingText: String,
    val customsRateText: String,
    val vatRateText: String,
    val engineText: String,
    val fuel: FuelType,
)

internal data class ImportCalculatorInputState(
    val vehiclePriceInvalid: Boolean,
    val shippingInvalid: Boolean,
    val customsRateInvalid: Boolean,
    val vatRateInvalid: Boolean,
    val assumptionsReady: Boolean,
    val assumptionsAreDefault: Boolean,
    val engineRequired: Boolean,
    val engineInvalid: Boolean,
    val engineCapacityCc: Int?,
    val estimate: ImportCostEstimate?,
)

internal fun evaluateImportCalculatorInput(
    fields: ImportCalculatorFields,
    numberSymbols: DecimalFormatSymbols,
): ImportCalculatorInputState {
    val vehiclePrice = parseLocalizedNonNegativeAmount(fields.vehiclePriceText, numberSymbols)
    val shipping = parseLocalizedNonNegativeAmount(fields.shippingText, numberSymbols)
    val customsRatePercent = parseLocalizedPercentage(fields.customsRateText, numberSymbols)
    val vatRatePercent = parseLocalizedPercentage(fields.vatRateText, numberSymbols)

    val vehiclePriceInvalid = fields.vehiclePriceText.isBlank() ||
        (vehiclePrice == null && !isIncompleteLocalizedAmount(fields.vehiclePriceText, numberSymbols))
    val shippingInvalid = fields.shippingText.isBlank() ||
        (shipping == null && !isIncompleteLocalizedAmount(fields.shippingText, numberSymbols))
    val customsRateInvalid = fields.customsRateText.isBlank() ||
        (customsRatePercent == null && !isIncompleteLocalizedPercentage(fields.customsRateText, numberSymbols))
    val vatRateInvalid = fields.vatRateText.isBlank() ||
        (vatRatePercent == null && !isIncompleteLocalizedPercentage(fields.vatRateText, numberSymbols))

    val assumptionsReady = shipping != null && customsRatePercent != null && vatRatePercent != null
    val assumptionsAreDefault = shipping.isNear(ImportCostCalculator.DEFAULT_US_SHIPPING_USD) &&
        customsRatePercent.isNear(ImportCostCalculator.DEFAULT_EU_CUSTOMS_DUTY_RATE * 100.0) &&
        vatRatePercent.isNear(ImportCostCalculator.DEFAULT_PL_VAT_RATE * 100.0)

    val engineRequired = fields.fuel != FuelType.ELECTRIC && fields.fuel != FuelType.HYDROGEN
    val engineCc = if (engineRequired) parsePositiveInteger(fields.engineText, numberSymbols) else null
    val engineInvalid = engineRequired && fields.engineText.isNotBlank() && engineCc == null

    val estimate = if (
        vehiclePrice != null && shipping != null && customsRatePercent != null &&
        vatRatePercent != null && !engineInvalid
    ) {
        ImportCostCalculator.estimate(
            vehiclePriceUsd = vehiclePrice,
            shippingUsd = shipping,
            engineCapacityCc = engineCc,
            fuelType = fields.fuel,
            customsDutyRate = customsRatePercent / 100.0,
            vatRate = vatRatePercent / 100.0,
        )
    } else {
        null
    }

    return ImportCalculatorInputState(
        vehiclePriceInvalid = vehiclePriceInvalid,
        shippingInvalid = shippingInvalid,
        customsRateInvalid = customsRateInvalid,
        vatRateInvalid = vatRateInvalid,
        assumptionsReady = assumptionsReady,
        assumptionsAreDefault = assumptionsAreDefault,
        engineRequired = engineRequired,
        engineInvalid = engineInvalid,
        engineCapacityCc = engineCc,
        estimate = estimate,
    )
}

private fun Double?.isNear(expected: Double): Boolean =
    this != null && abs(this - expected) < VALUE_COMPARISON_EPSILON
