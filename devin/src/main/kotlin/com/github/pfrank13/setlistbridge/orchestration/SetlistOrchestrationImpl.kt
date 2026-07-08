package com.github.pfrank13.setlistbridge.orchestration

import com.github.pfrank13.setlistbridge.setlistfm.Setlist
import com.github.pfrank13.setlistbridge.setlistfm.SetlistFmClient
import com.github.pfrank13.setlistbridge.spotify.AddItemsToPlaylistRequest
import com.github.pfrank13.setlistbridge.spotify.CreatePlaylistRequest
import com.github.pfrank13.setlistbridge.spotify.SearchItemType
import com.github.pfrank13.setlistbridge.spotify.SpotifyClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.milliseconds

@Component
class SetlistOrchestrationImpl(
	private val setlistFmClient: SetlistFmClient,
	private val spotifyClient: SpotifyClient,
) : SetlistOrchestration {

	private val log = LoggerFactory.getLogger(SetlistOrchestrationImpl::class.java)

	override fun transferSetlist(setlistFmId: String): SetlistPlaylist {
		val setlist = setlistFmClient.getSetListById(setlistFmId)
		val playlist = spotifyClient.createPlaylist(CreatePlaylistRequest(name = playlistName(setlist)))

		val artist = setlist.artist.name
		val songs = LinkedHashSet<SetlistSong>()

		setlist.set.flatMap { it.song }.forEach { song ->
			val response = spotifyClient.searchForItems(
				q = "$artist ${song.name}",
				type = setOf(SearchItemType.TRACK),
				market = null,
				limit = null,
				offset = null,
				includeExternal = null,
			)

			val track = response.tracks?.items?.firstOrNull()
			if (track == null) {
				log.warn("Could not find song '{}' by '{}' in Spotify", song.name, artist)
				return@forEach
			}

			spotifyClient.addItemsToPlaylist(
				playlist.id,
				AddItemsToPlaylistRequest(uris = listOf(track.uri.toString())),
			)

			songs.add(
				SetlistSong(
					externalSongId = track.id,
					artist = track.artists.firstOrNull()?.name ?: artist,
					name = track.name,
					runtime = track.durationMs.milliseconds,
				),
			)
		}

		return SetlistPlaylist(
			externalPlaylistId = playlist.id,
			songs = songs,
		)
	}

	private fun playlistName(setlist: Setlist): String =
		"${setlist.artist.name} at ${setlist.venue.name} - ${setlist.eventDate}"
}
