package com.pfrank13.spotify_setlist_playlist_bridge.spotify

import org.springframework.web.client.RestClient
import org.springframework.web.client.body

class RestClientSpotifyClient(private val restClient: RestClient) : SpotifyClient {
  companion object {
    const val CREATE_PLAYLIST_URI = "/v1/me/playlists"
    const val ADD_ITEMS_TO_PLAYLIST_URI = "/v1/playlists/{playlistId}/items"
    const val SEARCH_FOR_ITEMS_URI = "/v1/search"
    object SearchForItemsParams{
      const val Q = "q"
      const val TYPE = "type"
      const val LIMIT = "limit"
    }
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

  override fun searchForItems(searchRequest: SearchForItemsRequest): SearchForItemsResponse {
    return restClient.get().uri { uriBuilder ->
      uriBuilder.path(SEARCH_FOR_ITEMS_URI)
        .queryParam(SearchForItemsParams.Q, searchRequest.q)
        .queryParam(SearchForItemsParams.TYPE, searchRequest.type.value)
      if (searchRequest.limit != null) {
        uriBuilder.queryParam(SearchForItemsParams.LIMIT, searchRequest.limit)
      }
      uriBuilder.build()
    }
      .retrieve()
      .body<SearchForItemsResponse>()
      ?: throw SpotifyException("Somehow the body was null")
  }
}