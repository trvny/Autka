package com.autka.feature.importcalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autka.R
import com.autka.core.model.Currency
import com.autka.core.model.ExchangeRates
import com.autka.core.model.FuelType
import com.autka.core.model.ImportCostCalculator
import com.autka.core.model.ImportCostEstimate
import com.autka.ui.components.formatted
import com.autka.ui.components.isIncompleteLocalizedAmount
import com.autka.ui.components.parseLocalizedNonNegativeAmount
import com.autka.ui.components.parsePositiveInteger
import java.text.DecimalFormatSymbols

private val CALCULATOR_FUELS = listOf(
    FuelType.PETROL,
    FuelType.DIESEL,
    FuelType.HYBRID,
    FuelType.PLUGIN_HYBRID,
    FuelType.ELECTRIC,
    FuelType.HYDROGEN,
    FuelType.LPG,
)

@Composable
fun ImportCalculatorRoute(
    onBack: () -> Unit,
    viewModel: ImportCalculatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ImportCalculatorScreen(
        onBack = onBack,
        displayCurrency = uiState.displayCurrency,
        exchangeRates = uiState.exchangeRates,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImportCalculatorScreen(
    onBack: () -> Unit,
    displayCurrency: Currency,
    exchangeRates: ExchangeRates?,
) {
    var vehiclePriceText by rememberSaveable {
        mutableStateOf(ImportCostCalculator.DEFAULT_VEHICLE_PRICE_USD.toLong().toString())
    }
    var shippingText by rememberSaveable {
        mutableStateOf(ImportCostCalculator.DEFAULT_US_SHIPPING_USD.toLong().toString())
    }
    var engineText by rememberSaveable { mutableStateOf("") }
    var fuelName by rememberSaveable { mutableStateOf(FuelType.PETROL.name) }

    val locale = LocalConfiguration.current.locales[0]
    val numberSymbols = remember(locale) { DecimalFormatSymbols.getInstance(locale) }
    val vehiclePrice = parseLocalizedNonNegativeAmount(vehiclePriceText, numberSymbols)
    val shipping = parseLocalizedNonNegativeAmount(shippingText, numberSymbols)
    val vehiclePriceInvalid = vehiclePrice == null &&
        !isIncompleteLocalizedAmount(vehiclePriceText, numberSymbols)
    val shippingInvalid = shipping == null &&
        !isIncompleteLocalizedAmount(shippingText, numberSymbols)
    val fuel = FuelType.entries.firstOrNull { it.name == fuelName } ?: FuelType.PETROL
    val engineRequired = fuel != FuelType.ELECTRIC && fuel != FuelType.HYDROGEN
    val engineCc = if (engineRequired) parsePositiveInteger(engineText, numberSymbols) else null
    val engineInvalid = engineRequired && engineText.isNotBlank() && engineCc == null
    val estimate = if (vehiclePrice != null && shipping != null && !engineInvalid) {
        ImportCostCalculator.estimate(
            vehiclePriceUsd = vehiclePrice,
            shippingUsd = shipping,
            engineCapacityCc = engineCc,
            fuelType = fuel,
        )
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_calculator)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = vehiclePriceText,
                    onValueChange = { vehiclePriceText = it },
                    label = { Text(stringResource(R.string.import_vehicle_price_usd)) },
                    singleLine = true,
                    isError = vehiclePriceInvalid,
                    supportingText = if (vehiclePriceInvalid) {
                        { Text(stringResource(R.string.import_invalid_amount)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = shippingText,
                    onValueChange = { shippingText = it },
                    label = { Text(stringResource(R.string.import_shipping_usd)) },
                    singleLine = true,
                    isError = shippingInvalid,
                    supportingText = if (shippingInvalid) {
                        { Text(stringResource(R.string.import_invalid_amount)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.import_fuel_type),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CALCULATOR_FUELS.forEach { option ->
                        FilterChip(
                            selected = fuel == option,
                            onClick = { fuelName = option.name },
                            label = { Text(option.label()) },
                        )
                    }
                }
            }

            if (engineRequired) {
                OutlinedTextField(
                    value = engineText,
                    onValueChange = { engineText = it },
                    label = { Text(stringResource(R.string.import_engine_cc)) },
                    singleLine = true,
                    isError = engineInvalid,
                    supportingText = if (engineInvalid) {
                        { Text(stringResource(R.string.import_invalid_engine_cc)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            estimate?.let {
                ImportEstimateBreakdown(
                    estimate = it,
                    displayCurrency = displayCurrency,
                    exchangeRates = exchangeRates,
                )
            }

            Text(
                stringResource(R.string.import_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImportEstimateBreakdown(
    estimate: ImportCostEstimate,
    displayCurrency: Currency,
    exchangeRates: ExchangeRates?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (estimate.usesConservativeExcise) {
            Text(
                stringResource(R.string.import_unknown_engine_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        CostRow(stringResource(R.string.import_vehicle), estimate.vehiclePrice.formatted())
        CostRow(stringResource(R.string.import_shipping), estimate.shipping.formatted())
        CostRow(stringResource(R.string.import_customs), estimate.customsDuty.formatted())
        CostRow(stringResource(R.string.import_excise), estimate.exciseDuty.formatted())
        CostRow(stringResource(R.string.import_vat), estimate.vat.formatted())
        Divider()
        CostRow(stringResource(R.string.import_total), estimate.total.formatted(), emphasized = true)
        if (exchangeRates != null && estimate.total.currency != displayCurrency) {
            CostRow(
                stringResource(R.string.import_total_in, displayCurrency.name),
                exchangeRates.convert(estimate.total, displayCurrency).formatted(),
                emphasized = true,
            )
            if (exchangeRates.isStale) {
                Text(
                    stringResource(R.string.listing_rates_stale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CostRow(label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun FuelType.label(): String = when (this) {
    FuelType.PETROL -> stringResource(R.string.fuel_petrol)
    FuelType.DIESEL -> stringResource(R.string.fuel_diesel)
    FuelType.HYBRID -> stringResource(R.string.fuel_hybrid)
    FuelType.PLUGIN_HYBRID -> stringResource(R.string.fuel_plugin)
    FuelType.ELECTRIC -> stringResource(R.string.fuel_electric)
    FuelType.HYDROGEN -> stringResource(R.string.fuel_hydrogen)
    FuelType.LPG -> stringResource(R.string.fuel_lpg)
    FuelType.OTHER -> stringResource(R.string.fuel_other)
    FuelType.UNKNOWN -> stringResource(R.string.fuel_unknown)
}
