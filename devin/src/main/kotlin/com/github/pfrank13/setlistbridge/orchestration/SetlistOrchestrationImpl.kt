package com.github.pfrank13.setlistbridge.orchestration

import ca.solostudios.fuzzykt.FuzzyKt
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
	private val matchingProperties: MatchingProperties,
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

		val songs = LinkedHashSet<SetlistSong>()

		setlist.sets.set.flatMap { it.song }.forEach { song ->
			val searchArtist = song.cover?.name ?: setlist.artist.name
			val query = "${song.name} $searchArtist"
			log.info("Searching Spotify for song '{}' by '{}' with q '{}'", song.name, searchArtist, query)
			val response = spotifyClient.searchForItems(
				query,
				setOf(SearchItemType.TRACK),
				null,
				matchingProperties.searchLimit,
				0,
				null,
			)

			val tracks = response.tracks?.items.orEmpty()
			if (tracks.isEmpty()) {
				log.warn("Could not find song '{}' by '{}' in Spotify", song.name, searchArtist)
				return@forEach
			}

			val songName = song.name.lowercase()
			val scoredTracks = tracks.map { it to FuzzyKt.ratio(songName, it.name.lowercase()) }
			log.info(
				"Spotify returned {} track(s) for '{}': {}",
				scoredTracks.size,
				query,
				scoredTracks.joinToString(", ") { (candidate, ratio) -> "'${candidate.name}' ($ratio)" },
			)

			val track = scoredTracks.firstOrNull { (_, ratio) -> ratio >= matchingProperties.ratioThreshold }?.first
			if (track == null) {
				val bestMatch = scoredTracks.maxByOrNull { (_, ratio) -> ratio }
				log.warn(
					"Skipping song '{}' by '{}': best match track '{}' scored {} below threshold {}",
					song.name,
					searchArtist,
					bestMatch?.first?.name,
					bestMatch?.second,
					matchingProperties.ratioThreshold,
				)
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
					Artist(trackArtist?.id ?: "", trackArtist?.name ?: searchArtist),
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
