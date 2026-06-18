package com.github.pfrank13.setlistbridge.spotify

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class RestClientSpotifyClient(
	private val spotifyRestClient: RestClient,
) : SpotifyClient {

	override fun createPlaylist(name: String, description: String?, isPublic: Boolean): Playlist {
		val request = CreatePlaylistRequest(name = name, description = description, isPublic = isPublic)

		try {
			return spotifyRestClient.post()
				.uri(SpotifyClient.CREATE_PLAYLIST_URI)
				.body(request)
				.retrieve()
				.body(Playlist::class.java)
				?: throw SpotifyException("Empty response body when creating playlist '$name'")
		} catch (ex: RestClientException) {
			throw SpotifyException("Failed to create playlist '$name'", ex)
		}
	}

	override fun addItemsToPlaylist(playlistId: String, uris: List<String>, position: Int?): SnapshotResponse {
		val request = AddItemsToPlaylistRequest(uris = uris, position = position)

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
