package com.autka.feature.external

import com.autka.core.model.FuelType
import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import com.autka.core.model.SortOrder
import com.autka.core.model.Transmission

/** A compliant hand-off to a marketplace's own search page, never an ingested offer. */
data class MarketplaceLink(val sourceId: String, val displayName: String, val url: String)

private data class MarketplaceProvider(
    val sourceId: String,
    val displayName: String,
    val regions: Set<Region>,
    val build: (SearchFilter, String?) -> String,
)

/** Builds external marketplace search URLs from the shared [SearchFilter]. */
object MarketplaceSearchLinks {
    private val providers = listOf(
        MarketplaceProvider("otomoto", "Otomoto", setOf(Region.POLAND), ::otomoto),
        MarketplaceProvider("olx", "OLX", setOf(Region.POLAND), { f, _ -> olx(f) }),
        MarketplaceProvider("facebook", "Facebook", setOf(Region.POLAND, Region.EUROPE), { f, _ -> facebook(f) }),
        MarketplaceProvider("autoplac", "Autoplac", setOf(Region.POLAND), { f, _ -> autoplac(f) }),
        MarketplaceProvider("autotraderpl", "AutoTrader.pl", setOf(Region.POLAND), { f, _ -> autoTraderPl(f) }),
        MarketplaceProvider("autouncle", "AutoUncle", setOf(Region.POLAND, Region.EUROPE), { f, _ -> autoUncle(f) }),
        MarketplaceProvider("autoscout24", "AutoScout24", setOf(Region.EUROPE, Region.POLAND), { f, _ -> autoScout24(f) }),
        MarketplaceProvider("mobilede", "mobile.de", setOf(Region.EUROPE), { f, _ -> mobileDe(f) }),
        MarketplaceProvider("copart", "Copart (US)", setOf(Region.USA), { f, _ -> copart(f) }),
        MarketplaceProvider("iaai", "IAAI (US)", setOf(Region.USA), { f, _ -> iaai(f) }),
        MarketplaceProvider("carscom", "Cars.com (US)", setOf(Region.USA), { f, _ -> carsCom(f) }),
        MarketplaceProvider("autotrader", "AutoTrader (US)", setOf(Region.USA), { f, _ -> autoTrader(f) }),
    )

    fun all(filter: SearchFilter, affiliateId: String? = null): List<MarketplaceLink> =
        providers
            .filter { provider -> provider.regions.any { it in filter.regions } }
            .map { MarketplaceLink(it.sourceId, it.displayName, it.build(filter, affiliateId)) }

    private fun otomoto(f: SearchFilter, affiliateId: String?): String {
        val path = buildString {
            append("https://www.otomoto.pl/osobowe")
            f.make?.let { append("/").append(slug(it)) }
            if (f.make != null) f.model?.let { append("/").append(slug(it)) }
        }
        val q = Params()
        f.minPrice?.let { q["search[filter_float_price:from]"] = it.toLong().toString() }
        f.maxPrice?.let { q["search[filter_float_price:to]"] = it.toLong().toString() }
        f.minYear?.let { q["search[filter_float_year:from]"] = it.toString() }
        f.maxYear?.let { q["search[filter_float_year:to]"] = it.toString() }
        f.maxMileageKm?.let { q["search[filter_float_mileage:to]"] = it.toString() }
        f.fuelTypes.sortedBy { it.ordinal }.mapNotNull(::otomotoFuel).forEachIndexed { i, fuel ->
            q["search[filter_enum_fuel_type][$i]"] = fuel
        }
        f.transmissions.sortedBy { it.ordinal }.mapNotNull(::naspersTransmission).forEachIndexed { i, gearbox ->
            q["search[filter_enum_gearbox][$i]"] = gearbox
        }
        otomotoOrder(f.sort)?.let { q["search[order]"] = it }
        if (f.make == null) f.query.takeIf { it.isNotBlank() }?.let { q["search[filter_enum_make]"] = slug(it) }
        affiliateId?.let { q["utm_source"] = it } // TODO(verify): real affiliate parameter after programme access.
        return path + q.render()
    }

