package com.github.pfrank13.setlistbridge.orchestration

import com.github.pfrank13.setlistbridge.setlistfm.Artist
import com.github.pfrank13.setlistbridge.setlistfm.City
import com.github.pfrank13.setlistbridge.setlistfm.Coords
import com.github.pfrank13.setlistbridge.setlistfm.Country
import com.github.pfrank13.setlistbridge.setlistfm.Setlist
import com.github.pfrank13.setlistbridge.setlistfm.SetlistFmClient
import com.github.pfrank13.setlistbridge.setlistfm.Song
import com.github.pfrank13.setlistbridge.setlistfm.Tour
import com.github.pfrank13.setlistbridge.setlistfm.Venue
import com.github.pfrank13.setlistbridge.spotify.AddItemsToPlaylistRequest
import com.github.pfrank13.setlistbridge.spotify.CreatePlaylistRequest
import com.github.pfrank13.setlistbridge.spotify.PaginatedResult
import com.github.pfrank13.setlistbridge.spotify.Playlist
import com.github.pfrank13.setlistbridge.spotify.SearchItemType
import com.github.pfrank13.setlistbridge.spotify.SearchResponse
import com.github.pfrank13.setlistbridge.spotify.SimplifiedArtist
import com.github.pfrank13.setlistbridge.spotify.SnapshotResponse
import com.github.pfrank13.setlistbridge.spotify.SpotifyClient
import com.github.pfrank13.setlistbridge.spotify.TrackItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

class SetlistOrchestrationImplTest {

	private val setlistFmClient = mock<SetlistFmClient>()
	private val spotifyClient = mock<SpotifyClient>()
	private val orchestration = SetlistOrchestrationImpl(setlistFmClient, spotifyClient)

