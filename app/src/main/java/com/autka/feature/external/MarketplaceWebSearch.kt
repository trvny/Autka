package com.autka.feature.external

import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import java.net.URLEncoder
import java.util.Locale

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
        // Keep the Polish-locale aggregators here deliberately: they are already the
        // Europe-facing destinations exposed by MarketplaceSearchLinks. mobile.de adds
        // a native EU marketplace without widening this fallback into a general crawler.
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

                // Repeat sanitized terms in every OR branch so neither operator
                // precedence nor user-supplied search syntax can broaden the site scope.
                val query = domains.joinToString(" OR ") { domain -> "$terms site:$domain" }
                val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
                MarketplaceWebSearchLink(region, "https://search.brave.com/search?q=$encoded")
            }
    }

    /**
     * Six words / 40 chars keeps the largest regional query below Brave's documented
     * 400-char / 50-word API limit. Search operators are neutralized before interpolation
     * so only Autka controls the site scope.
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
            .mapNotNull(::sanitizeSearchTerm)

        val accepted = mutableListOf<String>()
        var oversizedFallback: String? = null
        for (word in words) {
            if (accepted.size >= 6) break
            if (word.length > 40) {
                if (oversizedFallback == null) oversizedFallback = word.take(40)
                continue
            }
            val nextLength = accepted.sumOf { it.length } + accepted.size + word.length
            if (nextLength > 40) continue
            accepted += word
        }
        return accepted.joinToString(" ").ifEmpty { oversizedFallback.orEmpty() }
    }

    private fun sanitizeSearchTerm(word: String): String? {
        val dequoted = word
            .replace("\"", "")
            .replace("(", "")
            .replace(")", "")
            .replace("|", "")
            .trimStart('-')
        if (dequoted.isEmpty() || dequoted.startsWith('!')) return null

        val colonPrefix = dequoted.substringBefore(':', missingDelimiterValue = "")
        if (':' in dequoted && colonPrefix.isNotEmpty() && colonPrefix.all(Char::isLetter)) return null

        val literal = dequoted.replace(":", "")
        if (literal.isEmpty()) return null
        return if (literal in setOf("AND", "OR", "NOT")) {
            literal.lowercase(Locale.ROOT)
        } else {
            literal
        }
    }
}
