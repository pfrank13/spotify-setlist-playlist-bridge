package com.github.pfrank13.setlistbridge.orchestration

/**
 * Orchestrates transferring a setlist.fm setlist into a Spotify playlist.
 */
interface SetlistOrchestration {

	/**
	 * Fetches the setlist with the given setlist.fm id, creates a Spotify playlist
	 * and adds the best match for each song to it.
	 *
	 * @param setlistFmId the setlist.fm id of the setlist to transfer
	 * @return a [SetlistPlaylist] describing the created playlist and the songs added to it
	 */
	fun transferSetlist(setlistFmId: String): SetlistPlaylist
}
