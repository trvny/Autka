package com.autka.data.repository

import com.autka.core.model.CarOffer
import com.autka.core.model.SearchFilter
import com.autka.data.local.CarOfferDao
import com.autka.data.local.toEntity
import com.autka.data.local.toModel
import com.autka.data.remote.CarOfferSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstCarOfferRepository @Inject constructor(
    private val dao: CarOfferDao,
    private val sources: Set<@JvmSuppressWildcards CarOfferSource>,
) : CarOfferRepository {

    override fun observeOffers(): Flow<List<CarOffer>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeOffer(id: String): Flow<CarOffer?> =
        dao.observeById(id).map { it?.toModel() }

    override suspend fun refresh(filter: SearchFilter): List<String> = coroutineScope {
        // CarOfferSource represents a transport (backend, local demo, etc.), not a
        // marketplace. A marketplace filter such as "otomoto" must still query the
        // backend transport, which forwards that filter to the server.
        val active = sources.filter { it.isEnabled }
        val disabledIds = sources.filterNot { it.isEnabled }.map { it.sourceId }
        if (disabledIds.isNotEmpty()) {
            // Remove sample or retired transport rows immediately after an upgrade instead
            // of leaving them visible until the normal age-based cache cleanup runs.
            dao.deleteBySourceIds(disabledIds)
        }

        val outcomes = active.map { source ->
            async { source to runCatching { source.fetch(filter) } }
        }.awaitAll()

        val fetchedAt = System.currentTimeMillis()
        val successful = outcomes.filter { it.second.isSuccess }
        val results = successful.mapNotNull { (_, result) -> result.getOrNull() }.flatten()
        if (results.isNotEmpty()) {
            dao.upsertAll(results.map { it.toEntity(fetchedAt) })
        }

        // deleteStale is global, while refresh is filter-scoped and one backend transport
        // may represent many marketplace sourceIds. Only a complete, unfiltered refresh
        // from every active transport is authoritative enough to expire unrelated cache.
        if (active.isNotEmpty() && successful.size == active.size && filter.isFullCatalogueScope()) {
            dao.deleteStale(fetchedAt - LOCAL_CACHE_MAX_AGE_MS)
        }

        outcomes.filter { it.second.isFailure }.map { it.first.sourceId }
    }

    private fun SearchFilter.isFullCatalogueScope(): Boolean {
        val defaults = SearchFilter()
        // Sort changes presentation only. Every other field can narrow the fetched scope;
        // data-class equality makes future filter fields conservative by default.
        return copy(sort = defaults.sort) == defaults
    }

    private companion object {
        const val LOCAL_CACHE_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1_000L
    }
}
