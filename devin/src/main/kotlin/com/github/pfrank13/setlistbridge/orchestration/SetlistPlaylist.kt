package com.github.pfrank13.setlistbridge.orchestration

import kotlin.time.Duration

/**
 * Orchestration-tier view of a playlist created from a setlist.
 *
 * Intentionally decoupled from any provider (Spotify) response type so the
 * orchestration layer can evolve independently of the underlying clients.
 */
data class SetlistPlaylist(
	val externalPlaylistId: String,
	val songs: Set<SetlistSong>,
)

/**
 * Orchestration-tier view of a single song placed on a [SetlistPlaylist].
 */
data class SetlistSong(
	val externalSongId: String,
	val artist: String,
	val name: String,
	val runtime: Duration,
)
