package com.github.pfrank13.setlistbridge.spotify

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

data class CreatePlaylistRequest(
	val name: String,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	val description: String? = null,
	@JsonProperty("public")
	val isPublic: Boolean = true,
)
