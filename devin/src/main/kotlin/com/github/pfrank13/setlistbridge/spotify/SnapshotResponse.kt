package com.github.pfrank13.setlistbridge.spotify

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response returned by the Spotify Web API when items are added to a playlist.
 *
 * See https://developer.spotify.com/documentation/web-api/reference/add-items-to-playlist
 */
data class SnapshotResponse(
	@JsonProperty("snapshot_id")
	val snapshotId: String,
)
