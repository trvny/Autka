package com.autka.data.remote.backend

import com.autka.core.model.Currency
import com.autka.core.model.FuelType
import com.autka.core.model.Region
import com.autka.core.model.Transmission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OfferDecoderTest {
    @Test
    fun `decodeJson maps normalized backend offer`() {
        val offer = OfferDecoder.decodeJson(
            """
            {
              "offers": [{
                "id": "otomoto:1",
                "sourceId": "otomoto",
                "title": "Test car",
                "make": "Test",
                "model": "One",
                "year": 2024,
                "mileageKm": 12345,
                "price": {"amount": 99999, "currency": "PLN"},
                "fuelType": "PETROL",
                "transmission": "AUTOMATIC",
                "powerHp": 150,
                "location": "Krakow, PL",
                "region": "POLAND",
                "thumbnailUrl": "https://example.test/car.jpg",
                "imageUrls": ["https://example.test/car.jpg"],
                "listingUrl": "https://example.test/1",
                "postedAtEpochMs": 123456789,
                "listingCount": 2,
                "latitude": 50.06,
                "longitude": 19.94,
                "futureField": "ignored"
              }],
              "count": 1
            }
            """.trimIndent(),
        ).single()

        assertEquals("otomoto:1", offer.id)
        assertEquals(Currency.PLN, offer.price.currency)
        assertEquals(FuelType.PETROL, offer.fuelType)
        assertEquals(Transmission.AUTOMATIC, offer.transmission)
        assertEquals(Region.POLAND, offer.region)
        assertEquals(2, offer.listingCount)
        assertEquals(50.06, offer.latitude)
        assertNull(offer.importEstimate)
    }

    @Test
    fun `unknown enum values keep Android fallbacks`() {
        val offer = OfferDecoder.decode(
            OffersResponse(
                offers = listOf(
                    OfferDto(
                        id = "x",
                        sourceId = "x",
                        title = "x",
                        make = "x",
                        model = "x",
                        price = MoneyDto(1.0, "NOPE"),
                        fuelType = "NOPE",
                        transmission = "NOPE",
                        region = "NOPE",
                        listingUrl = "https://example.test",
                    ),
                ),
                count = 1,
            ),
        ).single()

        assertEquals(Currency.PLN, offer.price.currency)
        assertEquals(FuelType.UNKNOWN, offer.fuelType)
        assertEquals(Transmission.UNKNOWN, offer.transmission)
        assertEquals(Region.EUROPE, offer.region)
    }

    @Test
    fun `safe decoder distinguishes malformed payload from empty offers`() {
        assertNotNull(OfferDecoder.decodeJsonOrNull("{\"offers\":[],\"count\":0}"))
        assertNull(OfferDecoder.decodeJsonOrNull("not json"))
    }
}
