package com.github.pfrank13.setlistbridge.spotify

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.net.URI

/**
 * Generic paginated result wrapper used in Spotify Search API responses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PaginatedResult<T : Item>(
	val href: String,
	val limit: Int,
	val next: URI?,
	val offset: Int,
	val previous: URI?,
	val total: Int,
	val items: List<T>,
)