    private fun otomotoFuel(t: FuelType?): String? = when (t) {
        FuelType.PETROL -> "petrol"
        FuelType.DIESEL -> "diesel"
        FuelType.LPG -> "petrol-lpg"
        FuelType.HYBRID -> "hybrid"
        FuelType.PLUGIN_HYBRID -> "plugin-hybrid"
        FuelType.ELECTRIC -> "electric"
        else -> null
    }

    private fun otomotoOrder(s: SortOrder): String? = when (s) {
        SortOrder.NEWEST -> "created_at_first:desc"
        SortOrder.PRICE_ASC -> "filter_float_price:asc"
        SortOrder.PRICE_DESC -> "filter_float_price:desc"
        SortOrder.MILEAGE_ASC -> "filter_float_mileage:asc"
        SortOrder.YEAR_DESC -> "filter_float_year:desc"
    }

    private fun naspersTransmission(t: Transmission?): String? = when (t) {
        Transmission.AUTOMATIC -> "automatic"
        Transmission.MANUAL -> "manual"
        else -> null
    }

    private fun olx(f: SearchFilter): String {
        val path = buildString {
            append("https://www.olx.pl/motoryzacja/samochody/")
            terms(f).takeIf { it.isNotEmpty() }?.let { append("q-").append(slug(it)).append("/") }
        }
        val q = Params()
        f.minPrice?.let { q["search[filter_float_price:from]"] = it.toLong().toString() }
        f.maxPrice?.let { q["search[filter_float_price:to]"] = it.toLong().toString() }
        f.minYear?.let { q["search[filter_float_year:from]"] = it.toString() }
        f.maxYear?.let { q["search[filter_float_year:to]"] = it.toString() }
        f.maxMileageKm?.let { q["search[filter_float_milage:to]"] = it.toString() }
        f.fuelTypes.mapNotNull(::otomotoFuel).forEachIndexed { i, fuel ->
            q["search[filter_enum_petrol][$i]"] = fuel
        }
        f.transmissions.mapNotNull(::naspersTransmission).forEachIndexed { i, transmission ->
            q["search[filter_enum_transmission][$i]"] = transmission
        }
        otomotoOrder(f.sort)?.let { q["search[order]"] = it }
        return path + q.render()
    }

    private fun facebook(f: SearchFilter): String {
        val q = Params()
        terms(f).takeIf { it.isNotEmpty() }?.let { q["query"] = it }
        f.minPrice?.let { q["minPrice"] = it.toLong().toString() }
        f.maxPrice?.let { q["maxPrice"] = it.toLong().toString() }
        return "https://www.facebook.com/marketplace/category/vehicles" + q.render()
    }

    private fun autoUncle(f: SearchFilter): String {
        val path = buildString {
            append("https://www.autouncle.pl/pl/samochody-uzywane")
            f.fuelTypes.firstNotNullOfOrNull(::autoUncleFuel)?.let { append("/f-").append(it) }
            f.maxPrice?.let { append("/mp-do-").append(it.toLong()).append("-pln") }
        }
        val q = Params()
        f.minPrice?.let { q["s[min_price]"] = it.toLong().toString() }
        f.maxMileageKm?.let { q["s[max_km]"] = it.toString() }
        return path + q.render()
    }

    private fun autoUncleFuel(t: FuelType?): String? = when (t) {
        FuelType.PETROL -> "benzyna"
        FuelType.DIESEL -> "diesel"
        FuelType.HYBRID -> "hybryda"
        FuelType.ELECTRIC -> "elektryczny"
        FuelType.LPG -> "lpg"
        else -> null // TODO(verify): plug-in hybrid.
    }

    private fun autoScout24(f: SearchFilter): String {
        val path = buildString {
            append("https://www.autoscout24.pl/lst")
            f.make?.let { append("/").append(slug(it)) }
            if (f.make != null) f.model?.let { append("/").append(slug(it)) }
            if (f.model == null) {
                f.transmissions.firstNotNullOfOrNull(::autoScoutTransmission)?.let { append("/").append(it) }
            }
        }
        val q = Params()
        q["atype"] = "C"
        q["ustate"] = "N,U"
        q["damaged_listing"] = "exclude"
        q["cy"] = "D,A,I,B,NL,E,L,F"
        f.minPrice?.let { q["pricefrom"] = it.toLong().toString() }
        f.maxPrice?.let { q["priceto"] = it.toLong().toString() }
        f.minYear?.let { q["fregfrom"] = it.toString() }
        f.maxYear?.let { q["fregto"] = it.toString() }
        f.maxMileageKm?.let { q["kmto"] = it.toString() }
        f.fuelTypes.mapNotNull(::autoScoutFuel).distinct().takeIf { it.isNotEmpty() }
            ?.let { q["fuel"] = it.joinToString(",") }
        q["sort"] = autoScoutSort(f.sort)
        q["desc"] = if (f.sort == SortOrder.PRICE_DESC || f.sort == SortOrder.YEAR_DESC) "1" else "0"
        if (f.make == null) terms(f).takeIf { it.isNotEmpty() }?.let { q["kwd"] = it }
        return path + q.render()
    }

