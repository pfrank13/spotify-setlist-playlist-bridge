package com.pfrank13.spotify_setlist_playlist_bridge.orchestration

data class Artist(val externalArtistId: String,
  val name: String)

data class Song(val externalSongId: String,
  val artist: Artist,
  val name: String)

data class Playlist(val externalPlaylistId: String,
                    val name: String,
                    val songs: Set<Song>)

interface SetlistOrchestration {
  fun transferSetlist(externalSetlistId: String): Playlist
}