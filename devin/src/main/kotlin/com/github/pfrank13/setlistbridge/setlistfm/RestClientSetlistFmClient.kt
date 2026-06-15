package com.github.pfrank13.setlistbridge.setlistfm

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class RestClientSetlistFmClient(
	private val setlistFmRestClient: RestClient,
) : SetlistFmClient {

	override fun getSetListById(id: String): Setlist {
		try {
			return setlistFmRestClient.get()
				.uri("/1.0/setlist/{setlistId}", id)
				.retrieve()
				.body(Setlist::class.java)
				?: throw SetlistFmException("Empty response body for setlist id '$id'")
		} catch (ex: RestClientException) {
			throw SetlistFmException("Failed to fetch setlist with id '$id'", ex)
		}
	}
}
