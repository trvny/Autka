package com.autka.core.model

/**
 * An import/sourcing company — a business that brings a car INTO Poland (from US
 * auctions or elsewhere in Europe), as opposed to a marketplace you search.
 *
 * NOT searchable inventory and does NOT take a SearchFilter: most are brochure/quote
 * sites with a cost calculator. Surfaced on the offer DETAIL screen next to the
 * landed-cost breakdown ("import this car via:"), never as filtered deep-links.
 *
 * [origin] is the region the company imports FROM, matched against an offer's region.
 */
data class ImportService(
    val id: String,
    val displayName: String,
    val origin: Region,
    val url: String,
    val calculatorUrl: String? = null,
    val note: String? = null,
)
