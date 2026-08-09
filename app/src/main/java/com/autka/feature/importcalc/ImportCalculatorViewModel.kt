package com.autka.feature.importcalc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autka.core.model.Currency
import com.autka.core.model.ExchangeRates
import com.autka.data.repository.ExchangeRateRepository
import com.autka.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportCalculatorUiState(
    val displayCurrency: Currency = Currency.PLN,
    val exchangeRates: ExchangeRates? = null,
)

@HiltViewModel
class ImportCalculatorViewModel @Inject constructor(
    private val rateRepository: ExchangeRateRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<ImportCalculatorUiState> = combine(
        rateRepository.rates(),
        settingsRepository.displayCurrency,
    ) { rates, currency ->
        ImportCalculatorUiState(displayCurrency = currency, exchangeRates = rates)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        // Do not render a converted total until the persisted display currency emits.
        initialValue = ImportCalculatorUiState(),
    )

    init {
        viewModelScope.launch { rateRepository.refresh() }
    }
}
