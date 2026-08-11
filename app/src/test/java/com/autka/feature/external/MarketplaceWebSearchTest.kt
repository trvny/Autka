package com.autka.feature.external

import com.autka.core.model.Region
import com.autka.core.model.SearchFilter
import java.net.URI
import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceWebSearchTest {

    @Test
    fun `Poland web search scopes every OR branch to the current vehicle terms`() {
        val link = MarketplaceWebSearch.all(
            SearchFilter(
                make = "BMW",
                model = "X5",
                regions = setOf(Region.POLAND),
            ),
        ).single()
        val query = decodedQuery(link.url)

        assertEquals(Region.POLAND, link.region)
        assertTrue(query.startsWith("BMW X5 site:otomoto.pl"))
        assertTrue(query.contains("OR BMW X5 site:olx.pl"))
        assertFalse(query.contains("site:cars.com"))
    }

    @Test
    fun `Boolean user words cannot escape marketplace site scope`() {
        val link = MarketplaceWebSearch.all(
            SearchFilter(
                query = "BMW OR Audi",
                regions = setOf(Region.POLAND),
            ),
        ).single()
        val query = decodedQuery(link.url)

        assertTrue(query.startsWith("BMW or Audi site:otomoto.pl"))
        assertTrue(query.contains("OR BMW or Audi site:olx.pl"))
        assertFalse(query.startsWith("BMW OR Audi"))
    }

    @Test
    fun `search syntax tokens cannot replace marketplace site scope`() {
        val link = MarketplaceWebSearch.all(
            SearchFilter(
                query = "site:example.com -site:otomoto.pl filetype:pdf \"BMW X5\"",
                regions = setOf(Region.POLAND),
            ),
        ).single()
        val query = decodedQuery(link.url)

        assertTrue(query.startsWith("BMW X5 site:otomoto.pl"))
        assertFalse(query.contains("example.com"))
        assertFalse(query.contains("filetype:"))
        assertFalse(query.contains("\""))
    }

    @Test
    fun `USA web search excludes Poland-only marketplace domains`() {
        val link = MarketplaceWebSearch.all(
            SearchFilter(
                query = "Mustang Mach-E",
                regions = setOf(Region.USA),
            ),
        ).single()
        val query = decodedQuery(link.url)

        assertTrue(query.contains("Mustang Mach-E site:cars.com"))
        assertTrue(query.contains("Mustang Mach-E site:autotrader.com"))
        assertFalse(query.contains("site:otomoto.pl"))
    }

    @Test
    fun `all-region search produces one bounded query per region`() {
        val longQuery = (1..80).joinToString(" ") { "term$it" }
        val links = MarketplaceWebSearch.all(SearchFilter(query = longQuery))

        assertEquals(Region.entries.size, links.size)
        links.forEach { link ->
            val query = decodedQuery(link.url)
            assertTrue(query.length <= 400)
            assertTrue(query.split(Regex("\\s+")).size <= 50)
            assertFalse(query.contains("site:facebook.com"))
            assertFalse(query.contains("site:iaai.com"))
        }
    }

    @Test
    fun `term bound skips oversized token when useful words follow`() {
        val longToken = "x".repeat(80)
        val link = MarketplaceWebSearch.all(
            SearchFilter(
                query = "$longToken BMW X5",
                regions = setOf(Region.USA),
            ),
        ).single()
        val query = decodedQuery(link.url)

        assertTrue(query.startsWith("BMW X5 site:copart.com"))
        assertFalse(query.contains(longToken))
    }

    @Test
    fun `word cap is applied after oversized tokens are skipped`() {
        val oversized = (1..6).joinToString(" ") { "x".repeat(50 + it) }
        val link = MarketplaceWebSearch.all(
            SearchFilter(
                query = "$oversized BMW X5",
                regions = setOf(Region.USA),
            ),
        ).single()
        val query = decodedQuery(link.url)

        assertTrue(query.startsWith("BMW X5 site:copart.com"))
    }

    @Test
    fun `single oversized token still keeps web search available`() {
        val longToken = "x".repeat(80)
        val link = MarketplaceWebSearch.all(
            SearchFilter(
                query = longToken,
                regions = setOf(Region.USA),
            ),
        ).single()
        val query = decodedQuery(link.url)

        assertTrue(query.startsWith("${"x".repeat(40)} site:copart.com"))
    }

    @Test
    fun `term bound keeps whole normal words`() {
        val link = MarketplaceWebSearch.all(
            SearchFilter(
                query = "alpha beta gamma delta epsilon zeta supercalifragilisticexpialidocious",
                regions = setOf(Region.USA),
            ),
        ).single()
        val query = decodedQuery(link.url)

        assertTrue(query.startsWith("alpha beta gamma delta epsilon"))
        assertFalse(query.contains("supercalif"))
    }

    @Test
    fun `web search is hidden when there are no search terms`() {
        assertTrue(MarketplaceWebSearch.all(SearchFilter(regions = setOf(Region.POLAND))).isEmpty())
    }

    private fun decodedQuery(url: String): String {
        val rawQuery = requireNotNull(URI(url).rawQuery)
        val encoded = rawQuery.substringAfter("q=")
        return URLDecoder.decode(encoded, "UTF-8")
    }
}