    private fun autoScoutFuel(t: FuelType?): String? = when (t) {
        FuelType.PETROL -> "B"
        FuelType.DIESEL -> "D"
        FuelType.ELECTRIC -> "E"
        FuelType.LPG -> "L"
        FuelType.HYBRID, FuelType.PLUGIN_HYBRID -> "2"
        else -> null
    }

    private fun autoScoutTransmission(t: Transmission?): String? = when (t) {
        Transmission.AUTOMATIC -> "tr_automatyczna"
        Transmission.MANUAL -> "tr_manualna" // TODO(verify): parity-only value.
        else -> null
    }

    private fun autoScoutSort(s: SortOrder): String = when (s) {
        SortOrder.NEWEST -> "age"
        SortOrder.PRICE_ASC, SortOrder.PRICE_DESC -> "price"
        SortOrder.MILEAGE_ASC -> "mileage"
        SortOrder.YEAR_DESC -> "year"
    }

    private fun mobileDe(f: SearchFilter): String {
        val q = Params()
        q["isSearchRequest"] = "true"
        q["s"] = "Car"
        q["vc"] = "Car"
        q["dam"] = "false"
        range(f.minPrice?.toLong(), f.maxPrice?.toLong())?.let { q["p"] = it }
        range(f.minYear?.toLong(), f.maxYear?.toLong())?.let { q["fr"] = it }
        f.maxMileageKm?.let { q["ml"] = ":$it" }
        f.transmissions.firstNotNullOfOrNull(::mobileDeTransmission)?.let { q["tr"] = it }
        return "https://suchen.mobile.de/fahrzeuge/search.html" + q.render()
    }

    private fun mobileDeTransmission(t: Transmission?): String? = when (t) {
        Transmission.AUTOMATIC -> "AUTOMATIC_GEAR"
        Transmission.MANUAL -> "MANUAL_GEAR" // TODO(verify): parity-only value.
        else -> null
    }

    private fun autoplac(f: SearchFilter): String {
        val path = buildString {
            append("https://autoplac.pl/oferty/samochody-osobowe")
            f.maxPrice?.let { append("/cena-do-").append(it.toLong() / 1000).append("-tysiecy") }
        }
        val q = Params()
        f.fuelTypes.mapNotNull(::autoplacFuel).takeIf { it.isNotEmpty() }
            ?.let { q["fuelTypes"] = it.joinToString(",") }
        f.minYear?.let { q["yearFrom"] = it.toString() }
        f.maxMileageKm?.let { q["mileageTo"] = it.toString() }
        return path + q.render()
    }

    private fun autoplacFuel(t: FuelType?): String? = when (t) {
        FuelType.PETROL -> "GASOLINE"
        FuelType.DIESEL -> "DIESEL"
        FuelType.HYBRID -> "HYBRID"
        FuelType.LPG -> "LPG"
        FuelType.ELECTRIC -> "EV"
        FuelType.OTHER -> "OTHER"
        else -> null // TODO(verify): plug-in hybrid.
    }

    private fun autoTraderPl(f: SearchFilter): String {
        val q = Params()
        f.fuelTypes.firstNotNullOfOrNull(::autoTraderPlFuel)?.let { q["rodzaj_paliwa"] = it }
        f.transmissions.mapNotNull(::autoTraderPlTransmission).takeIf { it.isNotEmpty() }
            ?.let { q["skrzynia_biegow"] = it.joinToString(",") }
        f.minPrice?.let { q["cena_od_pln"] = it.toLong().toString() }
        f.maxPrice?.let { q["cena_do_pln"] = it.toLong().toString() }
        f.minYear?.let { q["rok_od"] = it.toString() }
        f.maxYear?.let { q["rok_do"] = it.toString() }
        f.maxMileageKm?.let { q["przebieg_do"] = it.toString() }
        return "https://www.autotrader.pl/szukaj/osobowe" + q.render()
    }

