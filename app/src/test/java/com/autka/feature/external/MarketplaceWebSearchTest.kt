package com.autka.feature.external

import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import java.net.URI
import java.net.URLDecoder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceWebSearchTest {

    @Test
    fun `Poland web search scopes the current query to Polish marketplace domains`() {
        val url = requireNotNull(
            MarketplaceWebSearch.url(
                SearchFilter(
                    make = "BMW",
                    model = "X5",
                    regions = setOf(Region.POLAND),
                ),
            ),
        )
        val query = decodedQuery(url)

        assertTrue(query.startsWith("BMW X5 site:otomoto.pl"))
        assertTrue(query.contains("OR site:olx.pl"))
        assertFalse(query.contains("site:cars.com"))
    }

    @Test
    fun `USA web search excludes Poland-only marketplace domains`() {
        val url = requireNotNull(
            MarketplaceWebSearch.url(
                SearchFilter(
                    query = "Mustang Mach-E",
                    regions = setOf(Region.USA),
                ),
            ),
        )
        val query = decodedQuery(url)

        assertTrue(query.contains("site:cars.com"))
        assertTrue(query.contains("site:autotrader.com"))
        assertFalse(query.contains("site:otomoto.pl"))
    }

    @Test
    fun `all-region web search stays bounded and skips poor index targets`() {
        val longQuery = (1..80).joinToString(" ") { "term$it" }
        val url = requireNotNull(MarketplaceWebSearch.url(SearchFilter(query = longQuery)))
        val query = decodedQuery(url)

        assertTrue(query.length <= 400)
        assertFalse(query.contains("site:facebook.com"))
        assertFalse(query.contains("site:iaai.com"))
        assertTrue(query.contains("site:otomoto.pl"))
        assertTrue(query.contains("site:autotrader.com"))
    }

    @Test
    fun `web search is hidden when there are no search terms`() {
        assertNull(MarketplaceWebSearch.url(SearchFilter(regions = setOf(Region.POLAND))))
    }

    private fun decodedQuery(url: String): String {
        val rawQuery = requireNotNull(URI(url).rawQuery)
        val encoded = rawQuery.substringAfter("q=")
        return URLDecoder.decode(encoded, "UTF-8")
    }
}
