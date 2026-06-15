package com.github.pfrank13.setlistbridge.setlistfm

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "setlistfm")
data class SetlistFmProperties(
	/** Base URL of the setlist.fm REST API. */
	val baseUrl: String,
	/** API key sent as the `x-api-key` header on every request. */
	val apiKey: String,
)
