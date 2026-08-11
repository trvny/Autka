package com.autka.core.model

/** A user-named local snapshot of [SearchFilter]. */
data class SavedSearch(
    val id: String,
    val name: String,
    val filter: SearchFilter,
)
