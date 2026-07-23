package com.github.pfrank13.setlistbridge.orchestration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "matching")
data class MatchingProperties(
	/**
	 * Minimum [ca.solostudios.fuzzykt.FuzzyKt.ratio] similarity (0.0–1.0) between a setlist song
	 * title and a Spotify track title for the track to be accepted as a match.
	 */
	val ratioThreshold: Double = 0.90,
)