	@Test
	fun `transferSetlist creates a playlist and adds the first track found for each song`() {
		whenever(setlistFmClient.getSetListById(SETLIST_ID))
			.thenReturn(setlist(songNames = listOf("Song One", "Song Two")))
		whenever(spotifyClient.createPlaylist(any())).thenReturn(playlist(PLAYLIST_ID))
		whenever(spotifyClient.searchForItems(eq("$ARTIST_NAME Song One"), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(searchResponse(trackItem("track-1", "Song One", "Track Artist 1", 210_000)))
		whenever(spotifyClient.searchForItems(eq("$ARTIST_NAME Song Two"), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(searchResponse(trackItem("track-2", "Song Two", "Track Artist 2", 180_000)))
		whenever(spotifyClient.addItemsToPlaylist(any(), any())).thenReturn(SnapshotResponse("snap"))

		val result = orchestration.transferSetlist(SETLIST_ID)

		assertThat(result.externalPlaylistId).isEqualTo(PLAYLIST_ID)
		assertThat(result.songs).containsExactly(
			SetlistSong("track-1", "Track Artist 1", "Song One", 210_000.milliseconds),
			SetlistSong("track-2", "Track Artist 2", "Song Two", 180_000.milliseconds),
		)

		verify(spotifyClient).addItemsToPlaylist(
			eq(PLAYLIST_ID),
			eq(AddItemsToPlaylistRequest(uris = listOf("spotify:track:track-1"))),
		)
		verify(spotifyClient).addItemsToPlaylist(
			eq(PLAYLIST_ID),
			eq(AddItemsToPlaylistRequest(uris = listOf("spotify:track:track-2"))),
		)
	}

	@Test
	fun `transferSetlist uses the performing artist and the song name as the search query`() {
		whenever(setlistFmClient.getSetListById(SETLIST_ID))
			.thenReturn(setlist(songNames = listOf("Song One")))
		whenever(spotifyClient.createPlaylist(any())).thenReturn(playlist(PLAYLIST_ID))
		whenever(spotifyClient.searchForItems(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(searchResponse(trackItem("track-1", "Song One", "Track Artist 1", 210_000)))
		whenever(spotifyClient.addItemsToPlaylist(any(), any())).thenReturn(SnapshotResponse("snap"))

		orchestration.transferSetlist(SETLIST_ID)

		verify(spotifyClient).searchForItems(
			eq("$ARTIST_NAME Song One"),
			eq(setOf(SearchItemType.TRACK)),
			anyOrNull(),
			anyOrNull(),
			anyOrNull(),
			anyOrNull(),
		)
	}

	@Test
	fun `transferSetlist skips songs that cannot be found in Spotify`() {
		whenever(setlistFmClient.getSetListById(SETLIST_ID))
			.thenReturn(setlist(songNames = listOf("Missing Song", "Song Two")))
		whenever(spotifyClient.createPlaylist(any())).thenReturn(playlist(PLAYLIST_ID))
		whenever(spotifyClient.searchForItems(eq("$ARTIST_NAME Missing Song"), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(searchResponse())
		whenever(spotifyClient.searchForItems(eq("$ARTIST_NAME Song Two"), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(searchResponse(trackItem("track-2", "Song Two", "Track Artist 2", 180_000)))
		whenever(spotifyClient.addItemsToPlaylist(any(), any())).thenReturn(SnapshotResponse("snap"))

		val result = orchestration.transferSetlist(SETLIST_ID)

		assertThat(result.songs).containsExactly(
			SetlistSong("track-2", "Track Artist 2", "Song Two", 180_000.milliseconds),
		)
		verify(spotifyClient, times(1)).addItemsToPlaylist(any(), any())
	}

	@Test
	fun `transferSetlist creates the playlist even when no songs match`() {
		whenever(setlistFmClient.getSetListById(SETLIST_ID))
			.thenReturn(setlist(songNames = listOf("Missing Song")))
		whenever(spotifyClient.createPlaylist(any())).thenReturn(playlist(PLAYLIST_ID))
		whenever(spotifyClient.searchForItems(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
			.thenReturn(searchResponse())

		val result = orchestration.transferSetlist(SETLIST_ID)

		assertThat(result.externalPlaylistId).isEqualTo(PLAYLIST_ID)
		assertThat(result.songs).isEmpty()
		verify(spotifyClient).createPlaylist(any())
		verify(spotifyClient, never()).addItemsToPlaylist(any(), any())
	}

	private fun setlist(songNames: List<String>): Setlist =
		Setlist(
			id = SETLIST_ID,
			versionId = "v1",
			eventDate = "10-07-2026",
			lastUpdated = "2026-07-08T00:00:00.000+0000",
			artist = Artist(
				mbid = "mbid",
				name = ARTIST_NAME,
				sortName = ARTIST_NAME,
				disambiguation = "",
				url = "https://www.setlist.fm/artist",
			),
			venue = Venue(
				id = "venue-id",
				name = "The Venue",
				url = "https://www.setlist.fm/venue",
				city = City(
					id = "city-id",
					name = "City",
					state = "State",
					stateCode = "ST",
					coords = Coords(lat = null, long = null),
					country = Country(code = "US", name = "United States"),
				),
			),
			tour = Tour(name = "The Tour"),
			set = listOf(
				com.github.pfrank13.setlistbridge.setlistfm.Set(
					name = null,
					encore = null,
					song = songNames.map { Song(name = it, info = null, tape = false, with = null, cover = null) },
				),
			),
			info = "",
			url = "https://www.setlist.fm/setlist",
		)

	private fun playlist(id: String): Playlist =
		Playlist(
			id = id,
			name = "playlist",
			description = null,
			isPublic = true,
			externalUrls = emptyMap(),
			href = "https://api.spotify.com/v1/playlists/$id",
			uri = "spotify:playlist:$id",
			snapshotId = "snapshot",
		)

	private fun searchResponse(vararg tracks: TrackItem): SearchResponse =
		SearchResponse(
			tracks = PaginatedResult(
				href = "https://api.spotify.com/v1/search",
				limit = 5,
				next = null,
				offset = 0,
				previous = null,
				total = tracks.size,
				items = tracks.toList(),
			),
			albums = null,
			artists = null,
			playlists = null,
			shows = null,
			episodes = null,
			audiobooks = null,
		)

	private fun trackItem(id: String, name: String, artistName: String, durationMs: Int): TrackItem =
		TrackItem(
			id = id,
			name = name,
			href = URI.create("https://api.spotify.com/v1/tracks/$id"),
			uri = URI.create("spotify:track:$id"),
			type = SearchItemType.TRACK,
			externalUrls = emptyMap(),
			discNumber = 1,
			durationMs = durationMs,
			explicit = false,
			externalIds = null,
			isPlayable = true,
			trackNumber = 1,
			isLocal = false,
			album = null,
			artists = listOf(
				SimplifiedArtist(
					id = "artist-$id",
					name = artistName,
					href = null,
					uri = URI.create("spotify:artist:$id"),
					externalUrls = emptyMap(),
				),
			),
		)

	private companion object {
		const val SETLIST_ID = "setlist-123"
		const val PLAYLIST_ID = "playlist-abc"
		const val ARTIST_NAME = "The Band"
	}
}
