package com.github.pfrank13.setlistbridge.spotify

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class RestClientSpotifyClient(
	private val spotifyRestClient: RestClient,
) : SpotifyClient {

	override fun createPlaylist(request: CreatePlaylistRequest): Playlist {
		try {
			return spotifyRestClient.post()
				.uri(SpotifyClient.CREATE_PLAYLIST_URI)
				.body(request)
				.retrieve()
				.body(Playlist::class.java)
				?: throw SpotifyException("Empty response body when creating playlist '${request.name}'")
		} catch (ex: RestClientException) {
			throw SpotifyException("Failed to create playlist '${request.name}'", ex)
		}
	}

	override fun addItemsToPlaylist(playlistId: String, request: AddItemsToPlaylistRequest): SnapshotResponse {
		try {
			return spotifyRestClient.post()
				.uri(SpotifyClient.ADD_ITEMS_TO_PLAYLIST_URI, playlistId)
				.body(request)
				.retrieve()
				.body(SnapshotResponse::class.java)
				?: throw SpotifyException("Empty response body when adding items to playlist '$playlistId'")
		} catch (ex: RestClientException) {
			throw SpotifyException("Failed to add items to playlist '$playlistId'", ex)
		}
	}

	override fun searchForItems(
		q: String,
		type: Set<SearchItemType>,
		market: String?,
		limit: Int?,
		offset: Int?,
		includeExternal: String?,
	): SearchResponse {
		try {
			return spotifyRestClient.get()
				.uri { builder ->
					builder.path(SpotifyClient.SEARCH_URI)
						.queryParam("q", q)
						.queryParam("type", type.joinToString(",") { it.value })
					market?.let { builder.queryParam("market", it) }
					limit?.let { builder.queryParam("limit", it) }
					offset?.let { builder.queryParam("offset", it) }
					includeExternal?.let { builder.queryParam("include_external", it) }
					builder.build()
				}
				.retrieve()
				.body(SearchResponse::class.java)
				?: throw SpotifyException("Empty response body when searching for '$q'")
		} catch (ex: RestClientException) {
			throw SpotifyException("Failed to search for '$q'", ex)
		}
	}
}
