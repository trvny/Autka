package com.autka.data.remote.rates

import com.autka.core.model.Currency
import com.autka.core.model.ExchangeRates
import javax.inject.Inject

/**
 * Live PLN-based rates from NBP. NBP "mid" is PLN per 1 unit of the foreign currency,
 * which is exactly the "base per unit" convention [ExchangeRates] expects.
 */
class NbpRateProvider @Inject constructor(
    private val api: NbpApi,
) : RateProvider {

    override suspend fun latest(): ExchangeRates {
        val rates = api.tableA().firstOrNull()?.rates.orEmpty()
        val byCode = rates.associate { it.code.uppercase() to it.mid }
        val eur = byCode["EUR"].validRateOrNull()
            ?: error("NBP response missing a valid EUR rate")
        val usd = byCode["USD"].validRateOrNull()
            ?: error("NBP response missing a valid USD rate")
        return ExchangeRates(
            base = Currency.PLN,
            perUnit = mapOf(
                Currency.PLN to 1.0,
                Currency.EUR to eur,
                Currency.USD to usd,
            ),
            asOfEpochMs = System.currentTimeMillis(),
            isStale = false,
        )
    }

    private fun Double?.validRateOrNull(): Double? =
        this?.takeIf { it.isFinite() && it > 0.0 }
}
