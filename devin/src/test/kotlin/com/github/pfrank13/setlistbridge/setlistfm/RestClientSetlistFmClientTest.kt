package com.github.pfrank13.setlistbridge.setlistfm

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest
@ActiveProfiles("test")
class RestClientSetlistFmClientTest {

	@Autowired
	private lateinit var client: SetlistFmClient

	@Test
	fun `getSetListById returns the deserialized setlist`() {
		val body = readResource("/setlist.json")
		wireMock.stubFor(
			get(urlEqualTo("/1.0/setlist/63de4613"))
				.withHeader("x-api-key", equalTo("test-api-key"))
				.withHeader("Accept", containing("application/json"))
				.willReturn(
					aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(body),
				),
		)

		val setlist = client.getSetListById("63de4613")

		assertEquals("63de4613", setlist.id)
		assertEquals("7be1aaa0", setlist.versionId)
		assertEquals("23-08-1964", setlist.eventDate)
		assertEquals("The Beatles", setlist.artist?.name)
		assertEquals("Hollywood Bowl", setlist.venue?.name)
		assertEquals("Hollywood", setlist.venue?.city?.name)
		assertEquals("US", setlist.venue?.city?.country?.code)
		assertEquals("North American Tour 1964", setlist.tour?.name)
		assertEquals(2, setlist.set.size)
		assertEquals("Twist and Shout", setlist.set[0].song[0].name)
		assertEquals("The Top Notes", setlist.set[0].song[0].cover?.name)
		assertEquals(1, setlist.set[1].encore)
	}

	@Test
	fun `getSetListById wraps API errors in SetlistFmException`() {
		wireMock.stubFor(
			get(urlPathMatching("/1.0/setlist/.*"))
				.willReturn(aResponse().withStatus(404)),
		)

		assertFailsWith<SetlistFmException> { client.getSetListById("does-not-exist") }
	}

	private fun readResource(path: String): String =
		requireNotNull(javaClass.getResource(path)) { "Missing test resource $path" }.readText()

	companion object {
		@JvmStatic
		@RegisterExtension
		val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

		@JvmStatic
		@DynamicPropertySource
		fun registerProperties(registry: DynamicPropertyRegistry) {
			registry.add("setlistfm.base-url") { wireMock.baseUrl() }
		}
	}
}
