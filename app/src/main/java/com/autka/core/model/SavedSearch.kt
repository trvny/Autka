package com.autka.core.model

/** A user-named local snapshot of [SearchFilter] and its price/display currency. */
data class SavedSearch(
    val id: String,
    val name: String,
    val filter: SearchFilter,
    val displayCurrency: Currency,
)
