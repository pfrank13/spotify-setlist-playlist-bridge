package com.github.pfrank13.setlistbridge.spotify

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class RestClientSpotifyClient(
	private val spotifyRestClient: RestClient,
) : SpotifyClient {

	override fun createPlaylist(name: String, description: String?, isPublic: Boolean): Playlist {
		val body = buildMap<String, Any?> {
			put("name", name)
			if (description != null) put("description", description)
			put("public", isPublic)
		}

		try {
			return spotifyRestClient.post()
				.uri("/v1/me/playlists")
				.body(body)
				.retrieve()
				.body(Playlist::class.java)
				?: throw SpotifyException("Empty response body when creating playlist '$name'")
		} catch (ex: RestClientException) {
			throw SpotifyException("Failed to create playlist '$name'", ex)
		}
	}
}
