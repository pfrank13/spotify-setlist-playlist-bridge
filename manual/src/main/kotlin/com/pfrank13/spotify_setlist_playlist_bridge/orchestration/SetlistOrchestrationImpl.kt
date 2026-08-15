package com.pfrank13.spotify_setlist_playlist_bridge.orchestration

import com.pfrank13.spotify_setlist_playlist_bridge.setlistfm.Setlist
import com.pfrank13.spotify_setlist_playlist_bridge.setlistfm.SetlistFmClient
import com.pfrank13.spotify_setlist_playlist_bridge.setlistfm.Song
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.AddItemsToPlaylistRequest
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.CreatePlaylistRequest
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.ItemType
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.SearchForItemsRequest
import com.pfrank13.spotify_setlist_playlist_bridge.spotify.SpotifyClient
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64

class SetlistOrchestrationImpl(private val setlistFmClient: SetlistFmClient,
  private val spotifyClient: SpotifyClient,
  private val searchItemLimit: Int) : SetlistOrchestration {

  companion object {
    private val LOG = LoggerFactory.getLogger(SetlistOrchestrationImpl::class.java)
  }

  fun determineSetlistName(setList: Setlist): String {
    return "${setList.artist.name} ${setList.eventDate}"
  }

  private fun determineQ(setList: Setlist, song: Song): String {
    return "${setList.artist.name} (${song.name})"
  }

  override fun transferSetlist(externalSetlistId: String): Playlist {
    val setlistFmSetlistId = String(Base64.UrlSafe.decode(externalSetlistId.toByteArray(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
    //Get the setlist from the SetlistFmClient
    val setlistSetlist = setlistFmClient.getSetlistById(setlistFmSetlistId)
    if(setlistSetlist != null) {
      //Create a playlist from the SpotifyClient if a setlist is found
      val setlistName = determineSetlistName(setlistSetlist)
      val createPlaylistResponse = spotifyClient.createPlaylist(CreatePlaylistRequest(setlistName))
      val songsToReturn = mutableSetOf<com.pfrank13.spotify_setlist_playlist_bridge.orchestration.Song>()
      val playlistToReturn = Playlist(externalSetlistId, setlistName, songsToReturn)
      val spotifyPlaylistId = createPlaylistResponse.id

      //Iterate through the songs in the Setlist and find each track via the SpotifyClient.searchForItems and just chose the first TrackItem returned (the assumption is we will refine this over time). We should use both the artist and the song name in our search criteria as a song name by itself may yield many different Artists.
      LOG.info("Beginning to process ${setlistSetlist.set.size} sets in setlistId=$setlistFmSetlistId")
      for((name, song) in setlistSetlist.set) {
        LOG.info("Adding $name to spotify playlist $spotifyPlaylistId")
        LOG.info("Beginning to process ${song.size} songs in setlistId=$setlistFmSetlistId")
        for(song in song) {
          val q = determineQ(setlistSetlist, song)
          val searchForItemsRequest = SearchForItemsRequest(q,
            ItemType.TRACK, searchItemLimit)
          val foundTracks = spotifyClient.searchForItems(searchForItemsRequest)
          LOG.info("Found ${foundTracks.tracks.total} tracks for q=$q")

          //With that first TrackItem we will add it to the playlist we created before, specifically using SpotifyClient.addItemsToPlaylist
          val firstTrack = foundTracks.tracks.items.firstOrNull()
          if(firstTrack != null) {
            LOG.info("Adding ${firstTrack.name} to spotify playlist $spotifyPlaylistId for setlistFm song name ${song.name}")
            spotifyClient.addItemsToPlaylist(spotifyPlaylistId, AddItemsToPlaylistRequest(listOf(firstTrack.uri)))
            songsToReturn.add(Song(firstTrack.id, Artist(firstTrack.artists.first().id, setlistSetlist.artist.name), firstTrack.name))
          } else {
            //In Song from setlist.fm is not found we should just log out a warning that it could not be found in Spotify and just move on.
            LOG.warn("No tracks found for setlistFm song name =${song.name}")
          }

          return playlistToReturn
        }
      }
    }
    LOG.warn("SetlistFm setlistId=$setlistFmSetlistId not found, moving skipping")
    throw IllegalArgumentException("SetlistFm setlistId=$setlistFmSetlistId is not found")
  }
}