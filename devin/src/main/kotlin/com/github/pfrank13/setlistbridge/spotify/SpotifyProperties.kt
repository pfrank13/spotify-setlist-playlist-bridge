package com.github.pfrank13.setlistbridge.spotify

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spotify")
data class SpotifyProperties(
	/** Base URL of the Spotify Web API. */
	val baseUrl: String,
)
