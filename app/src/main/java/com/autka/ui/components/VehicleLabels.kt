package com.autka.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.autka.R
import com.autka.core.model.FuelType
import com.autka.core.model.Transmission

@Composable
fun FuelType.displayLabel(): String = when (this) {
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

@Composable
fun Transmission.displayLabel(): String = when (this) {
    Transmission.MANUAL -> stringResource(R.string.trans_manual)
    Transmission.AUTOMATIC -> stringResource(R.string.trans_automatic)
    Transmission.UNKNOWN -> stringResource(R.string.trans_unknown)
}
