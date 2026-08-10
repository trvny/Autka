package com.autka.feature.external

import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import java.net.URLEncoder

/**
 * Builds a browser-only Brave Search query over deliberately index-friendly marketplace
 * domains. No marketplace content is fetched or stored by Autka.
 */
object MarketplaceWebSearch {

    private data class Target(val domain: String, val regions: Set<Region>)

    private val targets = listOf(
        Target("otomoto.pl", setOf(Region.POLAND)),
        Target("olx.pl", setOf(Region.POLAND)),
        Target("autoplac.pl", setOf(Region.POLAND)),
        Target("autotrader.pl", setOf(Region.POLAND)),
        Target("autouncle.pl", setOf(Region.POLAND, Region.EUROPE)),
        Target("autoscout24.pl", setOf(Region.POLAND, Region.EUROPE)),
        Target("mobile.de", setOf(Region.EUROPE)),
        Target("copart.com", setOf(Region.USA)),
        Target("cars.com", setOf(Region.USA)),
        Target("autotrader.com", setOf(Region.USA)),
    )

    fun url(filter: SearchFilter): String? {
        val terms = boundedTerms(filter)
        if (terms.isEmpty()) return null

        val domains = targets
            .filter { target -> target.regions.any { it in filter.regions } }
            .map { it.domain }
        if (domains.isEmpty()) return null

        // Brave documents `site:` and uppercase OR operators. Keep user terms once so the
        // default all-region query remains comfortably below the 400-char / 50-word API cap.
        val siteScope = domains.joinToString(" OR ") { "site:$it" }
        val query = "$terms $siteScope"
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return "https://search.brave.com/search?q=$encoded"
    }

    /** Web search is intentionally bounded; marketplace deep links are not query-size limited. */
    private fun boundedTerms(filter: SearchFilter): String {
        val raw = listOfNotNull(
            filter.make,
            filter.model,
            filter.query.trim().ifEmpty { null },
        ).joinToString(" ").trim()

        return raw
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(20)
            .joinToString(" ")
            .take(160)
            .trimEnd()
    }
}
