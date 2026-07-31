package com.pfrank13.spotify_setlist_playlist_bridge.setlistfm

import java.net.URI

data class Artist(
  val name: String,
  val url: URI
)

data class City(
  val name: String,
  val stateCode: String
)

data class Venue(
  val id: String,
  val name: String,
  val url: URI,
  val city: City,
)

data class Tour(val name: String)

data class Song(
  val name: String,
  val with: Artist? = null,
  val cover: Artist? = null,
)

data class Set(
  val name: String,
  val song: List<Song>,
)

data class Setlist(
  val id: String,
  val eventDate: String,
  val url: URI,
  val set: List<Set>,
  val artist: Artist,
  val venue: Venue,
  val tour: Tour,
)

interface SetlistFmClient {
  fun getSetlistById(setlistFmId: String): Setlist?
}
