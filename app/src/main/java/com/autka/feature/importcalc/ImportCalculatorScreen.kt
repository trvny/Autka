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
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.autka.ui.components.isIncompleteLocalizedPercentage
import com.autka.ui.components.parseLocalizedNonNegativeAmount
import com.autka.ui.components.parseLocalizedPercentage
import com.autka.ui.components.parsePositiveInteger
import java.math.BigDecimal
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
    val locale = LocalConfiguration.current.locales[0]
    val numberSymbols = remember(locale) { DecimalFormatSymbols.getInstance(locale) }
    val defaultShippingUsd = ImportCostCalculator.DEFAULT_US_SHIPPING_USD.toLong()
    val defaultCustomsPercent = ImportCostCalculator.DEFAULT_EU_CUSTOMS_DUTY_RATE * 100.0
    val defaultVatPercent = ImportCostCalculator.DEFAULT_PL_VAT_RATE * 100.0
    val defaultCustomsPercentText = ratePercentText(
        ImportCostCalculator.DEFAULT_EU_CUSTOMS_DUTY_RATE,
        numberSymbols,
    )
    val defaultVatPercentText = ratePercentText(
        ImportCostCalculator.DEFAULT_PL_VAT_RATE,
        numberSymbols,
    )

    var vehiclePriceText by rememberSaveable {
        mutableStateOf(ImportCostCalculator.DEFAULT_VEHICLE_PRICE_USD.toLong().toString())
    }
    var shippingText by rememberSaveable { mutableStateOf(defaultShippingUsd.toString()) }
    var customsRateText by rememberSaveable { mutableStateOf(defaultCustomsPercentText) }
    var vatRateText by rememberSaveable { mutableStateOf(defaultVatPercentText) }
    var engineText by rememberSaveable { mutableStateOf("") }
    var fuelName by rememberSaveable { mutableStateOf(FuelType.PETROL.name) }
    var showAssumptions by rememberSaveable { mutableStateOf(false) }

    val vehiclePrice = parseLocalizedNonNegativeAmount(vehiclePriceText, numberSymbols)
    val shipping = parseLocalizedNonNegativeAmount(shippingText, numberSymbols)
    val customsRatePercent = parseLocalizedPercentage(customsRateText, numberSymbols)
    val vatRatePercent = parseLocalizedPercentage(vatRateText, numberSymbols)
    val vehiclePriceInvalid = vehiclePriceText.isBlank() || vehiclePrice == null &&
        !isIncompleteLocalizedAmount(vehiclePriceText, numberSymbols)
    val shippingInvalid = shippingText.isBlank() || shipping == null &&
        !isIncompleteLocalizedAmount(shippingText, numberSymbols)
    val customsRateInvalid = customsRateText.isBlank() || customsRatePercent == null &&
        !isIncompleteLocalizedPercentage(customsRateText, numberSymbols)
    val vatRateInvalid = vatRateText.isBlank() || vatRatePercent == null &&
        !isIncompleteLocalizedPercentage(vatRateText, numberSymbols)
    val assumptionsReady = shipping != null && customsRatePercent != null && vatRatePercent != null
    val assumptionsAreDefault = shipping == defaultShippingUsd.toDouble() &&
        customsRatePercent == defaultCustomsPercent &&
        vatRatePercent == defaultVatPercent
    val fuel = FuelType.entries.firstOrNull { it.name == fuelName } ?: FuelType.PETROL
    val engineRequired = fuel != FuelType.ELECTRIC && fuel != FuelType.HYDROGEN
    val engineCc = if (engineRequired) parsePositiveInteger(engineText, numberSymbols) else null
    val engineInvalid = engineRequired && engineText.isNotBlank() && engineCc == null
    val estimate = if (
        vehiclePrice != null && shipping != null && customsRatePercent != null &&
        vatRatePercent != null && !engineInvalid
    ) {
        ImportCostCalculator.estimate(
            vehiclePriceUsd = vehiclePrice,
            shippingUsd = shipping,
            engineCapacityCc = engineCc,
            fuelType = fuel,
            customsDutyRate = customsRatePercent / 100.0,
            vatRate = vatRatePercent / 100.0,
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
                modifier = Modifier.fillMaxWidth(),
            )

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

            AssumptionsCard(
                expanded = showAssumptions,
                assumptionsAreDefault = assumptionsAreDefault,
                canCollapse = assumptionsReady,
                shippingText = shippingText,
                customsRateText = customsRateText,
                vatRateText = vatRateText,
                shippingInvalid = shippingInvalid,
                customsRateInvalid = customsRateInvalid,
                vatRateInvalid = vatRateInvalid,
                defaultShippingUsd = defaultShippingUsd,
                defaultCustomsPercentText = defaultCustomsPercentText,
                defaultVatPercentText = defaultVatPercentText,
                onExpandedChange = { showAssumptions = it },
                onShippingChange = { shippingText = it },
                onCustomsRateChange = { customsRateText = it },
                onVatRateChange = { vatRateText = it },
                onReset = {
                    shippingText = defaultShippingUsd.toString()
                    customsRateText = defaultCustomsPercentText
                    vatRateText = defaultVatPercentText
                },
            )

            Text(
                stringResource(R.string.import_calculator_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AssumptionsCard(
    expanded: Boolean,
    assumptionsAreDefault: Boolean,
    canCollapse: Boolean,
    shippingText: String,
    customsRateText: String,
    vatRateText: String,
    shippingInvalid: Boolean,
    customsRateInvalid: Boolean,
    vatRateInvalid: Boolean,
    defaultShippingUsd: Long,
    defaultCustomsPercentText: String,
    defaultVatPercentText: String,
    onExpandedChange: (Boolean) -> Unit,
    onShippingChange: (String) -> Unit,
    onCustomsRateChange: (String) -> Unit,
    onVatRateChange: (String) -> Unit,
    onReset: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.import_assumptions),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(
                            if (assumptionsAreDefault) {
                                R.string.import_assumptions_default
                            } else {
                                R.string.import_assumptions_custom
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    enabled = !expanded || canCollapse,
                    onClick = { onExpandedChange(!expanded) },
                ) {
                    Text(
                        stringResource(
                            if (expanded) R.string.import_assumptions_done
                            else R.string.import_assumptions_adjust,
                        ),
                    )
                }
            }

            if (expanded) {
                Text(
                    stringResource(R.string.import_assumptions_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = shippingText,
                    onValueChange = onShippingChange,
                    label = { Text(stringResource(R.string.import_shipping_usd)) },
                    singleLine = true,
                    isError = shippingInvalid,
                    supportingText = {
                        Text(
                            if (shippingInvalid) {
                                stringResource(R.string.import_invalid_amount)
                            } else {
                                stringResource(R.string.import_default_shipping_usd, defaultShippingUsd)
                            },
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customsRateText,
                        onValueChange = onCustomsRateChange,
                        label = { Text(stringResource(R.string.import_customs_rate_percent)) },
                        singleLine = true,
                        isError = customsRateInvalid,
                        supportingText = {
                            Text(
                                if (customsRateInvalid) {
                                    stringResource(R.string.import_invalid_rate)
                                } else {
                                    stringResource(
                                        R.string.import_default_percent,
                                        defaultCustomsPercentText,
                                    )
                                },
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = vatRateText,
                        onValueChange = onVatRateChange,
                        label = { Text(stringResource(R.string.import_vat_rate_percent)) },
                        singleLine = true,
                        isError = vatRateInvalid,
                        supportingText = {
                            Text(
                                if (vatRateInvalid) {
                                    stringResource(R.string.import_invalid_rate)
                                } else {
                                    stringResource(
                                        R.string.import_default_percent,
                                        defaultVatPercentText,
                                    )
                                },
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.import_reset_assumptions))
                }
            }
        }
    }
}

private fun ratePercentText(rate: Double, symbols: DecimalFormatSymbols): String =
    BigDecimal.valueOf(rate)
        .movePointRight(2)
        .stripTrailingZeros()
        .toPlainString()
        .replace('.', symbols.decimalSeparator)

@Composable
private fun ImportEstimateBreakdown(
    estimate: ImportCostEstimate,
    displayCurrency: Currency,
    exchangeRates: ExchangeRates?,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.import_estimated_total),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                estimate.total.formatted(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (exchangeRates != null && estimate.total.currency != displayCurrency) {
                CostRow(
                    stringResource(R.string.import_total_in, displayCurrency.name),
                    exchangeRates.convert(estimate.total, displayCurrency).formatted(),
                )
                if (exchangeRates.isStale) {
                    Text(
                        stringResource(R.string.listing_rates_stale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (estimate.usesConservativeExcise) {
                Text(
                    stringResource(R.string.import_unknown_engine_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Divider(Modifier.padding(vertical = 4.dp))
            CostRow(stringResource(R.string.import_vehicle), estimate.vehiclePrice.formatted())
            CostRow(stringResource(R.string.import_shipping), estimate.shipping.formatted())
            CostRow(stringResource(R.string.import_customs), estimate.customsDuty.formatted())
            CostRow(stringResource(R.string.import_excise), estimate.exciseDuty.formatted())
            CostRow(stringResource(R.string.import_vat_result), estimate.vat.formatted())
        }
    }
}

@Composable
private fun CostRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
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
