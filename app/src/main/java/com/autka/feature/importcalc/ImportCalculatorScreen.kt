package com.autka.feature.importcalc

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.autka.core.model.ImportAssumptionPreset
import com.autka.core.model.ImportCostCalculator
import com.autka.core.model.ImportCostEstimate
import com.autka.core.model.Money
import com.autka.ui.components.displayLabel
import com.autka.ui.components.formatted
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

private val ImportCostEstimateSaver = listSaver<ImportCostEstimate, Any>(
    save = { estimate ->
        listOf(
            estimate.vehiclePrice.amount,
            estimate.vehiclePrice.currency.name,
            estimate.shipping.amount,
            estimate.shipping.currency.name,
            estimate.customsDuty.amount,
            estimate.customsDuty.currency.name,
            estimate.exciseDuty.amount,
            estimate.exciseDuty.currency.name,
            estimate.vat.amount,
            estimate.vat.currency.name,
            estimate.total.amount,
            estimate.total.currency.name,
            estimate.usesConservativeExcise,
        )
    },
    restore = { values ->
        fun moneyAt(index: Int) = Money(
            amount = values[index] as Double,
            currency = Currency.valueOf(values[index + 1] as String),
        )
        ImportCostEstimate(
            vehiclePrice = moneyAt(0),
            shipping = moneyAt(2),
            customsDuty = moneyAt(4),
            exciseDuty = moneyAt(6),
            vat = moneyAt(8),
            total = moneyAt(10),
            usesConservativeExcise = values[12] as Boolean,
        )
    },
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
        presets = uiState.presets,
        onSavePreset = viewModel::savePreset,
        onDeletePreset = viewModel::deletePreset,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImportCalculatorScreen(
    onBack: () -> Unit,
    displayCurrency: Currency,
    exchangeRates: ExchangeRates?,
    presets: List<ImportAssumptionPreset>,
    onSavePreset: (String, Double, Double, Double) -> Unit,
    onDeletePreset: (String) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val numberSymbols = remember(locale) { DecimalFormatSymbols.getInstance(locale) }
    val defaultShippingUsd = ImportCostCalculator.DEFAULT_US_SHIPPING_USD
    val defaultShippingText = amountText(defaultShippingUsd, numberSymbols)
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
    var shippingText by rememberSaveable { mutableStateOf(defaultShippingText) }
    var customsRateText by rememberSaveable { mutableStateOf(defaultCustomsPercentText) }
    var vatRateText by rememberSaveable { mutableStateOf(defaultVatPercentText) }
    var engineText by rememberSaveable { mutableStateOf("") }
    var fuelName by rememberSaveable { mutableStateOf(FuelType.PETROL.name) }
    var showAssumptions by rememberSaveable { mutableStateOf(false) }

    val fuel = FuelType.entries.firstOrNull { it.name == fuelName } ?: FuelType.PETROL
    val input = evaluateImportCalculatorInput(
        fields = ImportCalculatorFields(
            vehiclePriceText = vehiclePriceText,
            shippingText = shippingText,
            customsRateText = customsRateText,
            vatRateText = vatRateText,
            engineText = engineText,
            fuel = fuel,
        ),
        numberSymbols = numberSymbols,
    )
    val estimate = input.estimate

    val fallbackEstimate = remember {
        ImportCostCalculator.estimate(
            vehiclePriceUsd = ImportCostCalculator.DEFAULT_VEHICLE_PRICE_USD,
            shippingUsd = ImportCostCalculator.DEFAULT_US_SHIPPING_USD,
            engineCapacityCc = null,
            fuelType = FuelType.PETROL,
        )
    }
    var lastValidEstimate by rememberSaveable(stateSaver = ImportCostEstimateSaver) {
        mutableStateOf(estimate ?: fallbackEstimate)
    }
    SideEffect {
        if (estimate != null && estimate != lastValidEstimate) {
            lastValidEstimate = estimate
        }
    }
    val displayedEstimate = estimate ?: lastValidEstimate
    val shareText = if (estimate != null) {
        val engineSummary = when {
            !input.engineRequired -> stringResource(R.string.import_share_engine_not_applicable)
            input.engineCapacityCc != null -> stringResource(
                R.string.import_share_engine_cc,
                input.engineCapacityCc.toString(),
            )
            else -> stringResource(R.string.import_share_engine_unknown)
        }
        val convertedTotalSummary = if (
            exchangeRates != null && estimate.total.currency != displayCurrency
        ) {
            stringResource(
                R.string.import_share_converted_total,
                displayCurrency.name,
                exchangeRates.convert(estimate.total, displayCurrency).formatted(),
            )
        } else {
            null
        }
        val staleRatesSummary = if (convertedTotalSummary != null && exchangeRates?.isStale == true) {
            stringResource(R.string.listing_rates_stale)
        } else {
            null
        }
        listOfNotNull(
            stringResource(R.string.import_share_title),
            stringResource(
                R.string.import_share_breakdown,
                estimate.vehiclePrice.formatted(),
                estimate.shipping.formatted(),
                estimate.customsDuty.formatted(),
                estimate.exciseDuty.formatted(),
                estimate.vat.formatted(),
                estimate.total.formatted(),
            ),
            convertedTotalSummary,
            staleRatesSummary,
            stringResource(
                R.string.import_share_assumptions,
                shippingText,
                customsRateText,
                vatRateText,
                fuel.displayLabel(),
                engineSummary,
            ),
            stringResource(R.string.import_calculator_disclaimer),
        ).joinToString("\n\n")
    } else {
        null
    }
    val shareAction = remember(context, shareText) {
        shareText?.let { text -> { shareImportEstimate(context, text) } }
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
                isError = input.vehiclePriceInvalid,
                supportingText = if (input.vehiclePriceInvalid) {
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
                            label = { Text(option.displayLabel()) },
                        )
                    }
                }
            }

            if (input.engineRequired) {
                OutlinedTextField(
                    value = engineText,
                    onValueChange = { engineText = it },
                    label = { Text(stringResource(R.string.import_engine_cc)) },
                    singleLine = true,
                    isError = input.engineInvalid,
                    supportingText = if (input.engineInvalid) {
                        { Text(stringResource(R.string.import_invalid_engine_cc)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            ImportEstimateBreakdown(
                estimate = displayedEstimate,
                displayCurrency = displayCurrency,
                exchangeRates = exchangeRates,
                isPrevious = estimate == null,
                onShare = shareAction,
            )

            AssumptionsCard(
                expanded = showAssumptions,
                assumptionsAreDefault = input.assumptionsAreDefault,
                canCollapse = input.assumptionsReady,
                shippingText = shippingText,
                customsRateText = customsRateText,
                vatRateText = vatRateText,
                shippingInvalid = input.shippingInvalid,
                customsRateInvalid = input.customsRateInvalid,
                vatRateInvalid = input.vatRateInvalid,
                defaultShippingText = defaultShippingText,
                defaultCustomsPercentText = defaultCustomsPercentText,
                defaultVatPercentText = defaultVatPercentText,
                presets = presets,
                canSavePreset = input.assumptionsReady,
                onExpandedChange = { showAssumptions = it },
                onShippingChange = { shippingText = it },
                onCustomsRateChange = { customsRateText = it },
                onVatRateChange = { vatRateText = it },
                onApplyPreset = { preset ->
                    shippingText = amountText(preset.shippingUsd, numberSymbols)
                    customsRateText = ratePercentText(preset.customsDutyRate, numberSymbols)
                    vatRateText = ratePercentText(preset.vatRate, numberSymbols)
                },
                onSavePreset = { name ->
                    val shippingUsd = input.shippingUsd
                    val customsDutyRate = input.customsDutyRate
                    val vatRate = input.vatRate
                    if (shippingUsd != null && customsDutyRate != null && vatRate != null) {
                        onSavePreset(name, shippingUsd, customsDutyRate, vatRate)
                    }
                },
                onDeletePreset = onDeletePreset,
                onReset = {
                    shippingText = defaultShippingText
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
    defaultShippingText: String,
    defaultCustomsPercentText: String,
    defaultVatPercentText: String,
    presets: List<ImportAssumptionPreset>,
    canSavePreset: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onShippingChange: (String) -> Unit,
    onCustomsRateChange: (String) -> Unit,
    onVatRateChange: (String) -> Unit,
    onApplyPreset: (ImportAssumptionPreset) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
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
                            when {
                                !canCollapse -> R.string.import_assumptions_incomplete
                                assumptionsAreDefault -> R.string.import_assumptions_default
                                else -> R.string.import_assumptions_custom
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

            if (expanded && !canCollapse) {
                Text(
                    stringResource(R.string.import_assumptions_finish),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
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
                                stringResource(R.string.import_default_shipping_usd, defaultShippingText)
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
                ImportPresetControls(
                    presets = presets,
                    canSave = canSavePreset,
                    onApply = onApplyPreset,
                    onSave = onSavePreset,
                    onDelete = onDeletePreset,
                )
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.import_reset_assumptions))
                }
            }
        }
    }
}

private fun amountText(amount: Double, symbols: DecimalFormatSymbols): String =
    BigDecimal.valueOf(amount)
        .stripTrailingZeros()
        .toPlainString()
        .replace('.', symbols.decimalSeparator)

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
    isPrevious: Boolean,
    onShare: (() -> Unit)?,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(
                    if (isPrevious) R.string.import_previous_estimate
                    else R.string.import_estimated_total,
                ),
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
            if (onShare != null) {
                TextButton(onClick = onShare) {
                    Text(stringResource(R.string.import_share))
                }
            }
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

private fun shareImportEstimate(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(send, null))
    }.onFailure {
        Toast.makeText(context, R.string.import_share_failed, Toast.LENGTH_SHORT).show()
    }
}
