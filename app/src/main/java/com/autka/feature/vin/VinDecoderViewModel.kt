package com.autka.feature.vin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autka.core.model.VinDecodeResult
import com.autka.data.repository.VinDecoderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VinDecoderUiState(
    val vin: String = "",
    val isLoading: Boolean = false,
    val validationError: Boolean = false,
    val loadFailed: Boolean = false,
    val result: VinDecodeResult? = null,
)

@HiltViewModel
class VinDecoderViewModel @Inject constructor(
    private val repository: VinDecoderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VinDecoderUiState())
    val uiState = _uiState.asStateFlow()
    private var decodeJob: Job? = null

    fun onVinChange(value: String) {
        _uiState.value = VinDecoderUiState(vin = normalizeVinInput(value))
    }

    fun decode() {
        if (decodeJob?.isActive == true) return
        val vin = _uiState.value.vin
        if (!isValidVin(vin)) {
            _uiState.update { it.copy(validationError = true, loadFailed = false, result = null) }
            return
        }

        _uiState.update { it.copy(isLoading = true, validationError = false, loadFailed = false, result = null) }
        decodeJob = viewModelScope.launch {
            try {
                val result = repository.decode(vin)
                _uiState.update { it.copy(isLoading = false, result = result) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }
}

internal fun normalizeVinInput(value: String): String = value
    .filter(Char::isLetterOrDigit)
    .uppercase(Locale.ROOT)
    .take(17)

internal fun isValidVin(vin: String): Boolean =
    vin.length == 17 && vin.all { char ->
        char.isDigit() || (char in 'A'..'Z' && char !in setOf('I', 'O', 'Q'))
    }
