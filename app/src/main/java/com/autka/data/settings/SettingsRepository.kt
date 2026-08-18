package com.autka.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.autka.core.model.Currency
import com.autka.core.model.FuelType
import com.autka.core.model.ImportAssumptionPreset
import com.autka.core.model.MAX_IMPORT_PRESET_NAME_LENGTH
import com.autka.core.model.MAX_SAVED_SEARCH_NAME_LENGTH
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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * App-wide user settings and small local preferences persisted via Preferences DataStore,
 * so choices, saved searches and calculator presets survive process death without a Room migration.
 */
interface SettingsRepository {
    val displayCurrency: Flow<Currency>
    val savedSearches: Flow<List<SavedSearch>>
    val importAssumptionPresets: Flow<List<ImportAssumptionPreset>>

    suspend fun setDisplayCurrency(currency: Currency)
    suspend fun saveSearch(name: String, filter: SearchFilter, displayCurrency: Currency)
    suspend fun deleteSavedSearch(id: String)
    suspend fun saveImportAssumptionPreset(
        name: String,
        shippingUsd: Double,
        customsDutyRate: Double,
        vatRate: Double,
    )
    suspend fun deleteImportAssumptionPreset(id: String)
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val displayCurrency: Flow<Currency> =
        dataStore.data.map { prefs ->
            prefs[DISPLAY_CURRENCY]?.let { stored ->
                runCatching { Currency.valueOf(stored) }.getOrNull()
            } ?: DEFAULT_CURRENCY
        }

    override val savedSearches: Flow<List<SavedSearch>> =
        dataStore.data.map { prefs -> decodeSavedSearches(prefs[SAVED_SEARCHES]) }

    override val importAssumptionPresets: Flow<List<ImportAssumptionPreset>> =
        dataStore.data.map { prefs -> decodeImportPresets(prefs[IMPORT_PRESETS]) }

    override suspend fun setDisplayCurrency(currency: Currency) {
        dataStore.edit { prefs -> prefs[DISPLAY_CURRENCY] = currency.name }
    }

    override suspend fun saveSearch(
        name: String,
        filter: SearchFilter,
        displayCurrency: Currency,
    ) {
        val trimmedName = name.trim().take(MAX_SAVED_SEARCH_NAME_LENGTH)
        if (trimmedName.isEmpty() || !filter.hasFinitePriceBounds()) return

        dataStore.edit { prefs ->
            val document = parseSavedSearchDocument(
                prefs[SAVED_SEARCHES],
                recoverMalformed = true,
            ) ?: return@edit
            val decoded = document.items.map { element -> element to decodeSavedSearch(element) }
            val existingId = decoded.firstNotNullOfOrNull { (_, saved) ->
                saved?.takeIf {
                    it.filter == filter && it.displayCurrency == displayCurrency
                }?.id
            }
            val saved = SavedSearch(
                id = existingId ?: UUID.randomUUID().toString(),
                name = trimmedName,
                filter = filter,
                displayCurrency = displayCurrency,
            )
            val updated = buildList {
                add(encodeSavedSearch(saved))
                decoded.forEach { (element, model) ->
                    if (model?.id != saved.id) add(element)
                }
            }
            prefs[SAVED_SEARCHES] = encodeDocument(document, updated)
        }
    }

    override suspend fun deleteSavedSearch(id: String) {
        dataStore.edit { prefs ->
            val document = parseSavedSearchDocument(prefs[SAVED_SEARCHES]) ?: return@edit
            val updated = document.items.filter { element ->
                decodeSavedSearch(element)?.id != id
            }
            if (updated.isEmpty() && document.hasNoMetadata) {
                prefs.remove(SAVED_SEARCHES)
            } else {
                prefs[SAVED_SEARCHES] = encodeDocument(document, updated)
            }
        }
    }

