package com.github.pfrank13.setlistbridge.spotify

/**
 * Thrown when a call to the Spotify Web API fails.
 */
class SpotifyException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
