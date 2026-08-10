package com.pfrank13.spotify_setlist_playlist_bridge.spotify

import org.springframework.web.client.RestClient
import org.springframework.web.client.body

class RestClientSpotifyClient(private val restClient: RestClient) : SpotifyClient {
  companion object {
    const val CREATE_PLAYLIST_URI = "/v1/me/playlists"
    const val ADD_ITEMS_TO_PLAYLIST_URI = "/v1/playlists/{playlistId}/items"
  }

  override fun createPlaylist(createPlaylistRequest: CreatePlaylistRequest): CreatePlaylistResponse {
    return restClient
      .post().uri(CREATE_PLAYLIST_URI)
      .body(createPlaylistRequest)
      .retrieve()
      .body<CreatePlaylistResponse>()
      ?: throw SpotifyException("Somehow the body was null")
  }

  override fun addItemsToPlaylist(
    playlistId: String,
    addItemsToPlaylistRequest: AddItemsToPlaylistRequest
  ): AddItemsToPlaylistResponse {
    return restClient.post().uri(ADD_ITEMS_TO_PLAYLIST_URI, playlistId)
      .body(addItemsToPlaylistRequest)
      .retrieve()
      .body<AddItemsToPlaylistResponse>()
      ?: throw SpotifyException("Somehow the body was null")
  }
}