package com.autka.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

class ImportAssumptionPresetSettingsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `import preset round trips numeric assumptions`() = runTest {
        val repository = repository(backgroundScope)

        repository.saveImportAssumptionPreset(
            name = "Container",
            shippingUsd = 2_750.0,
            customsDutyRate = 0.10,
            vatRate = 0.23,
        )

        val preset = repository.importAssumptionPresets.first().single()
        assertEquals("Container", preset.name)
        assertEquals(2_750.0, preset.shippingUsd, 0.0)
        assertEquals(0.10, preset.customsDutyRate, 0.0)
        assertEquals(0.23, preset.vatRate, 0.0)
    }

    @Test
    fun `saving identical assumptions renames instead of duplicating`() = runTest {
        val repository = repository(backgroundScope)
        repository.saveImportAssumptionPreset("Old", 2_400.0, 0.10, 0.23)
        val original = repository.importAssumptionPresets.first().single()

        repository.saveImportAssumptionPreset("Current", 2_400.0, 0.10, 0.23)

        val saved = repository.importAssumptionPresets.first().single()
        assertEquals(original.id, saved.id)
        assertEquals("Current", saved.name)
    }

    @Test
    fun `preset can be deleted independently`() = runTest {
        val repository = repository(backgroundScope)
        repository.saveImportAssumptionPreset("A", 2_400.0, 0.10, 0.23)
        repository.saveImportAssumptionPreset("B", 3_000.0, 0.10, 0.23)
        val a = repository.importAssumptionPresets.first().single { it.name == "A" }

        repository.deleteImportAssumptionPreset(a.id)

        assertEquals(listOf("B"), repository.importAssumptionPresets.first().map { it.name })
    }

    @Test
    fun `invalid assumptions are rejected`() = runTest {
        val repository = repository(backgroundScope)

        repository.saveImportAssumptionPreset("Bad shipping", Double.NaN, 0.10, 0.23)
        repository.saveImportAssumptionPreset("Bad duty", 2_400.0, 1.01, 0.23)
        repository.saveImportAssumptionPreset("Bad VAT", 2_400.0, 0.10, -0.01)

        assertTrue(repository.importAssumptionPresets.first().isEmpty())
    }

    @Test
    fun `malformed preset storage recovers to an empty list`() = runTest {
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("import_assumption_presets_v1")] = "{broken"
        }
        val repository = DataStoreSettingsRepository(dataStore)

        assertTrue(repository.importAssumptionPresets.first().isEmpty())
        repository.saveImportAssumptionPreset("Recovered", 2_400.0, 0.10, 0.23)
        assertEquals(listOf("Recovered"), repository.importAssumptionPresets.first().map { it.name })
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
