package com.autka.feature.sourcehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autka.R
import com.autka.core.model.SourceHealth
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SourceHealthRoute(
    onBack: () -> Unit,
    viewModel: SourceHealthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SourceHealthScreen(
        uiState = uiState,
        onBack = onBack,
        onRefresh = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceHealthScreen(
    uiState: SourceHealthUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.source_health_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !uiState.isLoading) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.isLoading && uiState.sources.isNotEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (uiState.loadFailed) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.source_health_error),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRefresh, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            when {
                uiState.isLoading && uiState.sources.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.loadFailed && uiState.sources.isEmpty() -> Unit
                uiState.sources.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.source_health_empty),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.sources, key = { it.id }) { source ->
                            SourceHealthCard(source)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceHealthCard(source: SourceHealth) {
    val (statusText, statusColor) = sourceStatus(source)
    val locale = LocalConfiguration.current.locales[0]
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                source.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(statusText, style = MaterialTheme.typography.labelMedium, color = statusColor)
            if (source.displayName != source.id) {
                Text(
                    source.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            source.offerCount?.let {
                Text(stringResource(R.string.source_health_offers, it), style = MaterialTheme.typography.bodySmall)
            }
            source.lastCompletedAtEpochMs?.let {
                Text(
                    stringResource(R.string.source_health_last_completed, formatTimestamp(it, locale)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            source.lastOffersUpserted?.let {
                Text(
                    stringResource(R.string.source_health_last_upserted, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun sourceStatus(source: SourceHealth): Pair<String, Color> = when {
    !source.enabled -> stringResource(R.string.source_health_disabled) to MaterialTheme.colorScheme.onSurfaceVariant
    source.offerCount == null -> stringResource(R.string.source_health_unavailable) to MaterialTheme.colorScheme.error
    source.lastCompletedOk == true -> stringResource(R.string.source_health_healthy) to MaterialTheme.colorScheme.primary
    source.lastCompletedOk == false -> stringResource(R.string.source_health_failed) to MaterialTheme.colorScheme.error
    else -> stringResource(R.string.source_health_no_run) to MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatTimestamp(epochMs: Long, locale: Locale): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(epochMs))
