package com.autka.feature.external

import com.autka.core.model.FuelType
import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import com.autka.core.model.SortOrder
import com.autka.core.model.Transmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceSearchLinksTest {

    @Test
    fun `otomoto keeps verified path and filter parameter contract`() {
        val url = urlFor(
            "otomoto",
            SearchFilter(
                make = "BMW",
                model = "X5",
                minPrice = 50_000.0,
                maxPrice = 120_000.0,
                minYear = 2019,
                maxYear = 2023,
                maxMileageKm = 90_000,
                fuelTypes = setOf(FuelType.ELECTRIC, FuelType.DIESEL),
                transmissions = setOf(Transmission.AUTOMATIC, Transmission.MANUAL),
                regions = setOf(Region.POLAND),
                sort = SortOrder.PRICE_ASC,
            ),
        )

        assertEquals(
            "https://www.otomoto.pl/osobowe/bmw/x5" +
                "?search%5Bfilter_float_price%3Afrom%5D=50000" +
                "&search%5Bfilter_float_price%3Ato%5D=120000" +
                "&search%5Bfilter_float_year%3Afrom%5D=2019" +
                "&search%5Bfilter_float_year%3Ato%5D=2023" +
                "&search%5Bfilter_float_mileage%3Ato%5D=90000" +
                "&search%5Bfilter_enum_fuel_type%5D%5B0%5D=diesel" +
                "&search%5Bfilter_enum_fuel_type%5D%5B1%5D=electric" +
                "&search%5Bfilter_enum_gearbox%5D%5B0%5D=manual" +
                "&search%5Bfilter_enum_gearbox%5D%5B1%5D=automatic" +
                "&search%5Border%5D=filter_float_price%3Aasc",
            url,
        )
    }

    @Test
    fun `cars com keeps verified make model price and year parameters`() {
        val url = urlFor(
            "carscom",
            SearchFilter(
                make = "Nissan",
                model = "Rogue",
                minPrice = 10_000.0,
                maxPrice = 30_000.0,
                minYear = 2020,
                maxYear = 2024,
                regions = setOf(Region.USA),
            ),
        )

        assertEquals(
            "https://www.cars.com/shopping/results/" +
                "?makes%5B%5D=nissan" +
                "&models%5B%5D=nissan-rogue" +
                "&list_price_min=10000" +
                "&list_price_max=30000" +
                "&year_min=2020" +
                "&year_max=2024" +
                "&stock_type=used",
            url,
        )
    }

    @Test
    fun `autotrader US converts kilometre mileage cap to miles`() {
        val url = urlFor(
            "autotrader",
            SearchFilter(
                make = "Ford",
                model = "Mustang",
                maxPrice = 40_000.0,
                maxMileageKm = 80_000,
                regions = setOf(Region.USA),
            ),
        )

        assertEquals(
            "https://www.autotrader.com/cars-for-sale/all-cars/cars-under-40000/ford/mustang?mileage=49709",
            url,
        )
    }

    @Test
    fun `autouncle keeps verified LPG path slug`() {
        val url = urlFor(
            "autouncle",
            SearchFilter(
                maxPrice = 10_000.0,
                fuelTypes = setOf(FuelType.LPG),
                regions = setOf(Region.POLAND),
            ),
        )

        assertEquals(
            "https://www.autouncle.pl/pl/samochody-uzywane/f-lpg/mp-do-10000-pln",
            url,
        )
    }

    @Test
    fun `USA filter exposes US providers and hides Poland-only providers`() {
        val ids = MarketplaceSearchLinks.all(SearchFilter(regions = setOf(Region.USA)))
            .map { it.sourceId }
            .toSet()

        assertTrue("carscom" in ids)
        assertTrue("autotrader" in ids)
        assertTrue("copart" in ids)
        assertTrue("iaai" in ids)
        assertFalse("otomoto" in ids)
        assertFalse("olx" in ids)
        assertFalse("autoplac" in ids)
    }

    @Test
    fun `default regions expose every provider exactly once`() {
        val links = MarketplaceSearchLinks.all(SearchFilter())
        assertEquals(links.size, links.map { it.sourceId }.toSet().size)
        assertEquals(12, links.size)
    }

    @Test
    fun `mixed Poland and USA regions combine providers without duplicates`() {
        val links = MarketplaceSearchLinks.all(
            SearchFilter(regions = setOf(Region.POLAND, Region.USA)),
        )
        val ids = links.map { it.sourceId }

        assertEquals(ids.size, ids.toSet().size)
        assertTrue("otomoto" in ids)
        assertTrue("carscom" in ids)
        assertFalse("mobilede" in ids)
    }

    private fun urlFor(sourceId: String, filter: SearchFilter): String =
        MarketplaceSearchLinks.all(filter)
            .single { it.sourceId == sourceId }
            .url
}
