package com.autka.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.autka.core.model.Currency
import com.autka.data.remote.rates.NbpApi
import com.autka.data.remote.rates.NbpRate
import com.autka.data.remote.rates.NbpRateProvider
import com.autka.data.remote.rates.NbpTable
import com.autka.data.remote.rates.StaticRateProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeRateRepositoryTest {

    @Test
    fun `cached rates stay stale when live refresh fails`() = runTest {
        val store = FakePreferencesDataStore()

        val online = DefaultExchangeRateRepository(
            live = provider(eur = 4.2, usd = 3.9),
            staticRates = StaticRateProvider(),
            dataStore = store,
        )
        online.refresh()
        assertFalse(online.rates().value.isStale)

        val offline = DefaultExchangeRateRepository(
            live = failingProvider(),
            staticRates = StaticRateProvider(),
            dataStore = store,
        )
        offline.refresh()

        val cached = offline.rates().value
        assertTrue(cached.isStale)
        assertEquals(4.2, cached.perUnit[Currency.EUR] ?: error("EUR missing"), 0.0)
        assertEquals(3.9, cached.perUnit[Currency.USD] ?: error("USD missing"), 0.0)
    }

    @Test
    fun `incomplete live snapshot keeps cached rates`() = runTest {
        val store = FakePreferencesDataStore()
        DefaultExchangeRateRepository(
            live = provider(eur = 4.2, usd = 3.9),
            staticRates = StaticRateProvider(),
            dataStore = store,
        ).refresh()

        val incomplete = NbpRateProvider(object : NbpApi {
            override suspend fun tableA(): List<NbpTable> = listOf(
                NbpTable(rates = listOf(NbpRate(code = "EUR", mid = 4.5))),
            )
        })
        val repository = DefaultExchangeRateRepository(
            live = incomplete,
            staticRates = StaticRateProvider(),
            dataStore = store,
        )

        repository.refresh()

        val rates = repository.rates().value
        assertTrue(rates.isStale)
        assertEquals(4.2, rates.perUnit[Currency.EUR] ?: error("EUR missing"), 0.0)
        assertEquals(3.9, rates.perUnit[Currency.USD] ?: error("USD missing"), 0.0)
    }

    @Test
    fun `incomplete persisted snapshot is ignored`() = runTest {
        val persisted = preferencesOf(
            stringPreferencesKey("rates_base") to "PLN",
            stringPreferencesKey("rates_per_unit") to "PLN:1.0;EUR:4.2",
            longPreferencesKey("rates_as_of_epoch_ms") to 123L,
        )
        val fallback = StaticRateProvider().snapshot()
        val repository = DefaultExchangeRateRepository(
            live = failingProvider(),
            staticRates = StaticRateProvider(),
            dataStore = FakePreferencesDataStore(persisted),
        )

        repository.refresh()

        assertEquals(fallback, repository.rates().value)
    }

    @Test
    fun `failed cache read is retried on the next refresh`() = runTest {
        val store = FakePreferencesDataStore()
        DefaultExchangeRateRepository(
            live = provider(eur = 4.2, usd = 3.9),
            staticRates = StaticRateProvider(),
            dataStore = store,
        ).refresh()
        store.failNextRead()

        val offline = DefaultExchangeRateRepository(
            live = failingProvider(),
            staticRates = StaticRateProvider(),
            dataStore = store,
        )
        offline.refresh()
        offline.refresh()

        val cached = offline.rates().value
        assertTrue(cached.isStale)
        assertEquals(4.2, cached.perUnit[Currency.EUR] ?: error("EUR missing"), 0.0)
        assertEquals(3.9, cached.perUnit[Currency.USD] ?: error("USD missing"), 0.0)
    }

    @Test
    fun `successful live refresh prevents later stale rehydration`() = runTest {
        val store = FakePreferencesDataStore()
        store.failNextRead()
        var liveCalls = 0
        val live = NbpRateProvider(object : NbpApi {
            override suspend fun tableA(): List<NbpTable> {
                liveCalls += 1
                if (liveCalls == 1) return table(eur = 4.4, usd = 4.0)
                error("network unavailable")
            }
        })
        val repository = DefaultExchangeRateRepository(
            live = live,
            staticRates = StaticRateProvider(),
            dataStore = store,
        )

        repository.refresh()
        repository.refresh()

        val rates = repository.rates().value
        assertFalse(rates.isStale)
        assertEquals(2, liveCalls)
        assertEquals(4.4, rates.perUnit[Currency.EUR] ?: error("EUR missing"), 0.0)
        assertEquals(4.0, rates.perUnit[Currency.USD] ?: error("USD missing"), 0.0)
    }

    @Test
    fun `persist failure does not discard fresh in-memory rates`() = runTest {
        val store = FakePreferencesDataStore()
        store.failNextWrite()
        val repository = DefaultExchangeRateRepository(
            live = provider(eur = 4.5, usd = 4.1),
            staticRates = StaticRateProvider(),
            dataStore = store,
        )

        repository.refresh()

        val rates = repository.rates().value
        assertFalse(rates.isStale)
        assertEquals(4.5, rates.perUnit[Currency.EUR] ?: error("EUR missing"), 0.0)
        assertEquals(4.1, rates.perUnit[Currency.USD] ?: error("USD missing"), 0.0)
    }

    @Test
    fun `concurrent refresh is coalesced`() = runTest {
        val seedStore = FakePreferencesDataStore()
        DefaultExchangeRateRepository(
            live = provider(eur = 4.2, usd = 3.9),
            staticRates = StaticRateProvider(),
            dataStore = seedStore,
        ).refresh()

        val readStarted = CompletableDeferred<Unit>()
        val releaseRead = CompletableDeferred<Unit>()
        val store = DelayedPreferencesDataStore(seedStore.snapshot(), readStarted, releaseRead)
        var liveCalls = 0
        val live = NbpRateProvider(object : NbpApi {
            override suspend fun tableA(): List<NbpTable> {
                liveCalls += 1
                return table(eur = 4.4, usd = 4.0)
            }
        })
        val repository = DefaultExchangeRateRepository(
            live = live,
            staticRates = StaticRateProvider(),
            dataStore = store,
        )

        val first = launch { repository.refresh() }
        readStarted.await()
        val second = launch { repository.refresh() }
        runCurrent()
        releaseRead.complete(Unit)
        first.join()
        second.join()

        val rates = repository.rates().value
        assertFalse(rates.isStale)
        assertEquals(1, liveCalls)
        assertEquals(4.4, rates.perUnit[Currency.EUR] ?: error("EUR missing"), 0.0)
        assertEquals(4.0, rates.perUnit[Currency.USD] ?: error("USD missing"), 0.0)
    }

    @Test
    fun `waiting refresh retries when leader is cancelled`() = runTest {
        val firstStarted = CompletableDeferred<Unit>()
        var liveCalls = 0
        val live = NbpRateProvider(object : NbpApi {
            override suspend fun tableA(): List<NbpTable> {
                liveCalls += 1
                if (liveCalls == 1) {
                    firstStarted.complete(Unit)
                    awaitCancellation()
                }
                return table(eur = 4.6, usd = 4.2)
            }
        })
        val repository = DefaultExchangeRateRepository(
            live = live,
            staticRates = StaticRateProvider(),
            dataStore = FakePreferencesDataStore(),
        )

        val first = launch { repository.refresh() }
        firstStarted.await()
        val second = launch { repository.refresh() }
        runCurrent()
        first.cancelAndJoin()
        second.join()

        val rates = repository.rates().value
        assertEquals(2, liveCalls)
        assertFalse(rates.isStale)
        assertEquals(4.6, rates.perUnit[Currency.EUR] ?: error("EUR missing"), 0.0)
        assertEquals(4.2, rates.perUnit[Currency.USD] ?: error("USD missing"), 0.0)
    }

    private fun provider(eur: Double, usd: Double): NbpRateProvider =
        NbpRateProvider(object : NbpApi {
            override suspend fun tableA(): List<NbpTable> = table(eur, usd)
        })

    private fun failingProvider(): NbpRateProvider = NbpRateProvider(object : NbpApi {
        override suspend fun tableA(): List<NbpTable> = error("network unavailable")
    })

    private fun table(eur: Double, usd: Double): List<NbpTable> = listOf(
        NbpTable(
            rates = listOf(
                NbpRate(code = "EUR", mid = eur),
                NbpRate(code = "USD", mid = usd),
            ),
        ),
    )
}

private class FakePreferencesDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    private var readsToFail = 0
    private var writesToFail = 0

    override val data: Flow<Preferences> = flow {
        if (readsToFail > 0) {
            readsToFail -= 1
            error("store unavailable")
        }
        emit(state.value)
    }

    fun snapshot(): Preferences = state.value

    fun failNextRead() {
        readsToFail += 1
    }

    fun failNextWrite() {
        writesToFail += 1
    }

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        if (writesToFail > 0) {
            writesToFail -= 1
            error("store unavailable")
        }
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

private class DelayedPreferencesDataStore(
    initial: Preferences,
    private val readStarted: CompletableDeferred<Unit>,
    private val releaseRead: CompletableDeferred<Unit>,
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)

    override val data: Flow<Preferences> = flow {
        val snapshot = state.value
        readStarted.complete(Unit)
        releaseRead.await()
        emit(snapshot)
    }

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
