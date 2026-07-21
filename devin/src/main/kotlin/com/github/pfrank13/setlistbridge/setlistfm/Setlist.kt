package com.github.pfrank13.setlistbridge.setlistfm

/**
 * Response model for the setlist.fm `setlist` data type.
 *
 * Modelled after the shape the API returns in practice; fields that are absent for
 * some setlists are nullable.
 * See https://api.setlist.fm/docs/1.0/json_Setlist.html
 */
data class Setlist(
	val id: String,
	val versionId: String,
	val eventDate: String,
	val lastUpdated: String,
	val artist: Artist,
	val venue: Venue,
	val tour: Tour?,
	val sets: Sets,
	val info: String?,
	val url: String,
)

data class Sets(
	val set: List<SetFm>,
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
	val lat: Double?,
	val long: Double?,
)

data class Country(
	val code: String?,
	val name: String?,
)

data class Tour(
	val name: String,
)

data class SetFm(
	val name: String?,
	val encore: Int?,
	val song: List<Song>,
)

data class Song(
	val name: String,
	val info: String?,
	val tape: Boolean?,
	val with: Artist?,
	val cover: Artist?,
)
