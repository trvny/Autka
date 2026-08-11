package com.autka.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autka.feature.listings.ListingsViewModel

/** Map view of the active listings search, sharing its filters and cached results. */
@Composable
fun MapRoute(
    onBack: () -> Unit,
    onOfferClick: (String) -> Unit,
    viewModel: ListingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(offers = uiState.offers, onBack = onBack, onOfferClick = onOfferClick)
}
