package com.github.pfrank13.setlistbridge

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
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
