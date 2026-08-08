package com.autka.data.local

import com.autka.core.model.CarOffer
import com.autka.core.model.Currency
import com.autka.core.model.FuelType
import com.autka.core.model.Money
import com.autka.core.model.Region
import com.autka.core.model.Transmission
import org.junit.Assert.assertEquals
import org.junit.Test

class CarOfferEntityTest {

    @Test
    fun `entity round trip preserves persisted offer fields`() {
        val offer = CarOffer(
            id = "source:123",
            sourceId = "source",
            title = "2022 Ford Mustang GT",
            make = "Ford",
            model = "Mustang",
            year = 2022,
            mileageKm = 42_123,
            price = Money(31_500.0, Currency.USD),
            fuelType = FuelType.PETROL,
            transmission = Transmission.AUTOMATIC,
            powerHp = 450,
            location = "Austin, TX",
            region = Region.USA,
            thumbnailUrl = "https://example.test/thumb.jpg",
            imageUrls = listOf(
                "https://example.test/1.jpg",
                "https://example.test/2.jpg",
            ),
            listingUrl = "https://example.test/listing/123",
            postedAtEpochMs = 1_700_000_000_000L,
            listingCount = 3,
            latitude = 30.2672,
            longitude = -97.7431,
        )

        val restored = offer.toEntity(fetchedAt = 1_800_000_000_000L).toModel()

        assertEquals(offer, restored)
    }
}
