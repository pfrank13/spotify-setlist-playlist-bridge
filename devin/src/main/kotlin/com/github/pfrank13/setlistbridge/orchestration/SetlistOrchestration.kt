package com.github.pfrank13.setlistbridge.orchestration

/**
 * Orchestrates transferring a setlist.fm setlist into a Spotify playlist.
 */
interface SetlistOrchestration {

	/**
	 * Resumes a migration previously registered via [startSetlistMigration], creates a
	 * Spotify playlist and adds the best match for each song to it.
	 *
	 * @param setlistId the surrogate key returned by [startSetlistMigration] (read from the cookie)
	 * @return a [SetlistPlaylist] describing the created playlist and the songs added to it
	 * @throws IllegalArgumentException if no pending migration is registered under [setlistId]
	 */
	fun transferSetlist(setlistId: String): SetlistPlaylist

	/**
	 * Stores the given setlist.fm id under a freshly generated key so the migration
	 * can be resumed once the Spotify OAuth2 flow completes.
	 *
	 * @param externalSetlistId the setlist.fm id to migrate
	 * @return the generated key the id was stored under
	 */
	fun startSetlistMigration(externalSetlistId: String): String
}
