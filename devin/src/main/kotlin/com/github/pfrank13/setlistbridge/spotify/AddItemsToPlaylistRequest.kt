package com.github.pfrank13.setlistbridge.spotify

import com.fasterxml.jackson.annotation.JsonInclude

data class AddItemsToPlaylistRequest(
	val uris: List<String>,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	val position: Int? = null,
)
