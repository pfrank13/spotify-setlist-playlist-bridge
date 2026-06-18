package com.github.pfrank13.setlistbridge.spotify

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

/**
 * Base class for Spotify item types returned by the Search API.
 *
 * Contains attributes common across all item types.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class Item(
	open val id: String,
	open val name: String,
	open val href: String,
	open val uri: String,
	open val type: String,
	@JsonProperty("external_urls")
	open val externalUrls: Map<String, URI>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TrackItem(
	override val id: String,
	override val name: String,
	override val href: String,
	override val uri: String,
	override val type: String,
	@JsonProperty("external_urls")
	override val externalUrls: Map<String, URI>,
	@JsonProperty("disc_number")
	val discNumber: Int,
	@JsonProperty("duration_ms")
	val durationMs: Int,
	val explicit: Boolean,
	@JsonProperty("external_ids")
	val externalIds: Map<String, String>?,
	@JsonProperty("is_playable")
	val isPlayable: Boolean?,
	@JsonProperty("track_number")
	val trackNumber: Int,
	@JsonProperty("is_local")
	val isLocal: Boolean,
	val album: SimplifiedAlbum?,
	val artists: List<SimplifiedArtist>,
) : Item(id, name, href, uri, type, externalUrls)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimplifiedAlbum(
	val id: String,
	val name: String,
	val href: String,
	val uri: String,
	@JsonProperty("album_type")
	val albumType: String,
	@JsonProperty("total_tracks")
	val totalTracks: Int,
	@JsonProperty("external_urls")
	val externalUrls: Map<String, URI>,
	@JsonProperty("release_date")
	val releaseDate: String,
	@JsonProperty("release_date_precision")
	val releaseDatePrecision: String,
	val images: List<SpotifyImage>,
	val artists: List<SimplifiedArtist>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimplifiedArtist(
	val id: String,
	val name: String,
	val href: String?,
	val uri: String,
	@JsonProperty("external_urls")
	val externalUrls: Map<String, URI>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpotifyImage(
	val url: String,
	val height: Int?,
	val width: Int?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AlbumItem(
	override val id: String,
	override val name: String,
	override val href: String,
	override val uri: String,
	override val type: String,
	@JsonProperty("external_urls")
	override val externalUrls: Map<String, URI>,
) : Item(id, name, href, uri, type, externalUrls)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArtistItem(
	override val id: String,
	override val name: String,
	override val href: String,
	override val uri: String,
	override val type: String,
	@JsonProperty("external_urls")
	override val externalUrls: Map<String, URI>,
) : Item(id, name, href, uri, type, externalUrls)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlaylistItem(
	override val id: String,
	override val name: String,
	override val href: String,
	override val uri: String,
	override val type: String,
	@JsonProperty("external_urls")
	override val externalUrls: Map<String, URI>,
) : Item(id, name, href, uri, type, externalUrls)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ShowItem(
	override val id: String,
	override val name: String,
	override val href: String,
	override val uri: String,
	override val type: String,
	@JsonProperty("external_urls")
	override val externalUrls: Map<String, URI>,
) : Item(id, name, href, uri, type, externalUrls)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodeItem(
	override val id: String,
	override val name: String,
	override val href: String,
	override val uri: String,
	override val type: String,
	@JsonProperty("external_urls")
	override val externalUrls: Map<String, URI>,
) : Item(id, name, href, uri, type, externalUrls)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AudiobookItem(
	override val id: String,
	override val name: String,
	override val href: String,
	override val uri: String,
	override val type: String,
	@JsonProperty("external_urls")
	override val externalUrls: Map<String, URI>,
) : Item(id, name, href, uri, type, externalUrls)
