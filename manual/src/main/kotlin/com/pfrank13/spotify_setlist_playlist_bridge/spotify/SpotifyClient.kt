package com.pfrank13.spotify_setlist_playlist_bridge.spotify

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class CreatePlaylistRequest(
  val name: String,
  val public: Boolean = true,
  val collaborative: Boolean = false,
  val description: String? = null,
)

data class CreatePlaylistResponse(
  val id: String,
  @JsonProperty("snapshot_id")
  val snapshotId: String,
  val name: String,
  val public: Boolean,
  val collaborative: Boolean,
  val description: String?,
)

data class AddItemsToPlaylistRequest(
  val uris: List<URI>,
  val position: Int? = null,
)

data class AddItemsToPlaylistResponse(
  @JsonProperty("snapshot_id")
  val snapshotId: String,
)

enum class ItemType(val value: String) {
  ALBUM("album"),
  ARTIST("artist"),
  PLAYLIST("playlist"),
  TRACK("track"),
  SHOW("show"),
  EPISODE("episode"),
  AUDIOBOOK("audiobook"),
}

data class SearchForItemsRequest(
  val q: String,
  val type: ItemType,
  val limit: Int? = null,
)

data class TrackItem(
  val id: String,
  val name: String,
  val uri: URI,
)

data class Tracks(
  val limit: Int,
  val offset: Int,
  val next: URI,
  val previous: URI,
  val total: Int,
  val items: List<TrackItem>,
)

data class SearchForItemsResponse(
  val tracks: Tracks,
)

class SpotifyException : RuntimeException {
  constructor() : super()
  constructor(message: String?) : super(message)
  constructor(message: String?, cause: Throwable?) : super(message, cause)
  constructor(cause: Throwable?) : super(cause)
  constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(
    message,
    cause,
    enableSuppression,
    writableStackTrace
  )
}

interface SpotifyClient {
  fun createPlaylist(createPlaylistRequest: CreatePlaylistRequest): CreatePlaylistResponse
  fun addItemsToPlaylist(playlistId: String, addItemsToPlaylistRequest: AddItemsToPlaylistRequest): AddItemsToPlaylistResponse
  fun searchForItems(searchRequest: SearchForItemsRequest): SearchForItemsResponse
}