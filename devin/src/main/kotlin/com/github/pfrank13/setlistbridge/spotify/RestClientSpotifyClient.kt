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
}
