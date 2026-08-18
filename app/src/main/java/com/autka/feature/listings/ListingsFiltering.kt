package com.autka.feature.listings

import com.autka.core.model.CarOffer
import com.autka.core.model.Currency
import com.autka.core.model.ExchangeRates
import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import com.autka.core.model.SortOrder

/**
 * Client-side application of a [SearchFilter] over locally cached offers, so the list
 * reacts instantly to filter changes. Price comparisons are done in a single
 * comparison currency ([comparisonCurrency]) using [rates], so mixed PLN/EUR/USD
 * results filter and sort correctly. The user's min/max price are interpreted in that
 * same comparison currency.
 */
fun List<CarOffer>.applyFilter(
    f: SearchFilter,
    rates: ExchangeRates,
    comparisonCurrency: Currency,
): List<CarOffer> = filter { o ->
    val price = rates.convert(o.price, comparisonCurrency).amount
    val minPrice = f.minPrice
    val maxPrice = f.maxPrice
    val minYear = f.minYear
    val maxYear = f.maxYear
    val maxMileageKm = f.maxMileageKm
    (f.query.isBlank() ||
        o.title.contains(f.query, ignoreCase = true) ||
        o.make.contains(f.query, ignoreCase = true) ||
        o.model.contains(f.query, ignoreCase = true)) &&
        (f.make == null || o.make.equals(f.make, ignoreCase = true)) &&
        (f.model == null || o.model.equals(f.model, ignoreCase = true)) &&
        (minPrice == null || price >= minPrice) &&
        (maxPrice == null || price <= maxPrice) &&
        (minYear == null || (o.year ?: Int.MIN_VALUE) >= minYear) &&
        (maxYear == null || (o.year ?: Int.MAX_VALUE) <= maxYear) &&
        (maxMileageKm == null || (o.mileageKm ?: Int.MAX_VALUE) <= maxMileageKm) &&
        (f.fuelTypes.isEmpty() || o.fuelType in f.fuelTypes) &&
        (f.transmissions.isEmpty() || o.transmission in f.transmissions) &&
        (o.region in f.regions) &&
        (f.sourceIds.isEmpty() || o.sourceId in f.sourceIds)
}

fun sortComparator(
    sort: SortOrder,
    rates: ExchangeRates,
    comparisonCurrency: Currency,
): Comparator<CarOffer> = when (sort) {
    SortOrder.NEWEST -> compareByDescending { it.postedAtEpochMs ?: 0L }
    SortOrder.PRICE_ASC -> compareBy { rates.convert(it.price, comparisonCurrency).amount }
    SortOrder.PRICE_DESC -> compareByDescending { rates.convert(it.price, comparisonCurrency).amount }
    SortOrder.MILEAGE_ASC -> compareBy { it.mileageKm ?: Int.MAX_VALUE }
    SortOrder.YEAR_DESC -> compareByDescending { it.year ?: 0 }
}

/** Number of non-default filter facets, for the toolbar badge. */
fun SearchFilter.activeCount(): Int {
    var n = 0
    if (make != null) n++
    if (model != null) n++
    if (minPrice != null) n++
    if (maxPrice != null) n++
    if (minYear != null) n++
    if (maxYear != null) n++
    if (maxMileageKm != null) n++
    if (fuelTypes.isNotEmpty()) n++
    if (transmissions.isNotEmpty()) n++
    if (regions.size != Region.entries.size) n++
    if (sourceIds.isNotEmpty()) n++
    if (sort != SortOrder.NEWEST) n++
    return n
}
