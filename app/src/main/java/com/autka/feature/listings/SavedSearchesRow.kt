package com.autka.feature.listings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.autka.R
import com.autka.core.model.SavedSearch
import com.autka.core.model.SearchFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSearchesRow(
    filter: SearchFilter,
    savedSearches: List<SavedSearch>,
    canSave: Boolean,
    onSave: (String) -> Unit,
    onApply: (SavedSearch) -> Unit,
    onDelete: (String) -> Unit,
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf("") }
    val fallbackName = stringResource(R.string.saved_search_default_name)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.saved_searches),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canSave) {
                item(key = "save-current-search") {
                    AssistChip(
                        onClick = {
                            draftName = suggestedSavedSearchName(filter, fallbackName)
                            showSaveDialog = true
                        },
                        label = { Text(stringResource(R.string.save_search)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }

            items(savedSearches, key = { it.id }) { saved ->
                InputChip(
                    selected = saved.filter == filter,
                    onClick = { onApply(saved) },
                    label = { Text(saved.name) },
                    trailingIcon = {
                        IconButton(
                            onClick = { onDelete(saved.id) },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(
                                    R.string.saved_search_delete,
                                    saved.name,
                                ),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                )
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.save_search)) },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it.take(80) },
                    label = { Text(stringResource(R.string.saved_search_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = draftName.isNotBlank(),
                    onClick = {
                        onSave(draftName)
                        showSaveDialog = false
                    },
                ) { Text(stringResource(R.string.saved_search_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.saved_search_cancel))
                }
            },
        )
    }
}

private fun suggestedSavedSearchName(filter: SearchFilter, fallback: String): String {
    val vehicle = listOfNotNull(filter.make, filter.model)
        .joinToString(" ")
        .trim()
    return vehicle
        .ifBlank { filter.query.trim() }
        .ifBlank { fallback }
        .take(80)
}
