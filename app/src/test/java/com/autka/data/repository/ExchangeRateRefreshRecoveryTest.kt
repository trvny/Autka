package com.autka.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.autka.core.model.Currency
import com.autka.data.remote.rates.NbpApi
import com.autka.data.remote.rates.NbpRate
import com.autka.data.remote.rates.NbpRateProvider
import com.autka.data.remote.rates.NbpTable
import com.autka.data.remote.rates.StaticRateProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExchangeRateRefreshRecoveryTest {

    @Test
    fun `waiting refresh retries after unexpected leader failure`() = runTest {
        val firstStarted = CompletableDeferred<Unit>()
        val failFirst = CompletableDeferred<Unit>()
        var liveCalls = 0
        val live = NbpRateProvider(object : NbpApi {
            override suspend fun tableA(): List<NbpTable> {
                liveCalls += 1
                if (liveCalls == 1) {
                    firstStarted.complete(Unit)
                    failFirst.await()
                    throw AssertionError("unexpected provider failure")
                }
                return listOf(
                    NbpTable(
                        rates = listOf(
                            NbpRate(code = "EUR", mid = 4.7),
                            NbpRate(code = "USD", mid = 4.3),
                        ),
                    ),
                )
            }
        })
        val repository = DefaultExchangeRateRepository(
            live = live,
            staticRates = StaticRateProvider(),
            dataStore = MemoryPreferencesDataStore(),
        )

        val first = launch {
            try {
                repository.refresh()
            } catch (_: AssertionError) {
                // The leader may still surface an unexpected fatal failure to its caller.
            }
        }
        firstStarted.await()
        val second = launch { repository.refresh() }
        runCurrent()
        failFirst.complete(Unit)
        first.join()
        second.join()

        val rates = repository.rates().value
        assertEquals(2, liveCalls)
        assertFalse(rates.isStale)
        assertEquals(4.7, rates.perUnit[Currency.EUR] ?: error("EUR missing"), 0.0)
        assertEquals(4.3, rates.perUnit[Currency.USD] ?: error("USD missing"), 0.0)
    }
}

private class MemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
