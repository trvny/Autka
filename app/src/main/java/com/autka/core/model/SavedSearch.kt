package com.autka.core.model

const val MAX_SAVED_SEARCH_NAME_LENGTH = 80

/** A user-named local snapshot of [SearchFilter] and its price/display currency. */
data class SavedSearch(
    val id: String,
    val name: String,
    val filter: SearchFilter,
    val displayCurrency: Currency,
)
