package com.autka.feature.listings

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.autka.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autka.core.model.CarOffer
import com.autka.core.model.Currency
import com.autka.core.model.ExchangeRates
import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import com.autka.feature.external.MarketplaceLinksRow
import com.autka.ui.components.EmptyState
import com.autka.ui.components.OfferImage
import com.autka.ui.components.LoadingIndicator
import com.autka.ui.components.formatted
import com.autka.ui.components.kmOrDash

@Composable
fun ListingsRoute(
    onOfferClick: (String) -> Unit,
    onMapClick: () -> Unit,
    onImportCalculatorClick: () -> Unit,
    onSourceHealthClick: () -> Unit,
    viewModel: ListingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ListingsScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::refresh,
        onApplyFilter = viewModel::onApplyFilter,
        onResetFilter = viewModel::onResetFilter,
        onDisplayCurrencyChange = viewModel::onDisplayCurrencyChange,
        onOfferClick = onOfferClick,
        onMapClick = onMapClick,
        onImportCalculatorClick = onImportCalculatorClick,
        onSourceHealthClick = onSourceHealthClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingsScreen(
    uiState: ListingsUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onApplyFilter: (SearchFilter) -> Unit,
    onResetFilter: () -> Unit,
    onDisplayCurrencyChange: (Currency) -> Unit,
    onOfferClick: (String) -> Unit,
    onMapClick: () -> Unit,
    onImportCalculatorClick: () -> Unit,
    onSourceHealthClick: () -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    CurrencyMenu(
                        selected = uiState.displayCurrency,
                        onSelect = onDisplayCurrencyChange,
                    )
                    IconButton(onClick = onMapClick) {
                        Icon(Icons.Default.Map, contentDescription = stringResource(R.string.cd_map))
                    }
                    IconButton(onClick = { showFilters = true }) {
                        BadgedBox(
                            badge = {
                                if (uiState.activeFilterCount > 0) {
                                    Badge { Text(uiState.activeFilterCount.toString()) }
                                }
                            },
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.cd_filters))
                        }
                    }
                    Box {
                        IconButton(onClick = { showMore = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_options),
                            )
                        }
                        DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_calculator)) },
                                leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                                onClick = { showMore = false; onImportCalculatorClick() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.source_health_title)) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = { showMore = false; onSourceHealthClick() },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.filter.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                label = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )

            if (uiState.failedSources.isNotEmpty()) {
                Text(
                    stringResource(
                        R.string.listing_failed_sources,
                        uiState.failedSources.joinToString(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (uiState.ratesAreStale) {
                Text(
                    stringResource(R.string.listing_rates_stale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            when {
                uiState.isRefreshing && uiState.offers.isEmpty() -> LoadingIndicator()
                uiState.offers.isEmpty() -> Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    EmptyState(
                        if (uiState.activeFilterCount > 0) stringResource(R.string.empty_no_match)
                        else stringResource(R.string.empty_no_offers),
                    )
                    // No results in our cache: send the user to each marketplace's own
                    // pre-filled search instead. This remains a deep-link, never ingest.
                    MarketplaceLinksRow(filter = uiState.filter)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.offers, key = { it.id }) { offer ->
                        OfferCard(
                            offer = offer,
                            displayCurrency = uiState.displayCurrency,
                            rates = uiState.exchangeRates,
                            onClick = { onOfferClick(offer.id) },
                        )
                    }
                    // Footer keeps the same compliant hand-off for the active filter.
                    item { MarketplaceLinksRow(filter = uiState.filter) }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            filter = uiState.filter,
            availableMakes = uiState.availableMakes,
            availableSources = uiState.availableSources,
            priceCurrency = uiState.displayCurrency,
            onApply = { onApplyFilter(it); showFilters = false },
            onReset = { onResetFilter(); showFilters = false },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun CurrencyMenu(selected: Currency, onSelect: (Currency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) {
        Text(selected.name, color = MaterialTheme.colorScheme.primary)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        Currency.entries.forEach { currency ->
            DropdownMenuItem(
                text = { Text("${currency.name} (${currency.symbol})") },
                onClick = { onSelect(currency); expanded = false },
            )
        }
    }
}

@Composable
private fun OfferCard(
    offer: CarOffer,
    displayCurrency: Currency,
    rates: ExchangeRates?,
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OfferImage(
                url = offer.thumbnailUrl,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(offer.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(offer.price.formatted(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    if (rates != null && offer.price.currency != displayCurrency) {
                        Text(
                            "~ ${rates.convert(offer.price, displayCurrency).formatted()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text("${offer.year ?: "--"} | ${offer.mileageKm.kmOrDash()}", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(offer.location ?: "--", style = MaterialTheme.typography.bodySmall)
                    RegionBadge(offer.region)
                }
                offer.listingCount?.takeIf { it > 1 }?.let {
                    Text(
                        pluralStringResource(R.plurals.listed_on_sites, it, it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                offer.importEstimate?.let {
                    Text(
                        stringResource(
                            R.string.listing_estimated_landed_cost,
                            it.total.formatted(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionBadge(region: Region) {
    val label = when (region) {
        Region.POLAND -> "PL"
        Region.EUROPE -> "EU"
        Region.USA -> stringResource(R.string.region_usa_badge)
    }
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
}