    override suspend fun saveImportAssumptionPreset(
        name: String,
        shippingUsd: Double,
        customsDutyRate: Double,
        vatRate: Double,
    ) {
        val trimmedName = name.trim().take(MAX_IMPORT_PRESET_NAME_LENGTH)
        if (
            trimmedName.isEmpty() || !shippingUsd.isFinite() || shippingUsd < 0.0 ||
            !customsDutyRate.isFinite() || customsDutyRate !in 0.0..1.0 ||
            !vatRate.isFinite() || vatRate !in 0.0..1.0
        ) {
            return
        }

        dataStore.edit { prefs ->
            val current = decodeImportPresets(prefs[IMPORT_PRESETS])
            val existingId = current.firstOrNull {
                it.shippingUsd == shippingUsd &&
                    it.customsDutyRate == customsDutyRate &&
                    it.vatRate == vatRate
            }?.id
            val preset = ImportAssumptionPreset(
                id = existingId ?: UUID.randomUUID().toString(),
                name = trimmedName,
                shippingUsd = shippingUsd,
                customsDutyRate = customsDutyRate,
                vatRate = vatRate,
            )
            val updated = listOf(preset) + current.filterNot { it.id == preset.id }
            prefs[IMPORT_PRESETS] = encodeImportPresets(updated)
        }
    }

    override suspend fun deleteImportAssumptionPreset(id: String) {
        dataStore.edit { prefs ->
            val updated = decodeImportPresets(prefs[IMPORT_PRESETS]).filterNot { it.id == id }
            if (updated.isEmpty()) {
                prefs.remove(IMPORT_PRESETS)
            } else {
                prefs[IMPORT_PRESETS] = encodeImportPresets(updated)
            }
        }
    }

    private fun decodeSavedSearches(raw: String?): List<SavedSearch> =
        parseSavedSearchDocument(raw)
            ?.items
            .orEmpty()
            .mapNotNull(::decodeSavedSearch)
            .distinctBy { it.id }

    private fun parseSavedSearchDocument(
        raw: String?,
        recoverMalformed: Boolean = false,
    ): SavedSearchDocument? {
        if (raw.isNullOrBlank()) return emptySavedSearchDocument()

        val parsed = runCatching { SAVED_SEARCH_JSON.parseToJsonElement(raw) }
            .getOrElse {
                return if (recoverMalformed) emptySavedSearchDocument() else null
            }
        val envelope = parsed as? JsonObject ?: return null
        val items = envelope[ITEMS_KEY] as? JsonArray ?: return null
        return SavedSearchDocument(envelope, items.toList())
    }

    private fun emptySavedSearchDocument() =
        SavedSearchDocument(JsonObject(emptyMap()), emptyList())

    private fun decodeSavedSearch(element: JsonElement): SavedSearch? {
        val payloadObject = element as? JsonObject ?: return null
        if (!SavedSearchPayload.knownKeys.containsAll(payloadObject.keys)) return null

        return runCatching {
            SAVED_SEARCH_JSON.decodeFromJsonElement(SavedSearchPayload.serializer(), payloadObject)
        }.getOrNull()?.toModelOrNull()
    }

    private fun encodeSavedSearch(saved: SavedSearch): JsonElement =
        SAVED_SEARCH_JSON.encodeToJsonElement(
            SavedSearchPayload.serializer(),
            SavedSearchPayload.fromModel(saved),
        )

    private fun encodeDocument(
        document: SavedSearchDocument,
        items: List<JsonElement>,
    ): String = JsonObject(
        document.envelope + (ITEMS_KEY to JsonArray(items)),
    ).toString()

    private fun decodeImportPresets(raw: String?): List<ImportAssumptionPreset> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            IMPORT_PRESET_JSON.decodeFromString(
                ListSerializer(ImportAssumptionPresetPayload.serializer()),
                raw,
            )
        }.getOrDefault(emptyList())
            .mapNotNull(ImportAssumptionPresetPayload::toModelOrNull)
            .distinctBy { it.id }
    }

    private fun encodeImportPresets(presets: List<ImportAssumptionPreset>): String =
        IMPORT_PRESET_JSON.encodeToString(
            ListSerializer(ImportAssumptionPresetPayload.serializer()),
            presets.map(ImportAssumptionPresetPayload::fromModel),
        )

    private companion object {
        const val ITEMS_KEY = "items"
        val DISPLAY_CURRENCY = stringPreferencesKey("display_currency")
        val SAVED_SEARCHES = stringPreferencesKey("saved_searches_v1")
        val IMPORT_PRESETS = stringPreferencesKey("import_assumption_presets_v1")
        val DEFAULT_CURRENCY = Currency.PLN
        val SAVED_SEARCH_JSON = Json {
            ignoreUnknownKeys = false
            coerceInputValues = false
            encodeDefaults = false
        }
        val IMPORT_PRESET_JSON = Json {
            ignoreUnknownKeys = true
            coerceInputValues = false
            encodeDefaults = false
        }
    }
}

