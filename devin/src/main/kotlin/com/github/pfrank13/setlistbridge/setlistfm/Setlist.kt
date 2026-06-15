package com.github.pfrank13.setlistbridge.setlistfm

/**
 * Response model for the setlist.fm `setlist` data type.
 *
 * See https://api.setlist.fm/docs/1.0/json_Setlist.html
 */
data class Setlist(
	val id: String? = null,
	val versionId: String? = null,
	val eventDate: String? = null,
	val lastUpdated: String? = null,
	val artist: Artist? = null,
	val venue: Venue? = null,
	val tour: Tour? = null,
	val set: List<Set> = emptyList(),
	val info: String? = null,
	val url: String? = null,
)

data class Artist(
	val mbid: String? = null,
	val name: String? = null,
	val sortName: String? = null,
	val disambiguation: String? = null,
	val url: String? = null,
)

data class Venue(
	val id: String? = null,
	val name: String? = null,
	val url: String? = null,
	val city: City? = null,
)

data class City(
	val id: String? = null,
	val name: String? = null,
	val state: String? = null,
	val stateCode: String? = null,
	val coords: Coords? = null,
	val country: Country? = null,
)

data class Coords(
	val lat: Double? = null,
	val long: Double? = null,
)

data class Country(
	val code: String? = null,
	val name: String? = null,
)

data class Tour(
	val name: String? = null,
)

data class Set(
	val name: String? = null,
	val encore: Int? = null,
	val song: List<Song> = emptyList(),
)

data class Song(
	val name: String? = null,
	val info: String? = null,
	val tape: Boolean? = null,
	val with: Artist? = null,
	val cover: Artist? = null,
)
