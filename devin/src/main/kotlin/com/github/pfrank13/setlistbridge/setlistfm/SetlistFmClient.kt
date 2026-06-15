package com.github.pfrank13.setlistbridge.setlistfm

/**
 * Client for the setlist.fm REST API (https://api.setlist.fm/docs/1.0/index.html).
 */
interface SetlistFmClient {

	/**
	 * Returns the current version of the setlist with the given id.
	 *
	 * @throws SetlistFmException if the request fails or the response cannot be read
	 */
	fun getSetListById(id: String): Setlist

	companion object {
		/** Header setlist.fm requires the API key to be sent in. */
		const val API_KEY_HEADER = "x-api-key"
	}
}
