package com.github.pfrank13.setlistbridge.spotify

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Top-level response from the Spotify Search API (`GET /v1/search`).
 *
 * Each field is nullable since results are only present for the requested item types.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchResponse(
	val tracks: PaginatedResult<TrackItem>?,
	val albums: PaginatedResult<AlbumItem>?,
	val artists: PaginatedResult<ArtistItem>?,
	val playlists: PaginatedResult<PlaylistItem>?,
	val shows: PaginatedResult<ShowItem>?,
	val episodes: PaginatedResult<EpisodeItem>?,
	val audiobooks: PaginatedResult<AudiobookItem>?,
)
