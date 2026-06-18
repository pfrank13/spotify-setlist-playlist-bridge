package com.github.pfrank13.setlistbridge.spotify

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Top-level response from the Spotify Search API (`GET /v1/search`).
 *
 * Each field is nullable since results are only present for the requested item types.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchResponse(
	val tracks: PaginatedResult<TrackItem>? = null,
	val albums: PaginatedResult<AlbumItem>? = null,
	val artists: PaginatedResult<ArtistItem>? = null,
	val playlists: PaginatedResult<PlaylistItem>? = null,
	val shows: PaginatedResult<ShowItem>? = null,
	val episodes: PaginatedResult<EpisodeItem>? = null,
	val audiobooks: PaginatedResult<AudiobookItem>? = null,
)
