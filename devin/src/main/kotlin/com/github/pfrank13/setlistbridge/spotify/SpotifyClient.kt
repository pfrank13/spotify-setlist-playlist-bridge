package com.github.pfrank13.setlistbridge.spotify

/**
 * Client for the Spotify Web API (https://developer.spotify.com/documentation/web-api).
 */
interface SpotifyClient {

	/**
	 * Creates a playlist for the current Spotify user.
	 *
	 * @param request the [CreatePlaylistRequest] containing playlist details
	 * @return the created [Playlist]
	 * @throws SpotifyException if the request fails or the response cannot be read
	 */
	fun createPlaylist(request: CreatePlaylistRequest): Playlist

	/**
	 * Adds items to an existing playlist.
	 *
	 * @param playlistId the Spotify ID of the playlist
	 * @param request the [AddItemsToPlaylistRequest] containing URIs and optional position
	 * @return the [SnapshotResponse] containing the new snapshot id
	 * @throws SpotifyException if the request fails or the response cannot be read
	 */
	fun addItemsToPlaylist(playlistId: String, request: AddItemsToPlaylistRequest): SnapshotResponse

	companion object {
		const val CREATE_PLAYLIST_URI = "/v1/me/playlists"
		const val ADD_ITEMS_TO_PLAYLIST_URI = "/v1/playlists/{playlistId}/items"
	}
}
