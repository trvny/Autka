package com.autka.feature.vin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autka.R
import com.autka.core.model.VinDecodeResult

@Composable
fun VinDecoderRoute(
    onBack: () -> Unit,
    viewModel: VinDecoderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VinDecoderScreen(
        uiState = uiState,
        onVinChange = viewModel::onVinChange,
        onDecode = viewModel::decode,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VinDecoderScreen(
    uiState: VinDecoderUiState,
    onVinChange: (String) -> Unit,
    onDecode: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vin_decoder_title)) },
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
                value = uiState.vin,
                onValueChange = onVinChange,
                label = { Text(stringResource(R.string.vin_decoder_input)) },
                supportingText = {
                    Text(
                        stringResource(
                            if (uiState.validationError) R.string.vin_decoder_invalid
                            else R.string.vin_decoder_hint,
                        ),
                    )
                },
                isError = uiState.validationError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { onDecode() }),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onDecode,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.vin_decoder_action))
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            if (uiState.loadFailed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.vin_decoder_error),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onDecode, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            uiState.result?.let { result ->
                VinResultCard(result)
            }

            Text(
                stringResource(R.string.vin_decoder_source_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VinResultCard(result: VinDecodeResult) {
    val title = listOfNotNull(result.make, result.model).joinToString(" ").ifBlank { result.vin }
    val plant = listOfNotNull(result.plantCity, result.plantCountry).joinToString(", ").ifBlank { null }

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            result.decoderWarning?.let {
                Text(
                    stringResource(R.string.vin_decoder_warning, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            VinRow(stringResource(R.string.vin_decoder_vin), result.vin)
            result.modelYear?.let { VinRow(stringResource(R.string.vin_decoder_year), it) }
            result.trim?.let { VinRow(stringResource(R.string.vin_decoder_trim), it) }
            result.series?.let { VinRow(stringResource(R.string.vin_decoder_series), it) }
            result.vehicleType?.let { VinRow(stringResource(R.string.vin_decoder_vehicle_type), it) }
            result.bodyClass?.let { VinRow(stringResource(R.string.vin_decoder_body), it) }
            result.fuelType?.let { VinRow(stringResource(R.string.vin_decoder_fuel), it) }
            result.electrificationLevel?.let {
                VinRow(stringResource(R.string.vin_decoder_electrification), it)
            }
            result.displacementLiters?.let {
                VinRow(
                    stringResource(R.string.vin_decoder_displacement),
                    stringResource(R.string.vin_decoder_liters, it),
                )
            }
            result.engineCylinders?.let { VinRow(stringResource(R.string.vin_decoder_cylinders), it) }
            result.engineHp?.let {
                VinRow(
                    stringResource(R.string.vin_decoder_power),
                    stringResource(R.string.vin_decoder_hp, it),
                )
            }
            result.driveType?.let { VinRow(stringResource(R.string.vin_decoder_drive), it) }
            result.transmissionStyle?.let { VinRow(stringResource(R.string.vin_decoder_transmission), it) }
            plant?.let { VinRow(stringResource(R.string.vin_decoder_plant), it) }
        }
    }
}

@Composable
private fun VinRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
        )
    }
}
