package com.autka.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.autka.core.model.Currency
import com.autka.core.model.ExchangeRates
import com.autka.data.remote.rates.NbpRateProvider
import com.autka.data.remote.rates.StaticRateProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultExchangeRateRepository @Inject constructor(
    private val live: NbpRateProvider,
    staticRates: StaticRateProvider,
    private val dataStore: DataStore<Preferences>,
) : ExchangeRateRepository {

    private enum class RefreshOutcome { COMPLETED, CANCELLED }

    // Static fallback is the immediate initial value so rates() never blocks.
    private val state = MutableStateFlow(staticRates.snapshot())
    private val refreshStateLock = Any()
    private var inFlightRefresh: CompletableDeferred<RefreshOutcome>? = null
    private var hydrated = false

    override fun rates(): StateFlow<ExchangeRates> = state.asStateFlow()

    override suspend fun refresh() {
        while (true) {
            val (flight, leader) = synchronized(refreshStateLock) {
                val current = inFlightRefresh
                if (current != null) {
                    current to false
                } else {
                    CompletableDeferred<RefreshOutcome>().also { inFlightRefresh = it } to true
                }
            }

            if (!leader) {
                when (flight.await()) {
                    RefreshOutcome.COMPLETED -> return
                    RefreshOutcome.CANCELLED -> continue
                }
            }

            var outcome = RefreshOutcome.CANCELLED
            var liveApplied = false
            try {
                refreshOnce { liveApplied = true }
                outcome = RefreshOutcome.COMPLETED
                return
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                if (liveApplied) outcome = RefreshOutcome.COMPLETED
                synchronized(refreshStateLock) {
                    if (inFlightRefresh === flight) inFlightRefresh = null
                    // Complete while holding the same lock used to discover in-flight work,
                    // so a late caller can never observe an already-finished stale flight.
                    flight.complete(outcome)
                }
            }
        }
    }

    private suspend fun refreshOnce(onLiveApplied: () -> Unit) {
        hydrateFromCacheOnce()
        val fresh = try {
            live.latest()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return
        }

        state.value = fresh
        // A successful live response is authoritative for this repository instance.
        // Do not re-hydrate it from disk as stale if a later refresh fails.
        hydrated = true
        onLiveApplied()

        try {
            persist(fresh)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Persistence is only a cold-start optimization; keep the fresh in-memory rates.
        }
    }

    /**
     * Seed from the last persisted snapshot exactly once, so a cold start shows the
     * most recent real rates instead of the static seed while the network call is in
     * flight (or if it never succeeds). A failed DataStore read is retried later.
     */
    private suspend fun hydrateFromCacheOnce() {
        if (hydrated) return
        val cached = try {
            readCached()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return
        }
        hydrated = true
        cached?.let { state.value = it }
    }

    private suspend fun readCached(): ExchangeRates? {
        val prefs = dataStore.data.first()
        val base = prefs[KEY_BASE]
            ?.let { runCatching { Currency.valueOf(it) }.getOrNull() }
            ?: return null
        val perUnit = prefs[KEY_PER_UNIT]?.let(::decodeRates).orEmpty()
        if (!perUnit.hasRequiredRates()) return null
        return ExchangeRates(
            base = base,
            perUnit = perUnit,
            asOfEpochMs = prefs[KEY_AS_OF] ?: 0L,
            isStale = true, // cached until a fresh live request succeeds
        )
    }

    private suspend fun persist(rates: ExchangeRates) {
        dataStore.edit { prefs ->
            prefs[KEY_BASE] = rates.base.name
            prefs[KEY_PER_UNIT] = encodeRates(rates.perUnit)
            prefs[KEY_AS_OF] = rates.asOfEpochMs
        }
    }

    private companion object {
        val KEY_BASE = stringPreferencesKey("rates_base")
        val KEY_PER_UNIT = stringPreferencesKey("rates_per_unit")
        val KEY_AS_OF = longPreferencesKey("rates_as_of_epoch_ms")
        val REQUIRED_CURRENCIES = setOf(Currency.PLN, Currency.EUR, Currency.USD)

        fun Map<Currency, Double>.hasRequiredRates(): Boolean =
            REQUIRED_CURRENCIES.all { currency ->
                this[currency]?.let { it.isFinite() && it > 0.0 } == true
            }

        // "PLN:1.0;EUR:4.3;USD:4.0" — dependency-free, no serialization lib needed.
        fun encodeRates(perUnit: Map<Currency, Double>): String =
            perUnit.entries.joinToString(";") { (c, v) -> "${c.name}:$v" }

        fun decodeRates(encoded: String): Map<Currency, Double> =
            encoded.split(";").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size != 2) return@mapNotNull null
                val currency = runCatching { Currency.valueOf(parts[0]) }.getOrNull()
                    ?: return@mapNotNull null
                val rate = parts[1].toDoubleOrNull()
                    ?.takeIf { it.isFinite() && it > 0.0 }
                    ?: return@mapNotNull null
                currency to rate
            }.toMap()
    }
}
