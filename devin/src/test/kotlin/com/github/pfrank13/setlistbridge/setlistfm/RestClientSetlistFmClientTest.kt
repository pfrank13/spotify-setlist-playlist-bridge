package com.github.pfrank13.setlistbridge.setlistfm

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

class RestClientSetlistFmClientTest {

	private lateinit var client: SetlistFmClient

	@BeforeEach
	fun setUp() {
		val properties = SetlistFmProperties(baseUrl = wireMock.baseUrl(), apiKey = API_KEY)
		client = RestClientSetlistFmClient(SetlistFmConfig().setlistFmRestClient(properties))
	}

	@Test
	fun `getSetListById returns the deserialized setlist`() {
		wireMock.stubFor(
			get(urlEqualTo("/1.0/setlist/$SETLIST_ID"))
				.withHeader(SetlistFmClient.API_KEY_HEADER, equalTo(API_KEY))
				.withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
				.willReturn(
					aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(readResource("/setlist.json")),
				),
		)

		val setlist = client.getSetListById(SETLIST_ID)

		assertThat(setlist).isEqualTo(EXPECTED_SETLIST)
	}

	@Test
	fun `getSetListById wraps API errors in SetlistFmException`() {
		wireMock.stubFor(
			get(urlPathMatching("/1.0/setlist/.*"))
				.willReturn(aResponse().withStatus(404)),
		)

		assertThatThrownBy { client.getSetListById("does-not-exist") }
			.isInstanceOf(SetlistFmException::class.java)
	}

	private fun readResource(path: String): String =
		requireNotNull(javaClass.getResource(path)) { "Missing test resource $path" }.readText()

	companion object {
		private const val API_KEY = "test-api-key"
		private const val SETLIST_ID = "63de4613"

		@JvmStatic
		@RegisterExtension
		val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

		private val EXPECTED_SETLIST = Setlist(
			id = SETLIST_ID,
			versionId = "7be1aaa0",
			eventDate = "23-08-1964",
			lastUpdated = "2013-10-20T05:18:08.000+0000",
			artist = Artist(
				mbid = "b10bbbfc-cf9e-42e0-be17-e2c3e1d2600d",
				name = "The Beatles",
				sortName = "Beatles, The",
				disambiguation = "John, Paul, George and Ringo",
				url = "https://www.setlist.fm/setlists/the-beatles-23d6a88b.html",
			),
			venue = Venue(
				id = "6bd6ca6e",
				name = "Hollywood Bowl",
				url = "https://www.setlist.fm/venue/hollywood-bowl-hollywood-ca-usa-6bd6ca6e.html",
				city = City(
					id = "5357527",
					name = "Hollywood",
					state = "California",
					stateCode = "CA",
					coords = Coords(lat = 34.0983425, long = -118.3267434),
					country = Country(code = "US", name = "United States"),
				),
			),
			tour = Tour(name = "North American Tour 1964"),
			set = listOf(
				Set(
					name = null,
					encore = null,
					song = listOf(
						Song(
							name = "Twist and Shout",
							info = null,
							tape = false,
							with = null,
							cover = Artist(
								mbid = "f1eb7e69-2c1e-4b39-8de3-7c1a4f6f1234",
								name = "The Top Notes",
								sortName = "Top Notes, The",
								disambiguation = "",
								url = "https://www.setlist.fm/setlists/the-top-notes-13d6e7f1.html",
							),
						),
						Song(name = "You Can't Do That", info = null, tape = false, with = null, cover = null),
					),
				),
				Set(
					name = null,
					encore = 1,
					song = listOf(
						Song(name = "Long Tall Sally", info = "Last song of the night", tape = false, with = null, cover = null),
					),
				),
			),
			info = "Recorded and published as 'The Beatles at the Hollywood Bowl'",
			url = "https://www.setlist.fm/setlist/the-beatles/1964/hollywood-bowl-hollywood-ca-63de4613.html",
		)
	}
}
