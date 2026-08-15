package com.pfrank13.spotify_setlist_playlist_bridge.orchestration

import com.pfrank13.spotify_setlist_playlist_bridge.setlistfm.RestClientSetlistFmClientTest.Companion
import com.pfrank13.spotify_setlist_playlist_bridge.setlistfm.Setlist
import com.pfrank13.spotify_setlist_playlist_bridge.setlistfm.SetlistFmClient
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.ArtistItem
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.CreatePlaylistRequest
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.CreatePlaylistResponse
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.ItemType
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.SearchForItemsRequest
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.SearchForItemsResponse
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.SpotifyClient
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.TrackItem
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.Tracks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64

class SetlistOrchestrationImplTest {
  companion object {
    private val objectMapper = jacksonObjectMapper()
  }
  private lateinit var setlistFmClient: SetlistFmClient
  private lateinit var spotifyClient: SpotifyClient
  private lateinit var setlistOrchestration: SetlistOrchestrationImpl

  @BeforeEach
  fun setUp() {
    setlistFmClient = mock()
    spotifyClient = mock()
    setlistOrchestration = SetlistOrchestrationImpl(setlistFmClient, spotifyClient, 1)
  }

  @Test
  fun testSetlistOrchestration() {
    //GIVEN
    val setlistFmSetlistId = "124"
    val externalSetlistId = Base64.UrlSafe.encode(setlistFmSetlistId.toByteArray(StandardCharsets.UTF_8))
    val setlistFmSetList = objectMapper.readValue(getResourceAsString("/setlist.json"), Setlist::class.java)
    whenever(setlistFmClient.getSetlistById(setlistFmSetlistId)).thenReturn(setlistFmSetList)

    val spotifyPlaylistId = "spotifyPlaylistId"
    whenever(spotifyClient.createPlaylist(eq(CreatePlaylistRequest("The Beatles 23-08-1964")))).thenReturn(
      CreatePlaylistResponse(
        spotifyPlaylistId,
        "snapshotId",
        "someName",
        true,
        true,
        null
      )
    )
    val artistItem = ArtistItem("spotifyArtistId", "artistName", URI.create("http://localhost/artist"))
    val trackItem = TrackItem("spotifyTrackId", "songName", URI.create("http://localhost/track"), listOf(artistItem))
    whenever(spotifyClient.searchForItems(eq(SearchForItemsRequest("The Beatles (Yesterday)", ItemType.TRACK, 1)))).thenReturn(
      SearchForItemsResponse(Tracks(1, 0, URI.create("http://localhost/next"), URI.create("http://localhost/previous"), 1,
        listOf(trackItem)),))

    //WHEN
    val playlist = setlistOrchestration.transferSetlist(externalSetlistId)

    //THEN
    Assertions.assertThat(playlist).isNotNull
    Assertions.assertThat(playlist.externalPlaylistId).isEqualTo("MTI0")
    Assertions.assertThat(playlist.name).isEqualTo("The Beatles 23-08-1964")
    val songs = playlist.songs
    Assertions.assertThat(songs).hasSize(1)
    val song = playlist.songs.first()
    Assertions.assertThat(song.externalSongId).isEqualTo(trackItem.id)
    Assertions.assertThat(song.name).isEqualTo("songName")
    val artist = song.artist
    Assertions.assertThat(artist.externalArtistId).isEqualTo(artistItem.id)
    Assertions.assertThat(artist.name).isEqualTo("The Beatles")
  }

  @Test
  fun notFound() {
    //GIVEN
    val setlistFmSetlistId = "124"
    val externalSetlistId = Base64.UrlSafe.encode(setlistFmSetlistId.toByteArray(StandardCharsets.UTF_8))
    whenever(setlistFmClient.getSetlistById(setlistFmSetlistId)).thenReturn(null)

    //WHEN
    Assertions.assertThatThrownBy {
      setlistOrchestration.transferSetlist(externalSetlistId)
    }.hasMessage("SetlistFm setlistId=$setlistFmSetlistId is not found")
      .isInstanceOf(IllegalArgumentException::class.java)
  }

  private fun getResourceAsString(path: String): String {
    return Companion::class.java.getResourceAsStream(path)!!.readAllBytes()
      .toString(Charsets.UTF_8)
  }
}