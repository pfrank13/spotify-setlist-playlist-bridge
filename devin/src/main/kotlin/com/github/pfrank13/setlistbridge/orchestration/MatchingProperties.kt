package com.github.pfrank13.setlistbridge.orchestration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "matching")
data class MatchingProperties(
	/**
	 * Minimum [ca.solostudios.fuzzykt.FuzzyKt.ratio] similarity (0.0–1.0) between a setlist song
	 * title and a Spotify track title for the track to be accepted as a match.
	 */
	val ratioThreshold: Double = 0.60,
	/**
	 * Maximum number of Spotify search results to request per song (Spotify allows 0–10). The
	 * returned tracks are iterated in order and the first one matching [ratioThreshold] is used.
	 */
	val searchLimit: Int = 5,
)
