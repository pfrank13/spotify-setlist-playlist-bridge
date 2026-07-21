package com.github.pfrank13.setlistbridge.orchestration

import com.github.pfrank13.setlistbridge.setlistfm.Setlist
import com.github.pfrank13.setlistbridge.setlistfm.SetlistFmClient
import com.github.pfrank13.setlistbridge.spotify.AddItemsToPlaylistRequest
import com.github.pfrank13.setlistbridge.spotify.CreatePlaylistRequest
import com.github.pfrank13.setlistbridge.spotify.SearchItemType
import com.github.pfrank13.setlistbridge.spotify.SpotifyClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@Component
class SetlistOrchestrationImpl(
	private val setlistFmClient: SetlistFmClient,
	private val spotifyClient: SpotifyClient,
) : SetlistOrchestration {

	private val log = LoggerFactory.getLogger(SetlistOrchestrationImpl::class.java)

	private val pendingMigrations = ConcurrentHashMap<String, String>()

	override fun startSetlistMigration(externalSetlistId: String): String {
		val key = UUID.randomUUID().toString()
		pendingMigrations[key] = externalSetlistId
		return key
	}

	override fun transferSetlist(setlistId: String): SetlistPlaylist {
		val setlistFmId = pendingMigrations.remove(setlistId)
			?: throw IllegalArgumentException("No pending migration for setlistId '$setlistId'")

		// TODO: should getSetListById return a nullable Setlist so a missing setlist can be handled explicitly?
		val setlist = setlistFmClient.getSetListById(setlistFmId)
		val playlist = spotifyClient.createPlaylist(CreatePlaylistRequest(playlistName(setlist)))

		val artistName = setlist.artist.name
		val songs = LinkedHashSet<SetlistSong>()

		setlist.sets.set.flatMap { it.song }.forEach { song ->
			val response = spotifyClient.searchForItems(
				"$artistName ${song.name}",
				setOf(SearchItemType.TRACK),
				null,
				1,
				0,
				null,
			)

			val track = response.tracks?.items?.firstOrNull()
			if (track == null) {
				log.warn("Could not find song '{}' by '{}' in Spotify", song.name, artistName)
				return@forEach
			}

			// TODO: adds a single track per request; could be optimized to batch all uris into one addItemsToPlaylist call.
			val snapshot = spotifyClient.addItemsToPlaylist(
				playlist.id,
				AddItemsToPlaylistRequest(listOf(track.uri.toString())),
			)
			log.info(
				"Added song '{}' to playlist '{}' (new snapshot id '{}')",
				track.name,
				playlist.id,
				snapshot.snapshotId,
			)

			val trackArtist = track.artists.firstOrNull()
			songs.add(
				SetlistSong(
					track.id,
					Artist(trackArtist?.id ?: "", trackArtist?.name ?: artistName),
					track.name,
					track.durationMs.milliseconds,
				),
			)
		}

		return SetlistPlaylist(playlist.id, playlist.name, songs)
	}

	private fun playlistName(setlist: Setlist): String =
		"${setlist.artist.name} at ${setlist.venue.name} - ${setlist.eventDate}"
}