private data class SavedSearchDocument(
    val envelope: JsonObject,
    val items: List<JsonElement>,
) {
    val hasNoMetadata: Boolean
        get() = envelope.keys.all { it == "items" }
}

@Serializable
private data class ImportAssumptionPresetPayload(
    val id: String,
    val name: String,
    val shippingUsd: Double,
    val customsDutyRate: Double,
    val vatRate: Double,
) {
    fun toModelOrNull(): ImportAssumptionPreset? {
        if (
            id.isBlank() || name.isBlank() || !shippingUsd.isFinite() || shippingUsd < 0.0 ||
            !customsDutyRate.isFinite() || customsDutyRate !in 0.0..1.0 ||
            !vatRate.isFinite() || vatRate !in 0.0..1.0
        ) {
            return null
        }
        return ImportAssumptionPreset(
            id = id,
            name = name.take(MAX_IMPORT_PRESET_NAME_LENGTH),
            shippingUsd = shippingUsd,
            customsDutyRate = customsDutyRate,
            vatRate = vatRate,
        )
    }

    companion object {
        fun fromModel(preset: ImportAssumptionPreset) = ImportAssumptionPresetPayload(
            id = preset.id,
            name = preset.name,
            shippingUsd = preset.shippingUsd,
            customsDutyRate = preset.customsDutyRate,
            vatRate = preset.vatRate,
        )
    }
}

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
    val displayCurrency: String = Currency.PLN.name,
) {
    fun toModelOrNull(): SavedSearch? {
        if (id.isBlank() || name.isBlank()) return null

        val decodedFuelTypes = decodeEnumSetOrNull<FuelType>(fuelTypes) ?: return null
        val decodedTransmissions = decodeEnumSetOrNull<Transmission>(transmissions) ?: return null
        val decodedRegions = decodeEnumSetOrNull<Region>(regions) ?: return null
        val decodedSort = enumOrNull<SortOrder>(sort) ?: return null
        val decodedCurrency = enumOrNull<Currency>(displayCurrency) ?: return null

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
                fuelTypes = decodedFuelTypes,
                transmissions = decodedTransmissions,
                regions = decodedRegions.ifEmpty { Region.entries.toSet() },
                sourceIds = sourceIds.toSet(),
                sort = decodedSort,
            ),
            displayCurrency = decodedCurrency,
        )
    }

    companion object {
        val knownKeys: Set<String> by lazy {
            val descriptor = serializer().descriptor
            buildSet {
                repeat(descriptor.elementsCount) { index -> add(descriptor.getElementName(index)) }
            }
        }

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
                regions = regions
                    .takeUnless { it == Region.entries.toSet() }
                    ?.sortedBy { it.ordinal }
                    ?.map { it.name }
                    .orEmpty(),
                sourceIds = sourceIds.sorted(),
                sort = sort.name,
                displayCurrency = saved.displayCurrency.name,
            )
        }
    }
}

private fun SearchFilter.hasFinitePriceBounds(): Boolean {
    val min = minPrice
    val max = maxPrice
    return (min == null || min.isFinite()) && (max == null || max.isFinite())
}

private inline fun <reified T : Enum<T>> decodeEnumSetOrNull(names: List<String>): Set<T>? {
    val decoded = names.map { enumOrNull<T>(it) }
    return decoded.takeIf { values -> values.none { it == null } }
        ?.filterNotNull()
        ?.toSet()
}

private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }
