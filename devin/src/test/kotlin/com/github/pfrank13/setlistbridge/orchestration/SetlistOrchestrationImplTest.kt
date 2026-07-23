package com.github.pfrank13.setlistbridge.orchestration

import com.github.pfrank13.setlistbridge.setlistfm.City
import com.github.pfrank13.setlistbridge.setlistfm.Coords
import com.github.pfrank13.setlistbridge.setlistfm.Country
import com.github.pfrank13.setlistbridge.setlistfm.Setlist
import com.github.pfrank13.setlistbridge.setlistfm.SetlistFmClient
import com.github.pfrank13.setlistbridge.setlistfm.Sets
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
	private lateinit var setlistId: String

	@BeforeEach
	fun setUp() {
		setlistFmClient = mock()
		spotifyClient = mock()
		orchestration = SetlistOrchestrationImpl(setlistFmClient, spotifyClient, MatchingProperties(RATIO_THRESHOLD))
		setlistId = orchestration.startSetlistMigration(SETLIST_ID)
	}

	@Test
	fun `transferSetlist creates a playlist and adds the first track found for each song`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_TWO_SONGS)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(SONG_ONE_QUERY)).thenReturn(SEARCH_RESPONSE_ONE)
		whenever(searchFor(SONG_TWO_QUERY)).thenReturn(SEARCH_RESPONSE_TWO)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))).thenReturn(SNAPSHOT)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_TWO_REQUEST))).thenReturn(SNAPSHOT)

		val result = orchestration.transferSetlist(setlistId)

		assertThat(result.externalPlaylistId).isEqualTo(PLAYLIST_ID)
		assertThat(result.name).isEqualTo(PLAYLIST_NAME)
		assertThat(result.songs).containsExactly(SONG_ONE_RESULT, SONG_TWO_RESULT)

		verify(spotifyClient).addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))
		verify(spotifyClient).addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_TWO_REQUEST))
	}

	@Test
	fun `transferSetlist uses the song name and then the performing artist as the search query`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_ONE_SONG)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(SONG_ONE_QUERY)).thenReturn(SEARCH_RESPONSE_ONE)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))).thenReturn(SNAPSHOT)

		orchestration.transferSetlist(setlistId)

		verify(spotifyClient).searchForItems(
			eq(SONG_ONE_QUERY),
			eq(setOf(SearchItemType.TRACK)),
			isNull(),
			eq(SEARCH_LIMIT),
			eq(0),
			isNull(),
		)
	}

	@Test
	fun `transferSetlist searches for the cover artist when a song is a cover`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_COVER_SONG)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(COVER_SONG_QUERY)).thenReturn(SEARCH_RESPONSE_COVER)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_COVER_REQUEST))).thenReturn(SNAPSHOT)

		val result = orchestration.transferSetlist(setlistId)

		assertThat(result.songs).containsExactly(COVER_SONG_RESULT)
		verify(spotifyClient).searchForItems(
			eq(COVER_SONG_QUERY),
			eq(setOf(SearchItemType.TRACK)),
			isNull(),
			eq(SEARCH_LIMIT),
			eq(0),
			isNull(),
		)
	}

	@Test
	fun `transferSetlist skips tracks whose title does not match the setlist song`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_ONE_SONG)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(SONG_ONE_QUERY)).thenReturn(SEARCH_RESPONSE_MISMATCH)

		val result = orchestration.transferSetlist(setlistId)

		assertThat(result.songs).isEmpty()
		verify(spotifyClient, never()).addItemsToPlaylist(any(), any())
	}

	@Test
	fun `transferSetlist matches song titles case-insensitively`() {
		val song = song("SONG one")
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(setlist(listOf(song)))
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor("SONG one $ARTIST_NAME")).thenReturn(SEARCH_RESPONSE_ONE)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))).thenReturn(SNAPSHOT)

		val result = orchestration.transferSetlist(setlistId)

		assertThat(result.songs).containsExactly(SONG_ONE_RESULT)
		verify(spotifyClient).addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))
	}

	@Test
	fun `transferSetlist accepts tracks whose title is within the fuzzy match threshold`() {
		val song = song("Comfortably Numb")
		val nearMissTrack = track("track-numb", "Comfortably Num", "Track Artist", 300_000)
		val addRequest = AddItemsToPlaylistRequest(listOf("spotify:track:track-numb"))
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(setlist(listOf(song)))
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor("Comfortably Numb $ARTIST_NAME")).thenReturn(searchResponse(nearMissTrack))
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(addRequest))).thenReturn(SNAPSHOT)

		val result = orchestration.transferSetlist(setlistId)

		assertThat(result.songs).containsExactly(
			SetlistSong("track-numb", Artist("artist-track-numb", "Track Artist"), "Comfortably Num", 300_000.milliseconds),
		)
		verify(spotifyClient).addItemsToPlaylist(eq(PLAYLIST_ID), eq(addRequest))
	}

	@Test
	fun `transferSetlist iterates past non-matching results and adds the first track that matches`() {
		val song = song("Song One")
		val mismatch = track("track-x", "A Completely Different Track", "Track Artist 4", 100_000)
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(setlist(listOf(song)))
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(SONG_ONE_QUERY)).thenReturn(searchResponse(mismatch, TRACK_ONE))
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))).thenReturn(SNAPSHOT)

		val result = orchestration.transferSetlist(setlistId)

		assertThat(result.songs).containsExactly(SONG_ONE_RESULT)
		verify(spotifyClient).addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_ONE_REQUEST))
	}

	@Test
	fun `transferSetlist skips a song when none of the returned tracks match`() {
		val song = song("Purple Rain")
		val missOne = track("track-rain", "Purpel Rain", "Track Artist", 300_000)
		val missTwo = track("track-other", "A Completely Different Track", "Track Artist", 200_000)
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(setlist(listOf(song)))
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor("Purple Rain $ARTIST_NAME")).thenReturn(searchResponse(missOne, missTwo))

		val result = orchestration.transferSetlist(setlistId)

		assertThat(result.songs).isEmpty()
		verify(spotifyClient, never()).addItemsToPlaylist(any(), any())
	}

	@Test
	fun `transferSetlist skips tracks whose title falls below the fuzzy match threshold`() {
		val song = song("Purple Rain")
		val belowThresholdTrack = track("track-rain", "Purpel Rain", "Track Artist", 300_000)
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(setlist(listOf(song)))
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor("Purple Rain $ARTIST_NAME")).thenReturn(searchResponse(belowThresholdTrack))

		val result = orchestration.transferSetlist(setlistId)

		assertThat(result.songs).isEmpty()
		verify(spotifyClient, never()).addItemsToPlaylist(any(), any())
	}

	@Test
	fun `transferSetlist skips songs that cannot be found in Spotify`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_MISSING_AND_FOUND)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(MISSING_SONG_QUERY)).thenReturn(EMPTY_SEARCH_RESPONSE)
		whenever(searchFor(SONG_TWO_QUERY)).thenReturn(SEARCH_RESPONSE_TWO)
		whenever(spotifyClient.addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_TWO_REQUEST))).thenReturn(SNAPSHOT)

		val result = orchestration.transferSetlist(setlistId)

		assertThat(result.songs).containsExactly(SONG_TWO_RESULT)
		verify(spotifyClient).addItemsToPlaylist(eq(PLAYLIST_ID), eq(ADD_TRACK_TWO_REQUEST))
		verify(spotifyClient, times(1)).addItemsToPlaylist(any(), any())
	}

	@Test
	fun `transferSetlist creates the playlist even when no songs match`() {
		whenever(setlistFmClient.getSetListById(eq(SETLIST_ID))).thenReturn(SETLIST_MISSING_ONLY)
		whenever(spotifyClient.createPlaylist(eq(CREATE_PLAYLIST_REQUEST))).thenReturn(PLAYLIST)
		whenever(searchFor(MISSING_SONG_QUERY)).thenReturn(EMPTY_SEARCH_RESPONSE)

		val result = orchestration.transferSetlist(setlistId)

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

		orchestration.transferSetlist(setlistId)

		assertThatThrownBy { orchestration.transferSetlist(setlistId) }
			.isInstanceOf(IllegalArgumentException::class.java)
	}

	@Test
	fun `startSetlistMigration returns a fresh key for each call`() {
		val freshOrchestration = SetlistOrchestrationImpl(setlistFmClient, spotifyClient, MatchingProperties(RATIO_THRESHOLD))
		val keyOne = freshOrchestration.startSetlistMigration("setlist-1")
		val keyTwo = freshOrchestration.startSetlistMigration("setlist-1")

		assertThat(keyOne).isNotBlank()
		assertThat(keyTwo).isNotBlank()
		assertThat(keyOne).isNotEqualTo(keyTwo)
	}

	private fun searchFor(query: String): SearchResponse =
		spotifyClient.searchForItems(
			eq(query),
			eq(setOf(SearchItemType.TRACK)),
			isNull(),
			eq(SEARCH_LIMIT),
			eq(0),
			isNull(),
		)

	private companion object {
		const val RATIO_THRESHOLD = 0.90
		const val SEARCH_LIMIT = 5
		const val SETLIST_ID = "setlist-123"
		const val PLAYLIST_ID = "playlist-abc"
		const val ARTIST_NAME = "The Band"
		const val VENUE_NAME = "The Venue"
		const val EVENT_DATE = "10-07-2026"
		const val PLAYLIST_NAME = "$ARTIST_NAME at $VENUE_NAME - $EVENT_DATE"

		const val COVER_ARTIST_NAME = "The Original Artist"
		const val SONG_ONE_QUERY = "Song One $ARTIST_NAME"
		const val SONG_TWO_QUERY = "Song Two $ARTIST_NAME"
		const val MISSING_SONG_QUERY = "Missing Song $ARTIST_NAME"
		const val COVER_SONG_QUERY = "Cover Song $COVER_ARTIST_NAME"

		val SONG_ONE = song("Song One")
		val SONG_TWO = song("Song Two")
		val MISSING_SONG = song("Missing Song")
		val COVER_SONG = Song("Cover Song", null, false, null, coverArtist(COVER_ARTIST_NAME))

		val SETLIST_ONE_SONG = setlist(listOf(SONG_ONE))
		val SETLIST_TWO_SONGS = setlist(listOf(SONG_ONE, SONG_TWO))
		val SETLIST_MISSING_AND_FOUND = setlist(listOf(MISSING_SONG, SONG_TWO))
		val SETLIST_MISSING_ONLY = setlist(listOf(MISSING_SONG))
		val SETLIST_COVER_SONG = setlist(listOf(COVER_SONG))

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
		val COVER_TRACK = track("track-cover", "Cover Song", "Track Artist 3", 200_000)
		val MISMATCH_TRACK = track("track-x", "A Completely Different Track", "Track Artist 4", 100_000)

		val SEARCH_RESPONSE_ONE = searchResponse(TRACK_ONE)
		val SEARCH_RESPONSE_TWO = searchResponse(TRACK_TWO)
		val SEARCH_RESPONSE_COVER = searchResponse(COVER_TRACK)
		val SEARCH_RESPONSE_MISMATCH = searchResponse(MISMATCH_TRACK)
		val EMPTY_SEARCH_RESPONSE = searchResponse()

		val ADD_TRACK_ONE_REQUEST = AddItemsToPlaylistRequest(listOf("spotify:track:track-1"))
		val ADD_TRACK_TWO_REQUEST = AddItemsToPlaylistRequest(listOf("spotify:track:track-2"))
		val ADD_TRACK_COVER_REQUEST = AddItemsToPlaylistRequest(listOf("spotify:track:track-cover"))

		val SONG_ONE_RESULT = SetlistSong("track-1", Artist("artist-track-1", "Track Artist 1"), "Song One", 210_000.milliseconds)
		val SONG_TWO_RESULT = SetlistSong("track-2", Artist("artist-track-2", "Track Artist 2"), "Song Two", 180_000.milliseconds)
		val COVER_SONG_RESULT = SetlistSong("track-cover", Artist("artist-track-cover", "Track Artist 3"), "Cover Song", 200_000.milliseconds)

		private fun song(name: String): Song = Song(name, null, false, null, null)

		private fun coverArtist(name: String): SetlistFmArtist =
			SetlistFmArtist("mbid-cover", name, name, "", "https://www.setlist.fm/artist-cover")

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
			Sets(listOf(SetlistFmSet(null, null, songs))),
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
