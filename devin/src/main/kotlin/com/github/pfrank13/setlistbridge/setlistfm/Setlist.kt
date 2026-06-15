package com.github.pfrank13.setlistbridge.setlistfm

/**
 * Response model for the setlist.fm `setlist` data type.
 *
 * Modelled after the documented example, treating fields shown there as required.
 * See https://api.setlist.fm/docs/1.0/json_Setlist.html
 */
data class Setlist(
	val id: String,
	val versionId: String,
	val eventDate: String,
	val lastUpdated: String,
	val artist: Artist,
	val venue: Venue,
	val tour: Tour,
	val set: List<Set>,
	val info: String,
	val url: String,
)

data class Artist(
	val mbid: String,
	val name: String,
	val sortName: String,
	val disambiguation: String,
	val url: String,
)

data class Venue(
	val id: String,
	val name: String,
	val url: String,
	val city: City,
)

data class City(
	val id: String,
	val name: String,
	val state: String,
	val stateCode: String,
	val coords: Coords,
	val country: Country,
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
	val name: String,
)

data class Set(
	val name: String? = null,
	val encore: Int? = null,
	val song: List<Song>,
)

data class Song(
	val name: String,
	val info: String? = null,
	val tape: Boolean = false,
	val with: Artist? = null,
	val cover: Artist? = null,
)
