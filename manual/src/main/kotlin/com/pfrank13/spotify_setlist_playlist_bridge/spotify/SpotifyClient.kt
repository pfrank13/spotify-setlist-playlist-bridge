package com.pfrank13.spotify_setlist_playlist_bridge.spotify

import com.fasterxml.jackson.annotation.JsonProperty

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
}