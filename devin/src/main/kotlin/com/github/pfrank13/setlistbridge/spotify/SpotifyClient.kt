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

	/**
	 * Adds items to an existing playlist.
	 *
	 * @param playlistId the Spotify ID of the playlist
	 * @param uris Spotify URIs of the items to add (max 100)
	 * @param position zero-based position to insert items; appends when null
	 * @return the [SnapshotResponse] containing the new snapshot id
	 * @throws SpotifyException if the request fails or the response cannot be read
	 */
	fun addItemsToPlaylist(playlistId: String, uris: List<String>, position: Int? = null): SnapshotResponse

	companion object {
		const val CREATE_PLAYLIST_URI = "/v1/me/playlists"
		const val ADD_ITEMS_TO_PLAYLIST_URI = "/v1/playlists/{playlistId}/items"
	}
}
