package com.autka.feature.importcalc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autka.core.model.Currency
import com.autka.core.model.ExchangeRates
import com.autka.core.model.ImportAssumptionPreset
import com.autka.data.repository.ExchangeRateRepository
import com.autka.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ImportCalculatorUiState(
    val displayCurrency: Currency = Currency.PLN,
    val exchangeRates: ExchangeRates? = null,
    val presets: List<ImportAssumptionPreset> = emptyList(),
)

@HiltViewModel
class ImportCalculatorViewModel @Inject constructor(
    private val rateRepository: ExchangeRateRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<ImportCalculatorUiState> = combine(
        rateRepository.rates(),
        settingsRepository.displayCurrency,
        settingsRepository.importAssumptionPresets,
    ) { rates, currency, presets ->
        ImportCalculatorUiState(
            displayCurrency = currency,
            exchangeRates = rates,
            presets = presets,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        // Do not render a converted total until the persisted display currency emits.
        initialValue = ImportCalculatorUiState(),
    )

    init {
        viewModelScope.launch { rateRepository.refresh() }
    }

    fun savePreset(
        name: String,
        shippingUsd: Double,
        customsDutyRate: Double,
        vatRate: Double,
    ) {
        viewModelScope.launch {
            settingsRepository.saveImportAssumptionPreset(
                name = name,
                shippingUsd = shippingUsd,
                customsDutyRate = customsDutyRate,
                vatRate = vatRate,
            )
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch { settingsRepository.deleteImportAssumptionPreset(id) }
    }
}
