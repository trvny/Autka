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
    fun `all-region search uses the expandable sentinel`() = runTest {
        val key = stringPreferencesKey("saved_searches_v1")
        val dataStore = dataStore(backgroundScope)
        val repository = DataStoreSettingsRepository(dataStore)

        repository.saveSearch("All BMW", SearchFilter(query = "BMW"), Currency.PLN)

        assertEquals(
            Region.entries.toSet(),
            repository.savedSearches.first().single().filter.regions,
        )
        assertTrue(!dataStore.data.first()[key].orEmpty().contains("\"regions\""))
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
    fun `saved searches are not silently evicted`() = runTest {
        val repository = repository(backgroundScope)

        repeat(25) { index ->
            repository.saveSearch(
                "Search $index",
                SearchFilter(query = "car-$index"),
                Currency.PLN,
            )
        }

        assertEquals(25, repository.savedSearches.first().size)
    }

    @Test
    fun `non-finite price bounds are not serialized`() = runTest {
        val repository = repository(backgroundScope)

        repository.saveSearch(
            "NaN price",
            SearchFilter(minPrice = Double.NaN),
            Currency.PLN,
        )
        repository.saveSearch(
            "Infinite price",
            SearchFilter(maxPrice = Double.POSITIVE_INFINITY),
            Currency.PLN,
        )

        assertTrue(repository.savedSearches.first().isEmpty())
    }

    @Test
    fun `malformed saved search payload fails closed`() = runTest {
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("saved_searches_v1")] = "{not-json"
        }
        val repository = DataStoreSettingsRepository(dataStore)

        assertTrue(repository.savedSearches.first().isEmpty())
    }

    @Test
    fun `malformed entry does not discard valid siblings`() = runTest {
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("saved_searches_v1")] =
                """{"items":[{"id":"good","name":"Good","query":"BMW"},{"id":"bad","name":"Bad","minPrice":"oops"}]}"""
        }
        val repository = DataStoreSettingsRepository(dataStore)

        val saved = repository.savedSearches.first().single()
        assertEquals("Good", saved.name)
        assertEquals("BMW", saved.filter.query)
    }

    @Test
    fun `explicit null constraints stay opaque instead of widening`() = runTest {
        val key = stringPreferencesKey("saved_searches_v1")
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[key] =
                """{"items":[
                    {"id":"null-regions","name":"Null regions","regions":null},
                    {"id":"null-fuel","name":"Null fuel","fuelTypes":null},
                    {"id":"null-sources","name":"Null sources","sourceIds":null},
                    {"id":"null-sort","name":"Null sort","sort":null},
                    {"id":"null-currency","name":"Null currency","displayCurrency":null}
                ]}""".trimIndent()
        }
        val repository = DataStoreSettingsRepository(dataStore)

        assertTrue(repository.savedSearches.first().isEmpty())
        repository.saveSearch("Audi", SearchFilter(query = "Audi"), Currency.PLN)

        assertEquals(listOf("Audi"), repository.savedSearches.first().map { it.name })
        assertTrue(dataStore.data.first()[key].orEmpty().contains("null-regions"))
    }

    @Test
    fun `opaque entries survive saving another search`() = runTest {
        val key = stringPreferencesKey("saved_searches_v1")
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[key] =
                """{"items":[
                    {"id":"good","name":"Good","query":"BMW"},
                    {"id":"future","name":"Future","fuelTypes":["FUTURE_FUEL"]}
                ]}""".trimIndent()
        }
        val repository = DataStoreSettingsRepository(dataStore)

        repository.saveSearch("Audi", SearchFilter(query = "Audi"), Currency.PLN)

        assertEquals(listOf("Audi", "Good"), repository.savedSearches.first().map { it.name })
        assertTrue(dataStore.data.first()[key].orEmpty().contains("FUTURE_FUEL"))
    }

    @Test
    fun `unknown future fields stay opaque and survive rewrites`() = runTest {
        val key = stringPreferencesKey("saved_searches_v1")
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[key] =
                """{"items":[
                    {"id":"future","name":"Future","query":"BMW","dealerRating":4.8}
                ]}""".trimIndent()
        }
        val repository = DataStoreSettingsRepository(dataStore)

        assertTrue(repository.savedSearches.first().isEmpty())
        repository.saveSearch("Audi", SearchFilter(query = "Audi"), Currency.PLN)

        assertEquals(listOf("Audi"), repository.savedSearches.first().map { it.name })
        assertTrue(dataStore.data.first()[key].orEmpty().contains("dealerRating"))
    }

    @Test
    fun `future envelope metadata survives rewrites`() = runTest {
        val key = stringPreferencesKey("saved_searches_v1")
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[key] =
                """{"schemaVersion":2,"writer":"future","items":[
                    {"id":"good","name":"Good","query":"BMW"}
                ]}""".trimIndent()
        }
        val repository = DataStoreSettingsRepository(dataStore)

        repository.saveSearch("Audi", SearchFilter(query = "Audi"), Currency.PLN)

        assertEquals(listOf("Audi", "Good"), repository.savedSearches.first().map { it.name })
        val persisted = dataStore.data.first()[key].orEmpty()
        assertTrue(persisted.contains("\"schemaVersion\":2"))
        assertTrue(persisted.contains("\"writer\":\"future\""))
    }

    @Test
    fun `unsupported future envelope is preserved without rewriting`() = runTest {
        val key = stringPreferencesKey("saved_searches_v1")
        val original = """{"schemaVersion":3,"records":[{"id":"future"}]}"""
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs -> prefs[key] = original }
        val repository = DataStoreSettingsRepository(dataStore)

        assertTrue(repository.savedSearches.first().isEmpty())
        repository.saveSearch("Audi", SearchFilter(query = "Audi"), Currency.PLN)

        assertTrue(repository.savedSearches.first().isEmpty())
        assertEquals(original, dataStore.data.first()[key])
    }

    @Test
    fun `duplicate stored ids expose only one saved search`() = runTest {
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("saved_searches_v1")] =
                """{"items":[
                    {"id":"same","name":"First","query":"BMW"},
                    {"id":"same","name":"Second","query":"Audi"}
                ]}""".trimIndent()
        }
        val repository = DataStoreSettingsRepository(dataStore)

        val saved = repository.savedSearches.first().single()
        assertEquals("First", saved.name)
    }

    @Test
    fun `unsupported enum values fail closed instead of widening or reinterpreting scope`() = runTest {
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("saved_searches_v1")] =
                """{"items":[
                    {"id":"fuel","name":"Fuel","fuelTypes":["FUTURE_FUEL"]},
                    {"id":"gearbox","name":"Gearbox","transmissions":["CVT_FUTURE"]},
                    {"id":"region","name":"Region","regions":["MARS"]},
                    {"id":"sort","name":"Sort","sort":"FUTURE_SORT"},
                    {"id":"currency","name":"Currency","displayCurrency":"CHF_FUTURE"}
                ]}""".trimIndent()
        }
        val repository = DataStoreSettingsRepository(dataStore)

        assertTrue(repository.savedSearches.first().isEmpty())
    }

    private fun repository(scope: CoroutineScope) =
        DataStoreSettingsRepository(dataStore(scope))

    private fun dataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = ::newPreferencesFile,
    )

    private fun newPreferencesFile(): File =
        File(temporaryFolder.root, "${UUID.randomUUID()}.preferences_pb")
}
