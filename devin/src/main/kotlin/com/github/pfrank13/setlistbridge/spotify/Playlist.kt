package com.github.pfrank13.setlistbridge.spotify

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response model for the Spotify `playlist` object.
 *
 * See https://developer.spotify.com/documentation/web-api/reference/create-playlist
 */
data class Playlist(
	val id: String,
	val name: String,
	val description: String?,
	@JsonProperty("public")
	val isPublic: Boolean?,
	@JsonProperty("external_urls")
	val externalUrls: Map<String, String>,
	val href: String,
	val uri: String,
	@JsonProperty("snapshot_id")
	val snapshotId: String,
)
