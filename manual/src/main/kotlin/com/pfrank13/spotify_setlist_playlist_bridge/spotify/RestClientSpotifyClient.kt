package com.pfrank13.spotify_setlist_playlist_bridge.spotify

import org.springframework.web.client.RestClient
import org.springframework.web.client.body

class RestClientSpotifyClient(private val restClient: RestClient) : SpotifyClient {
  companion object {
    const val CREATE_PLAYLIST_URI = "/v1/me/playlists"
  }

  override fun createPlaylist(createPlaylistRequest: CreatePlaylistRequest): CreatePlaylistResponse {
    return restClient
        .post().uri(CREATE_PLAYLIST_URI)
        .body(createPlaylistRequest)
        .retrieve()
        .body<CreatePlaylistResponse>()
      ?: throw SpotifyException("Somehow the body was null")
  }
}