package com.github.pfrank13.setlistbridge.spotify

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Generic paginated result wrapper used in Spotify Search API responses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PaginatedResult<T : Item>(
	val href: String,
	val limit: Int,
	val next: String?,
	val offset: Int,
	val previous: String?,
	val total: Int,
	val items: List<T>,
)
