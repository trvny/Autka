package com.autka.feature.external

import com.autka.core.model.SearchFilter
import java.net.URI
import java.net.URLEncoder

/**
 * Builds a browser-only Brave Search query over marketplaces already exposed by
 * [MarketplaceSearchLinks]. No marketplace content is fetched or stored by Autka.
 */
object MarketplaceWebSearch {

    fun url(filter: SearchFilter): String? {
        val terms = listOfNotNull(
            filter.make,
            filter.model,
            filter.query.trim().ifEmpty { null },
        ).joinToString(" ").trim()
        if (terms.isEmpty()) return null

        val domains = MarketplaceSearchLinks.all(filter)
            .mapNotNull { link -> runCatching { URI(link.url).host }.getOrNull() }
            .map { it.removePrefix("www.") }
            .distinct()
        if (domains.isEmpty()) return null

        val query = domains.joinToString(" OR ") { domain -> "$terms site:$domain" }
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return "https://search.brave.com/search?q=$encoded"
    }
}
