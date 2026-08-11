package com.autka.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.autka.core.model.Currency
import com.autka.core.model.FuelType
import com.autka.core.model.Region
import com.autka.core.model.SavedSearch
import com.autka.core.model.SearchFilter
import com.autka.core.model.SortOrder
import com.autka.core.model.Transmission
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * App-wide user settings and small local preferences persisted via Preferences DataStore,
 * so choices and saved searches survive process death without requiring a Room migration.
 */
interface SettingsRepository {
    val displayCurrency: Flow<Currency>
    val savedSearches: Flow<List<SavedSearch>>

    suspend fun setDisplayCurrency(currency: Currency)
    suspend fun saveSearch(name: String, filter: SearchFilter)
    suspend fun deleteSavedSearch(id: String)
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : SettingsRepository {

    override val displayCurrency: Flow<Currency> =
        dataStore.data.map { prefs ->
            prefs[DISPLAY_CURRENCY]?.let { stored ->
                runCatching { Currency.valueOf(stored) }.getOrNull()
            } ?: DEFAULT_CURRENCY
        }

    override val savedSearches: Flow<List<SavedSearch>> =
        dataStore.data.map { prefs -> decodeSavedSearches(prefs[SAVED_SEARCHES]) }

    override suspend fun setDisplayCurrency(currency: Currency) {
        dataStore.edit { prefs -> prefs[DISPLAY_CURRENCY] = currency.name }
    }

    override suspend fun saveSearch(name: String, filter: SearchFilter) {
        val trimmedName = name.trim().take(MAX_SAVED_SEARCH_NAME_LENGTH)
        if (trimmedName.isEmpty()) return

        dataStore.edit { prefs ->
            val current = decodeSavedSearches(prefs[SAVED_SEARCHES])
            val existingId = current.firstOrNull { it.filter == filter }?.id
            val saved = SavedSearch(
                id = existingId ?: UUID.randomUUID().toString(),
                name = trimmedName,
                filter = filter,
            )
            val updated = buildList {
                add(saved)
                current.asSequence()
                    .filterNot { it.id == saved.id }
                    .take(MAX_SAVED_SEARCHES - 1)
                    .forEach(::add)
            }
            prefs[SAVED_SEARCHES] = encodeSavedSearches(updated)
        }
    }

    override suspend fun deleteSavedSearch(id: String) {
        dataStore.edit { prefs ->
            val updated = decodeSavedSearches(prefs[SAVED_SEARCHES])
                .filterNot { it.id == id }
            if (updated.isEmpty()) {
                prefs.remove(SAVED_SEARCHES)
            } else {
                prefs[SAVED_SEARCHES] = encodeSavedSearches(updated)
            }
        }
    }

    private fun decodeSavedSearches(raw: String?): List<SavedSearch> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<SavedSearchStore>(raw).items.mapNotNull(SavedSearchPayload::toModelOrNull)
        }.getOrDefault(emptyList())
    }

    private fun encodeSavedSearches(searches: List<SavedSearch>): String =
        json.encodeToString(
            SavedSearchStore(
                items = searches.map(SavedSearchPayload::fromModel),
            ),
        )

    private companion object {
        val DISPLAY_CURRENCY = stringPreferencesKey("display_currency")
        val SAVED_SEARCHES = stringPreferencesKey("saved_searches_v1")
        val DEFAULT_CURRENCY = Currency.PLN
        const val MAX_SAVED_SEARCHES = 20
        const val MAX_SAVED_SEARCH_NAME_LENGTH = 80
    }
}

@Serializable
private data class SavedSearchStore(
    val items: List<SavedSearchPayload> = emptyList(),
)

@Serializable
private data class SavedSearchPayload(
    val id: String,
    val name: String,
    val query: String = "",
    val make: String? = null,
    val model: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val maxMileageKm: Int? = null,
    val fuelTypes: List<String> = emptyList(),
    val transmissions: List<String> = emptyList(),
    val regions: List<String> = emptyList(),
    val sourceIds: List<String> = emptyList(),
    val sort: String = SortOrder.NEWEST.name,
) {
    fun toModelOrNull(): SavedSearch? {
        if (id.isBlank() || name.isBlank()) return null

        val decodedRegions = regions.mapNotNull { enumOrNull<Region>(it) }.toSet()
        return SavedSearch(
            id = id,
            name = name,
            filter = SearchFilter(
                query = query,
                make = make,
                model = model,
                minPrice = minPrice,
                maxPrice = maxPrice,
                minYear = minYear,
                maxYear = maxYear,
                maxMileageKm = maxMileageKm,
                fuelTypes = fuelTypes.mapNotNull { enumOrNull<FuelType>(it) }.toSet(),
                transmissions = transmissions.mapNotNull { enumOrNull<Transmission>(it) }.toSet(),
                regions = decodedRegions.ifEmpty { Region.entries.toSet() },
                sourceIds = sourceIds.toSet(),
                sort = enumOrNull<SortOrder>(sort) ?: SortOrder.NEWEST,
            ),
        )
    }

    companion object {
        fun fromModel(saved: SavedSearch): SavedSearchPayload = with(saved.filter) {
            SavedSearchPayload(
                id = saved.id,
                name = saved.name,
                query = query,
                make = make,
                model = model,
                minPrice = minPrice,
                maxPrice = maxPrice,
                minYear = minYear,
                maxYear = maxYear,
                maxMileageKm = maxMileageKm,
                fuelTypes = fuelTypes.sortedBy { it.ordinal }.map { it.name },
                transmissions = transmissions.sortedBy { it.ordinal }.map { it.name },
                regions = regions.sortedBy { it.ordinal }.map { it.name },
                sourceIds = sourceIds.sorted(),
                sort = sort.name,
            )
        }
    }
}

private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }
