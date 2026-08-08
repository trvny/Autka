package com.autka.data.repository

import com.autka.core.model.CarOffer
import com.autka.core.model.Currency
import com.autka.core.model.FuelType
import com.autka.core.model.Money
import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import com.autka.core.model.Transmission
import com.autka.data.local.CarOfferDao
import com.autka.data.local.CarOfferEntity
import com.autka.data.local.toEntity
import com.autka.data.remote.CarOfferSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstCarOfferRepositoryTest {

    @Test
    fun `marketplace filter does not suppress backend transport`() = runTest {
        val dao = FakeDao()
        val backend = RecordingSource(
            sourceId = "backend",
            offers = listOf(offer(id = "otomoto:1", sourceId = "otomoto")),
        )
        val repository = OfflineFirstCarOfferRepository(dao, setOf(backend))
        val filter = SearchFilter(sourceIds = setOf("otomoto"))

        val failed = repository.refresh(filter)

        assertTrue(failed.isEmpty())
        assertEquals(filter, backend.receivedFilter)
        assertNotNull(dao.rows.value.singleOrNull { it.id == "otomoto:1" })
        assertNull(dao.lastDeleteStaleCutoff)
    }

    @Test
    fun `disabled transport rows are removed immediately`() = runTest {
        val oldMock = offer(id = "mock:old", sourceId = "mock").toEntity()
        val dao = FakeDao(initialRows = listOf(oldMock))
        val disabledMock = RecordingSource(
            sourceId = "mock",
            offers = emptyList(),
            enabled = false,
        )
        val repository = OfflineFirstCarOfferRepository(dao, setOf(disabledMock))

        repository.refresh(SearchFilter())

        assertNull(dao.rows.value.singleOrNull { it.sourceId == "mock" })
        assertEquals(listOf("mock"), dao.lastDeletedSourceIds)
        assertNull(dao.lastDeleteStaleCutoff)
    }

    @Test
    fun `successful sources merge when another source fails`() = runTest {
        val dao = FakeDao()
        val first = RecordingSource(
            sourceId = "backend-a",
            offers = listOf(offer(id = "source-a:1", sourceId = "source-a")),
        )
        val second = RecordingSource(
            sourceId = "backend-b",
            offers = listOf(offer(id = "source-b:1", sourceId = "source-b")),
        )
        val repository = OfflineFirstCarOfferRepository(
            dao,
            setOf(first, FailingSource("broken"), second),
        )

        val failed = repository.refresh(SearchFilter())

        assertEquals(setOf("broken"), failed.toSet())
        assertEquals(setOf("source-a:1", "source-b:1"), dao.rows.value.map { it.id }.toSet())
        assertNull(dao.lastDeleteStaleCutoff)
    }

    @Test
    fun `total source outage preserves even aged offline cache`() = runTest {
        val now = System.currentTimeMillis()
        val recent = offer(id = "broken:recent", sourceId = "broken-marketplace")
            .toEntity(fetchedAt = now - 6L * 24L * 60L * 60L * 1_000L)
        val expired = offer(id = "broken:expired", sourceId = "broken-marketplace")
            .toEntity(fetchedAt = now - 8L * 24L * 60L * 60L * 1_000L)
        val dao = FakeDao(initialRows = listOf(recent, expired))
        val repository = OfflineFirstCarOfferRepository(dao, setOf(FailingSource("broken")))

        val failed = repository.refresh(SearchFilter())

        assertEquals(listOf("broken"), failed)
        assertEquals(setOf("broken:recent", "broken:expired"), dao.rows.value.map { it.id }.toSet())
        assertNull(dao.lastDeleteStaleCutoff)
    }

    @Test
    fun `filtered success does not expire unrelated offline cache`() = runTest {
        val expired = offer(id = "other:expired", sourceId = "other-marketplace")
            .toEntity(fetchedAt = System.currentTimeMillis() - 8L * 24L * 60L * 60L * 1_000L)
        val dao = FakeDao(initialRows = listOf(expired))
        val backend = RecordingSource(sourceId = "backend", offers = emptyList())
        val repository = OfflineFirstCarOfferRepository(dao, setOf(backend))

        val failed = repository.refresh(SearchFilter(make = "Toyota"))

        assertTrue(failed.isEmpty())
        assertNotNull(dao.rows.value.singleOrNull { it.id == "other:expired" })
        assertNull(dao.lastDeleteStaleCutoff)
    }

    @Test
    fun `age cap resumes after complete unfiltered refresh`() = runTest {
        val expired = offer(id = "old:expired", sourceId = "old-marketplace")
            .toEntity(fetchedAt = System.currentTimeMillis() - 8L * 24L * 60L * 60L * 1_000L)
        val dao = FakeDao(initialRows = listOf(expired))
        val first = RecordingSource(sourceId = "backend-a", offers = emptyList())
        val second = RecordingSource(sourceId = "backend-b", offers = emptyList())
        val repository = OfflineFirstCarOfferRepository(dao, setOf(first, second))

        val failed = repository.refresh(SearchFilter())

        assertTrue(failed.isEmpty())
        assertNull(dao.rows.value.singleOrNull { it.id == "old:expired" })
        assertNotNull(dao.lastDeleteStaleCutoff)
    }

    private class RecordingSource(
        override val sourceId: String,
        private val offers: List<CarOffer>,
        private val enabled: Boolean = true,
    ) : CarOfferSource {
        override val displayName = sourceId
        override val isEnabled = enabled
        var receivedFilter: SearchFilter? = null

        override suspend fun fetch(filter: SearchFilter): List<CarOffer> {
            receivedFilter = filter
            return offers
        }
    }

    private class FailingSource(override val sourceId: String) : CarOfferSource {
        override val displayName = sourceId
        override val isEnabled = true
        override suspend fun fetch(filter: SearchFilter): List<CarOffer> = error("source unavailable")
    }

    private class FakeDao(
        initialRows: List<CarOfferEntity> = emptyList(),
    ) : CarOfferDao {
        val rows = MutableStateFlow(initialRows)
        var lastDeleteStaleCutoff: Long? = null
        var lastDeletedSourceIds: List<String>? = null

        override suspend fun upsertAll(offers: List<CarOfferEntity>) {
            val incoming = offers.associateBy { it.id }
            rows.value = (rows.value.filterNot { it.id in incoming } + offers)
        }

        override fun observeAll(): Flow<List<CarOfferEntity>> = rows

        override fun observeById(id: String): Flow<CarOfferEntity?> =
            MutableStateFlow(rows.value.firstOrNull { it.id == id })

        override suspend fun deleteBySourceIds(sourceIds: List<String>) {
            lastDeletedSourceIds = sourceIds
            rows.value = rows.value.filterNot { it.sourceId in sourceIds }
        }

        override suspend fun deleteStale(olderThanEpochMs: Long) {
            lastDeleteStaleCutoff = olderThanEpochMs
            rows.value = rows.value.filter { it.fetchedAtEpochMs >= olderThanEpochMs }
        }

        override suspend fun clear() {
            rows.value = emptyList()
        }
    }

    private fun offer(id: String, sourceId: String) = CarOffer(
        id = id,
        sourceId = sourceId,
        title = "Toyota Corolla",
        make = "Toyota",
        model = "Corolla",
        year = 2020,
        mileageKm = 50_000,
        price = Money(50_000.0, Currency.PLN),
        fuelType = FuelType.PETROL,
        transmission = Transmission.MANUAL,
        powerHp = null,
        location = null,
        region = Region.POLAND,
        thumbnailUrl = null,
        imageUrls = emptyList(),
        listingUrl = "https://example.test/$id",
        postedAtEpochMs = 0L,
    )
}
