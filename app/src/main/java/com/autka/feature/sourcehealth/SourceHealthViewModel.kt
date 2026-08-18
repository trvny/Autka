package com.autka.feature.sourcehealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autka.core.model.SourceHealth
import com.autka.data.repository.SourceHealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceHealthUiState(
    val sources: List<SourceHealth> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
)

@HiltViewModel
class SourceHealthViewModel @Inject constructor(
    private val repository: SourceHealthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SourceHealthUiState(sources = repository.cachedSources().sortedForDisplay()),
    )
    val uiState = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        refreshJob = viewModelScope.launch {
            try {
                val sources = repository.getSources()
                _uiState.value = SourceHealthUiState(
                    sources = sources.sortedForDisplay(),
                    isLoading = false,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { state -> state.copy(isLoading = false, loadFailed = true) }
            }
        }
    }
}

private fun List<SourceHealth>.sortedForDisplay(): List<SourceHealth> =
    sortedWith(compareByDescending<SourceHealth> { it.enabled }.thenBy { it.id })
