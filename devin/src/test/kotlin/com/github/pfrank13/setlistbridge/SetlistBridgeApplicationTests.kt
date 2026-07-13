package com.github.pfrank13.setlistbridge

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.RequestOptions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.Base64

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SetlistBridgeApplicationTests {

	@LocalServerPort
	private var port: Int = 0

	private lateinit var page: Page

	@BeforeEach
	fun setUp() {
		page = browser.newPage()
	}

	@AfterEach
	fun tearDown() {
		page.close()
	}

	@Test
	fun contextLoads() {
	}

	@Test
	fun `initiating OAuth2 PKCE flow redirects to authorization endpoint with required params`() {
		wireMock.stubFor(
			get(urlPathEqualTo("/authorize"))
				.willReturn(aResponse().withStatus(200).withBody("mock-authorize-page")),
		)

		page.navigate("http://localhost:$port/oauth2/authorization/spotify")

		val requests = wireMock.findAll(getRequestedFor(urlPathEqualTo("/authorize")))
		assertThat(requests).hasSize(1)

		val authRequest = requests[0]
		val params = authRequest.queryParams

		assertThat(params["response_type"]?.firstValue()).isEqualTo("code")
		assertThat(params["client_id"]?.firstValue()).isEqualTo("test-client-id")
		assertThat(params["redirect_uri"]?.firstValue()).isNotBlank()
		assertThat(params["state"]?.firstValue()).isNotBlank()
		assertThat(params["code_challenge"]?.firstValue()).isNotBlank()
		assertThat(params["code_challenge_method"]?.firstValue()).isEqualTo("S256")

		val scope = params["scope"]?.firstValue()
		assertThat(scope).contains("playlist-modify-public")
		assertThat(scope).contains("playlist-modify-private")
	}

	@Test
	fun `starting a setlist migration sets a cookie and redirects into the OAuth2 flow`() {
		val encoded = Base64.getUrlEncoder().encodeToString("setlist-fm-id".toByteArray())

		val response = page.request().get(
			"http://localhost:$port/api/setlist/$encoded",
			RequestOptions.create().setMaxRedirects(0),
		)

		assertThat(response.status()).isEqualTo(302)
		assertThat(response.headers()["location"]).isEqualTo("/oauth2/authorization/spotify")
		assertThat(response.headers()["set-cookie"]).contains("externalSetlistId=")
	}

	@Test
	fun `starting a setlist migration follows through to the Spotify authorization endpoint`() {
		wireMock.stubFor(
			get(urlPathEqualTo("/authorize"))
				.willReturn(aResponse().withStatus(200).withBody("mock-authorize-page")),
		)

		val encoded = Base64.getUrlEncoder().encodeToString("setlist-fm-id".toByteArray())

		val response = page.request().get("http://localhost:$port/api/setlist/$encoded")

		assertThat(response.ok()).isTrue()
		assertThat(wireMock.findAll(getRequestedFor(urlPathEqualTo("/authorize")))).hasSize(1)
	}

	@Test
	fun `starting a setlist migration returns 400 for an invalid base64 id`() {
		val response = page.request().get(
			"http://localhost:$port/api/setlist/not!valid!base64",
			RequestOptions.create().setMaxRedirects(0),
		)

		assertThat(response.status()).isEqualTo(400)
	}

	companion object {
		private lateinit var playwright: Playwright
		private lateinit var browser: Browser

		@JvmStatic
		@RegisterExtension
		val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

		@JvmStatic
		@DynamicPropertySource
		fun overrideProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.security.oauth2.client.provider.spotify.authorization-uri") {
				"${wireMock.baseUrl()}/authorize"
			}
		}

		@JvmStatic
		@BeforeAll
		fun startPlaywright() {
			playwright = Playwright.create()
			browser = playwright.chromium().launch()
		}

		@JvmStatic
		@AfterAll
		fun stopPlaywright() {
			browser.close()
			playwright.close()
		}
	}
}
