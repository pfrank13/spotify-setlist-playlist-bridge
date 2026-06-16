package com.github.pfrank13.setlistbridge.spotify

/**
 * Client for the Spotify Web API (https://developer.spotify.com/documentation/web-api).
 */
interface SpotifyClient {

	/**
	 * Creates a playlist for the current Spotify user.
	 *
	 * @param name the name of the new playlist
	 * @param description optional playlist description
	 * @param public whether the playlist should be public
	 * @return the created [Playlist]
	 * @throws SpotifyException if the request fails or the response cannot be read
	 */
	fun createPlaylist(name: String, description: String? = null, isPublic: Boolean = true): Playlist

	companion object {
		const val CREATE_PLAYLIST_URI = "/v1/me/playlists"
	}
}