    private fun autoTraderPlTransmission(t: Transmission?): String? = when (t) {
        Transmission.AUTOMATIC -> "automatyczna"
        Transmission.MANUAL -> "manualna"
        else -> null
    }

    private fun autoTraderPlFuel(t: FuelType?): String? = when (t) {
        FuelType.PETROL -> "benzyna"
        FuelType.DIESEL -> "diesel"
        FuelType.ELECTRIC -> "elektryczny"
        FuelType.HYBRID -> "hybrydowy"
        else -> null // TODO(verify): LPG / plug-in hybrid.
    }

    private fun copart(f: SearchFilter): String {
        f.make?.let { make ->
            val q = Params()
            q["displayStr"] = make
            return "https://www.copart.com/pl/vehicle-search-make/${slug(make)}" + q.render()
        }
        val text = terms(f)
        if (text.isEmpty()) return "https://www.copart.com/lotSearchResults"
        val q = Params()
        q["free"] = "true"
        q["query"] = text
        return "https://www.copart.com/lotSearchResults" + q.render()
    }

    private fun iaai(@Suppress("UNUSED_PARAMETER") f: SearchFilter): String =
        "https://www.iaai.com/Search"

    private fun carsCom(f: SearchFilter): String {
        val q = Params()
        f.make?.let { q["makes[]"] = slug(it) }
        f.model?.let { model ->
            q["models[]"] = listOfNotNull(f.make?.let(::slug), slug(model)).joinToString("-")
        }
        f.minPrice?.let { q["list_price_min"] = it.toLong().toString() }
        f.maxPrice?.let { q["list_price_max"] = it.toLong().toString() }
        f.minYear?.let { q["year_min"] = it.toString() }
        f.maxYear?.let { q["year_max"] = it.toString() }
        q["stock_type"] = "used"
        return "https://www.cars.com/shopping/results/" + q.render()
    }

    private fun autoTrader(f: SearchFilter): String {
        val path = buildString {
            append("https://www.autotrader.com/cars-for-sale/all-cars")
            f.maxPrice?.let { append("/cars-under-").append(it.toLong()) }
            f.make?.let { append("/").append(slug(it)) }
            if (f.make != null) f.model?.let { append("/").append(slug(it)) }
        }
        val q = Params()
        f.maxMileageKm?.let { q["mileage"] = kmToMilesFloor(it).toString() }
        return path + q.render()
    }

    private fun terms(f: SearchFilter): String =
        listOfNotNull(f.make, f.model, f.query.ifBlank { null }).joinToString(" ").trim()

    private fun kmToMilesFloor(km: Int): Int = (km / 1.609344).toInt()

    private fun range(min: Long?, max: Long?): String? =
        if (min == null && max == null) null else "${min ?: ""}:${max ?: ""}"

    private fun slug(value: String): String = value.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

    private class Params {
        private val pairs = mutableListOf<Pair<String, String>>()

        operator fun set(key: String, value: String) {
            pairs += key to value
        }

        fun render(): String = if (pairs.isEmpty()) {
            ""
        } else {
            "?" + pairs.joinToString("&") { (key, value) ->
                "${encodeQueryComponent(key)}=${encodeQueryComponent(value)}"
            }
        }
    }
}

/** Java URLEncoder-compatible UTF-8 encoding, with spaces normalized to %20 as before. */
private fun encodeQueryComponent(value: String): String {
    val hex = "0123456789ABCDEF"
    return buildString {
        value.encodeToByteArray().forEach { byte ->
            val b = byte.toInt() and 0xFF
            val safe = b in 'a'.code..'z'.code ||
                b in 'A'.code..'Z'.code ||
                b in '0'.code..'9'.code ||
                b == '-'.code || b == '_'.code || b == '.'.code || b == '*'.code
            if (safe) {
                append(b.toChar())
            } else {
                append('%')
                append(hex[b ushr 4])
                append(hex[b and 0x0F])
            }
        }
    }
}
