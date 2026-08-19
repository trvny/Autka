package com.autka.data.remote.backend

import com.autka.core.model.CarOffer
import com.autka.core.model.Currency
import com.autka.core.model.FuelType
import com.autka.core.model.Money
import com.autka.core.model.Region
import com.autka.core.model.Transmission
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OffersResponse(
    val offers: List<OfferDto>,
    /** Total matching rows/groups in this response or before pagination. */
    val count: Int,
)

@Serializable
data class OfferDto(
    val id: String,
    val sourceId: String,
    val title: String,
    val make: String,
    val model: String,
    val year: Int? = null,
    val mileageKm: Int? = null,
    val price: MoneyDto,
    val fuelType: String,
    val transmission: String,
    val powerHp: Int? = null,
    val location: String? = null,
    val region: String,
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val listingUrl: String,
    val postedAtEpochMs: Long? = null,
    val listingCount: Int? = null,
    val otherSources: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class MoneyDto(
    val amount: Double,
    val currency: String,
)

object OfferDecoder {
    private val json = Json { ignoreUnknownKeys = true }

    fun decodeJson(payload: String): List<CarOffer> =
        decode(json.decodeFromString<OffersResponse>(payload))

    fun decodeJsonOrNull(payload: String): List<CarOffer>? =
        runCatching { decodeJson(payload) }.getOrNull()

    fun decode(response: OffersResponse): List<CarOffer> =
        response.offers.map(OfferDto::toModel)
}

fun OfferDto.toModel(): CarOffer = CarOffer(
    id = id,
    sourceId = sourceId,
    title = title,
    make = make,
    model = model,
    year = year,
    mileageKm = mileageKm,
    price = Money(
        price.amount,
        runCatching { Currency.valueOf(price.currency) }.getOrDefault(Currency.PLN),
    ),
    fuelType = runCatching { FuelType.valueOf(fuelType) }.getOrDefault(FuelType.UNKNOWN),
    transmission = runCatching { Transmission.valueOf(transmission) }.getOrDefault(Transmission.UNKNOWN),
    powerHp = powerHp,
    location = location,
    region = runCatching { Region.valueOf(region) }.getOrDefault(Region.EUROPE),
    thumbnailUrl = thumbnailUrl,
    imageUrls = imageUrls,
    listingUrl = listingUrl,
    postedAtEpochMs = postedAtEpochMs,
    importEstimate = null,
    listingCount = listingCount,
    latitude = latitude,
    longitude = longitude,
)
