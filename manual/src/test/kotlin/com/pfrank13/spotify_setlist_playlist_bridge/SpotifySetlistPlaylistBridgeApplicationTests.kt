package com.pfrank13.spotify_setlist_playlist_bridge

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Playwright
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpotifySetlistPlaylistBridgeApplicationTests {
	companion object {
		private lateinit var playwright: Playwright
		private lateinit var browser: Browser

		@JvmStatic
		@RegisterExtension
		val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

		@JvmStatic
		@BeforeAll
		fun setupClass() {
			playwright = Playwright.create()
			browser = playwright.chromium().launch()
		}

		@JvmStatic
		@DynamicPropertySource
		fun dynamicProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.security.oauth2.client.provider.spotify.authorization-uri") {
				"${wireMock.baseUrl()}/authorize"
			}
		}
	}

	@LocalServerPort
	private var port: Int = 0

	@Test
	fun contextLoads() {
	}

	@Test
	fun oauthIngressVerification() {
		//GIVEN
		wireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/authorize")).willReturn(aResponse().withStatus(200)))
		val page = browser.newPage()

		//WHEN
		page.navigate("http://localhost:$port/oauth2/authorization/spotify")

		//THEN
		val requests = wireMock.findAll(getRequestedFor(urlPathEqualTo("/authorize")))
		assertThat(requests).hasSize(1)

		val authRequest = requests[0]
		val params = authRequest.queryParams

		assertThat(params["response_type"]?.firstValue()).isEqualTo("code")
		assertThat(params["client_id"]?.firstValue()).isEqualTo("myClientId")
		assertThat(params["redirect_uri"]?.firstValue()).isNotBlank()
		assertThat(params["state"]?.firstValue()).isNotBlank()
		assertThat(params["code_challenge"]?.firstValue()).isNotBlank()
		assertThat(params["code_challenge_method"]?.firstValue()).isEqualTo("S256")

		val scope = params["scope"]?.firstValue()
		assertThat(scope).contains("playlist-modify-public")
		assertThat(scope).contains("playlist-modify-private")
	}
}
