package com.autka.feature.importcalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.autka.R
import com.autka.core.model.ImportAssumptionPreset
import com.autka.core.model.MAX_IMPORT_PRESET_NAME_LENGTH

@Composable
internal fun ImportPresetControls(
    presets: List<ImportAssumptionPreset>,
    canSave: Boolean,
    onApply: (ImportAssumptionPreset) -> Unit,
    onSave: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var presetName by rememberSaveable { mutableStateOf("") }
    val normalizedPresetName = presetName.trim().take(MAX_IMPORT_PRESET_NAME_LENGTH)

    if (presets.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.import_presets))
            presets.forEach { preset ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { onApply(preset) }, modifier = Modifier.weight(1f)) {
                        Text(preset.name)
                    }
                    TextButton(onClick = { onDelete(preset.id) }) {
                        Text(stringResource(R.string.import_preset_delete))
                    }
                }
            }
        }
    }

    TextButton(
        enabled = canSave,
        onClick = {
            presetName = ""
            showSaveDialog = true
        },
    ) {
        Text(stringResource(R.string.import_preset_save))
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.import_preset_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it.take(MAX_IMPORT_PRESET_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.import_preset_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = normalizedPresetName.isNotEmpty(),
                    onClick = {
                        onSave(normalizedPresetName)
                        showSaveDialog = false
                    },
                ) {
                    Text(stringResource(R.string.import_preset_save_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.import_preset_cancel))
                }
            },
        )
    }
}
