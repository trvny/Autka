package com.autka.feature.external

import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import java.net.URLEncoder

data class MarketplaceWebSearchLink(val region: Region, val url: String)

/**
 * Builds browser-only Brave Search queries over deliberately index-friendly marketplace
 * domains. No marketplace content is fetched or stored by Autka.
 *
 * The target list is intentionally narrower than MarketplaceSearchLinks: Facebook and
 * IAAI are omitted because their public listing pages are poor search-index targets.
 */
object MarketplaceWebSearch {

    private val targets = mapOf(
        Region.POLAND to listOf(
            "otomoto.pl",
            "olx.pl",
            "autoplac.pl",
            "autotrader.pl",
            "autouncle.pl",
            "autoscout24.pl",
        ),
        Region.EUROPE to listOf(
            "autouncle.pl",
            "autoscout24.pl",
            "mobile.de",
        ),
        Region.USA to listOf(
            "copart.com",
            "cars.com",
            "autotrader.com",
        ),
    )

    fun all(filter: SearchFilter): List<MarketplaceWebSearchLink> {
        val terms = boundedTerms(filter)
        if (terms.isEmpty()) return emptyList()

        return Region.entries
            .filter { it in filter.regions }
            .mapNotNull { region ->
                val domains = targets[region].orEmpty()
                if (domains.isEmpty()) return@mapNotNull null

                // Repeat terms in every OR branch so operator precedence cannot broaden
                // later domains beyond the user's vehicle query.
                val query = domains.joinToString(" OR ") { domain -> "$terms site:$domain" }
                val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
                MarketplaceWebSearchLink(region, "https://search.brave.com/search?q=$encoded")
            }
    }

    /**
     * Six words / 40 chars keeps the largest regional query below Brave's documented
     * 400-char / 50-word API limit while preserving whole tokens.
     */
    private fun boundedTerms(filter: SearchFilter): String {
        val words = listOfNotNull(
            filter.make,
            filter.model,
            filter.query.trim().ifEmpty { null },
        )
            .joinToString(" ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(6)

        val accepted = mutableListOf<String>()
        for (word in words) {
            val nextLength = accepted.sumOf { it.length } + accepted.size + word.length
            if (nextLength > 40) break
            accepted += word
        }
        return accepted.joinToString(" ")
    }
}
