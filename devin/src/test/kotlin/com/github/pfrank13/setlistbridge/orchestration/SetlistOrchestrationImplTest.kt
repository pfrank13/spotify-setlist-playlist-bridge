package com.github.pfrank13.setlistbridge.orchestration

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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds
import com.github.pfrank13.setlistbridge.setlistfm.Artist as SetlistFmArtist
import com.github.pfrank13.setlistbridge.setlistfm.Set as SetlistFmSet

class SetlistOrchestrationImplTest {

	private lateinit var setlistFmClient: SetlistFmClient
	private lateinit var spotifyClient: SpotifyClient
	private lateinit var orchestration: SetlistOrchestrationImpl

	@BeforeEach
	fun setUp() {
		setlistFmClient = mock()
		spotifyClient = mock()
		orchestration = SetlistOrchestrationImpl(setlistFmClient, spotifyClient)
	}

	@Test
	fun `transferSetlist creates a playlist and adds the first track found for each song`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_TWO_SONGS)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(SONG_ONE_QUERY)).thenReturn(SEARCH_RESPONSE_ONE)
		whenever(searchFor(SONG_TWO_QUERY)).thenReturn(SEARCH_RESPONSE_TWO)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))).thenReturn(SNAPSHOT)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_TWO_REQUEST))).thenReturn(SNAPSHOT)

		val result = orchestration.transferSetlist(orchestration.startSetlistMigration(SETLIST_ID))

		assertThat(result.externalPlaylistId).isEqualTo(PLAYLIST_ID)
		assertThat(result.name).isEqualTo(PLAYLIST_NAME)
		assertThat(result.songs).containsExactly(SONG_ONE_RESULT, SONG_TWO_RESULT)

		verify(spotifyClient).addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))
		verify(spotifyClient).addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_TWO_REQUEST))
	}

	@Test
	fun `transferSetlist uses the performing artist and the song name as the search query`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_ONE_SONG)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(SONG_ONE_QUERY)).thenReturn(SEARCH_RESPONSE_ONE)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))).thenReturn(SNAPSHOT)

		orchestration.transferSetlist(orchestration.startSetlistMigration(SETLIST_ID))

		verify(spotifyClient).searchForItems(
			eq(SONG_ONE_QUERY),
			eq(setOf(SearchItemType.TRACK)),
			isNull(),
			eq(1),
			eq(0),
			isNull(),
		)
	}

	@Test
	fun `transferSetlist skips songs that cannot be found in Spotify`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_MISSING_AND_FOUND)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(MISSING_SONG_QUERY)).thenReturn(EMPTY_SEARCH_RESPONSE)
		whenever(searchFor(SONG_TWO_QUERY)).thenReturn(SEARCH_RESPONSE_TWO)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_TWO_REQUEST))).thenReturn(SNAPSHOT)

		val result = orchestration.transferSetlist(orchestration.startSetlistMigration(SETLIST_ID))

		assertThat(result.songs).containsExactly(SONG_TWO_RESULT)
		verify(spotifyClient).addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_TWO_REQUEST))
		verify(spotifyClient, times(1)).addItemsToPlaylist(any(), any())
	}

	@Test
	fun `transferSetlist creates the playlist even when no songs match`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_MISSING_ONLY)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(MISSING_SONG_QUERY)).thenReturn(EMPTY_SEARCH_RESPONSE)

		val result = orchestration.transferSetlist(orchestration.startSetlistMigration(SETLIST_ID))

		assertThat(result.externalPlaylistId).isEqualTo(PLAYLIST_ID)
		assertThat(result.songs).isEmpty()
		verify(spotifyClient).createPlaylist(eq(CREATE_PLAYLIST_REQUEST))
		verify(spotifyClient, never()).addItemsToPlaylist(any(), any())
	}

	@Test
	fun `transferSetlist throws when no pending migration exists for the setlistId`() {
		assertThatThrownBy { orchestration.transferSetlist("unknown-key") }
			.isInstanceOf(IllegalArgumentException::class.java)

		verify(spotifyClient, never()).createPlaylist(any())
	}

	@Test
	fun `transferSetlist consumes the pending migration so it cannot be replayed`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_ONE_SONG)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(SONG_ONE_QUERY)).thenReturn(SEARCH_RESPONSE_ONE)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))).thenReturn(SNAPSHOT)

		val key = orchestration.startSetlistMigration(SETLIST_ID)
		orchestration.transferSetlist(key)

		assertThatThrownBy { orchestration.transferSetlist(key) }
			.isInstanceOf(IllegalArgumentException::class.java)
	}

	@Test
	fun `startSetlistMigration returns a fresh key for each call`() {
		val keyOne = orchestration.startSetlistMigration("setlist-1")
		val keyTwo = orchestration.startSetlistMigration("setlist-1")

		assertThat(keyOne).isNotBlank()
		assertThat(keyTwo).isNotBlank()
		assertThat(keyOne).isNotEqualTo(keyTwo)
	}

	private fun searchFor(query: String): SearchResponse =
		spotifyClient.searchForItems(
			eq(query),
			eq(setOf(SearchItemType.TRACK)),
			isNull(),
			eq(1),
			eq(0),
			isNull(),
		)

	private companion object {
		const val SETLIST_ID = "setlist-123"
		const val PLAYLIST_ID = "playlist-abc"
		const val ARTIST_NAME = "The Band"
		const val VENUE_NAME = "The Venue"
		const val EVENT_DATE = "10-07-2026"
		const val PLAYLIST_NAME = "$ARTIST_NAME at $VENUE_NAME - $EVENT_DATE"

		const val SONG_ONE_QUERY = "$ARTIST_NAME Song One"
		const val SONG_TWO_QUERY = "$ARTIST_NAME Song Two"
		const val MISSING_SONG_QUERY = "$ARTIST_NAME Missing Song"

		val SONG_ONE = song("Song One")
		val SONG_TWO = song("Song Two")
		val MISSING_SONG = song("Missing Song")

		val SETLIST_ONE_SONG = setlist(listOf(SONG_ONE))
		val SETLIST_TWO_SONGS = setlist(listOf(SONG_ONE, SONG_TWO))
		val SETLIST_MISSING_AND_FOUND = setlist(listOf(MISSING_SONG, SONG_TWO))
		val SETLIST_MISSING_ONLY = setlist(listOf(MISSING_SONG))

		val CREATE_PLAYLIST_REQUEST = CreatePlaylistRequest(PLAYLIST_NAME)
		val PLAYLIST = Playlist(
			PLAYLIST_ID,
			PLAYLIST_NAME,
			null,
			true,
			emptyMap(),
			"https://api.spotify.com/v1/playlists/$PLAYLIST_ID",
			"spotify:playlist:$PLAYLIST_ID",
			"snapshot",
		)
		val SNAPSHOT = SnapshotResponse("snap")

		val TRACK_ONE = track("track-1", "Song One", "Track Artist 1", 210_000)
		val TRACK_TWO = track("track-2", "Song Two", "Track Artist 2", 180_000)

		val SEARCH_RESPONSE_ONE = searchResponse(TRACK_ONE)
		val SEARCH_RESPONSE_TWO = searchResponse(TRACK_TWO)
		val EMPTY_SEARCH_RESPONSE = searchResponse()

		val ADD_TRACK_ONE_REQUEST = AddItemsToPlaylistRequest(listOf("spotify:track:track-1"))
		val ADD_TRACK_TWO_REQUEST = AddItemsToPlaylistRequest(listOf("spotify:track:track-2"))

		val SONG_ONE_RESULT = SetlistSong("track-1", Artist("artist-track-1", "Track Artist 1"), "Song One", 210_000.milliseconds)
		val SONG_TWO_RESULT = SetlistSong("track-2", Artist("artist-track-2", "Track Artist 2"), "Song Two", 180_000.milliseconds)

		private fun song(name: String): Song = Song(name, null, false, null, null)

		private fun setlist(songs: List<Song>): Setlist = Setlist(
			SETLIST_ID,
			"v1",
			EVENT_DATE,
			"2026-07-08T00:00:00.000+0000",
			SetlistFmArtist("mbid", ARTIST_NAME, ARTIST_NAME, "", "https://www.setlist.fm/artist"),
			Venue(
				"venue-id",
				VENUE_NAME,
				"https://www.setlist.fm/venue",
				City("city-id", "City", "State", "ST", Coords(null, null), Country("US", "United States")),
			),
			Tour("The Tour"),
			listOf(SetlistFmSet(null, null, songs)),
			"",
			"https://www.setlist.fm/setlist",
		)

		private fun searchResponse(vararg tracks: TrackItem): SearchResponse = SearchResponse(
			PaginatedResult(
				"https://api.spotify.com/v1/search",
				1,
				null,
				0,
				null,
				tracks.size,
				tracks.toList(),
			),
			null,
			null,
			null,
			null,
			null,
			null,
		)

		private fun track(id: String, name: String, artistName: String, durationMs: Int): TrackItem = TrackItem(
			id,
			name,
			URI.create("https://api.spotify.com/v1/tracks/$id"),
			URI.create("spotify:track:$id"),
			SearchItemType.TRACK,
			emptyMap(),
			1,
			durationMs,
			false,
			null,
			true,
			1,
			false,
			null,
			listOf(
				SimplifiedArtist(
					"artist-$id",
					artistName,
					null,
					URI.create("spotify:artist:$id"),
					emptyMap(),
				),
			),
		)
	}
}
