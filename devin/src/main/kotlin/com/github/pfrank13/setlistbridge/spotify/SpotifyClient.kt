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

	/**
	 * Searches for items in the Spotify catalog.
	 *
	 * @param q the search query
	 * @param type the set of [SearchItemType]s to search across
	 * @param market optional ISO 3166-1 alpha-2 country code
	 * @param limit optional maximum number of results per item type (0-10, default 5)
	 * @param offset optional index of the first result to return (0-1000, default 0)
	 * @param includeExternal optional; if "audio", marks externally hosted content as playable
	 * @return the [SearchResponse] containing paginated results for each requested type
	 * @throws SpotifyException if the request fails or the response cannot be read
	 */
	fun searchForItems(
		q: String,
		type: Set<SearchItemType>,
		market: String? = null,
		limit: Int? = null,
		offset: Int? = null,
		includeExternal: String? = null,
	): SearchResponse

	companion object {
		const val CREATE_PLAYLIST_URI = "/v1/me/playlists"
		const val ADD_ITEMS_TO_PLAYLIST_URI = "/v1/playlists/{playlistId}/items"
		const val SEARCH_URI = "/v1/search"

		const val QUERY_PARAM = "q"
		const val TYPE_PARAM = "type"
		const val MARKET_PARAM = "market"
		const val LIMIT_PARAM = "limit"
		const val OFFSET_PARAM = "offset"
		const val INCLUDE_EXTERNAL_PARAM = "include_external"
	}
}
