package com.autka.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.autka.core.model.Currency
import com.autka.core.model.FuelType
import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import com.autka.core.model.SortOrder
import com.autka.core.model.Transmission
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `saved search round trips the complete filter and currency`() = runTest {
        val repository = repository(backgroundScope)
        val filter = SearchFilter(
            query = "BMW X5",
            make = "BMW",
            model = "X5",
            minPrice = 90_000.0,
            maxPrice = 180_000.0,
            minYear = 2020,
            maxYear = 2024,
            maxMileageKm = 80_000,
            fuelTypes = setOf(FuelType.PETROL, FuelType.HYBRID),
            transmissions = setOf(Transmission.AUTOMATIC),
            regions = setOf(Region.POLAND, Region.EUROPE),
            sourceIds = setOf("otomoto", "autoplac"),
            sort = SortOrder.PRICE_ASC,
        )

        repository.saveSearch("Family SUV", filter, Currency.PLN)

        val saved = repository.savedSearches.first().single()
        assertEquals("Family SUV", saved.name)
        assertEquals(filter, saved.filter)
        assertEquals(Currency.PLN, saved.displayCurrency)
    }

    @Test
    fun `saving identical filter and currency renames instead of duplicating it`() = runTest {
        val repository = repository(backgroundScope)
        val filter = SearchFilter(query = "MX-5", regions = setOf(Region.EUROPE))

        repository.saveSearch("Roadster", filter, Currency.EUR)
        val original = repository.savedSearches.first().single()
        repository.saveSearch("Weekend", filter, Currency.EUR)

        val saved = repository.savedSearches.first().single()
        assertEquals(original.id, saved.id)
        assertEquals("Weekend", saved.name)
        assertEquals(filter, saved.filter)
        assertEquals(Currency.EUR, saved.displayCurrency)
    }

    @Test
    fun `identical numeric filter in another currency remains a distinct search`() = runTest {
        val repository = repository(backgroundScope)
        val filter = SearchFilter(minPrice = 50_000.0, maxPrice = 100_000.0)

        repository.saveSearch("PL prices", filter, Currency.PLN)
        repository.saveSearch("EU prices", filter, Currency.EUR)

        val saved = repository.savedSearches.first()
        assertEquals(2, saved.size)
        assertEquals(setOf(Currency.PLN, Currency.EUR), saved.map { it.displayCurrency }.toSet())
    }

    @Test
    fun `saved search can be deleted independently`() = runTest {
        val repository = repository(backgroundScope)
        repository.saveSearch("BMW", SearchFilter(query = "BMW"), Currency.PLN)
        repository.saveSearch("Audi", SearchFilter(query = "Audi"), Currency.PLN)
        val bmw = repository.savedSearches.first().single { it.name == "BMW" }

        repository.deleteSavedSearch(bmw.id)

        assertEquals(listOf("Audi"), repository.savedSearches.first().map { it.name })
    }

    @Test
    fun `malformed saved search payload fails closed`() = runTest {
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("saved_searches_v1")] = "{not-json"
        }
        val repository = DataStoreSettingsRepository(dataStore, json())

        assertTrue(repository.savedSearches.first().isEmpty())
    }

    private fun repository(scope: CoroutineScope) =
        DataStoreSettingsRepository(dataStore(scope), json())

    private fun dataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = ::newPreferencesFile,
    )

    private fun newPreferencesFile(): File =
        File(temporaryFolder.root, "${UUID.randomUUID()}.preferences_pb")

    private fun json() = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}
