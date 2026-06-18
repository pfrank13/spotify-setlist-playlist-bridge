package com.github.pfrank13.setlistbridge.spotify

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.time.Instant

class RestClientSpotifyClientTest {

	private lateinit var client: SpotifyClient

	@BeforeEach
	fun setUp() {
		val registration = ClientRegistration.withRegistrationId("spotify")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
			.clientId(CLIENT_ID)
			.authorizationUri("${wireMock.baseUrl()}/authorize")
			.tokenUri("${wireMock.baseUrl()}/api/token")
			.redirectUri("http://localhost:8080/callback")
			.build()

		val accessToken = OAuth2AccessToken(
			OAuth2AccessToken.TokenType.BEARER,
			ACCESS_TOKEN,
			Instant.now(),
			Instant.now().plusSeconds(3600),
		)

		val authorizedClient = OAuth2AuthorizedClient(registration, PRINCIPAL, accessToken)

		val regRepo = InMemoryClientRegistrationRepository(registration)
		val clientService = InMemoryOAuth2AuthorizedClientService(regRepo)
		val auth = TestingAuthenticationToken(PRINCIPAL, "")
		clientService.saveAuthorizedClient(authorizedClient, auth)

		val manager = AuthorizedClientServiceOAuth2AuthorizedClientManager(regRepo, clientService)

		val interceptor = OAuth2ClientHttpRequestInterceptor(manager)
		interceptor.setClientRegistrationIdResolver { "spotify" }

		SecurityContextHolder.getContext().authentication = auth

		val restClient = RestClient.builder()
			.baseUrl(wireMock.baseUrl())
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.requestInterceptor(interceptor)
			.build()

		client = RestClientSpotifyClient(restClient)
	}

	@AfterEach
	fun tearDown() {
		SecurityContextHolder.clearContext()
	}

	@Test
	fun `createPlaylist sends Bearer token and returns the deserialized playlist`() {
		val responseJson = OBJECT_MAPPER.writeValueAsString(EXPECTED_PLAYLIST)

		wireMock.stubFor(
			post(urlEqualTo(SpotifyClient.CREATE_PLAYLIST_URI))
				.withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $ACCESS_TOKEN"))
				.withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
				.withRequestBody(
					equalToJson(
						"""{"name":"My Setlist","description":"Concert prep","public":true}""",
					),
				)
				.willReturn(
					aResponse()
						.withStatus(201)
						.withHeader("Content-Type", "application/json")
						.withBody(responseJson),
				),
		)

		val playlist = client.createPlaylist(
			CreatePlaylistRequest(name = "My Setlist", description = "Concert prep", isPublic = true),
		)

		assertThat(playlist).isEqualTo(EXPECTED_PLAYLIST)
	}

	@Test
	fun `createPlaylist wraps API errors in SpotifyException`() {
		wireMock.stubFor(
			post(urlPathMatching(SpotifyClient.CREATE_PLAYLIST_URI))
				.willReturn(aResponse().withStatus(403)),
		)

		assertThatThrownBy { client.createPlaylist(CreatePlaylistRequest(name = "Forbidden")) }
			.isInstanceOf(SpotifyException::class.java)
	}

	@Test
	fun `addItemsToPlaylist sends Bearer token and returns the deserialized snapshot response`() {
		val responseJson = OBJECT_MAPPER.writeValueAsString(EXPECTED_SNAPSHOT_RESPONSE)

		wireMock.stubFor(
			post(urlEqualTo("/v1/playlists/$PLAYLIST_ID/items"))
				.withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $ACCESS_TOKEN"))
				.withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
				.withRequestBody(
					equalToJson(
						"""{"uris":["spotify:track:4iV5W9uYEdYUVa79Axb7Rh","spotify:track:1301WleyT98MSxVHPZCA6M"],"position":0}""",
					),
				)
				.willReturn(
					aResponse()
						.withStatus(201)
						.withHeader("Content-Type", "application/json")
						.withBody(responseJson),
				),
		)

		val snapshot = client.addItemsToPlaylist(
			playlistId = PLAYLIST_ID,
			request = AddItemsToPlaylistRequest(
				uris = listOf("spotify:track:4iV5W9uYEdYUVa79Axb7Rh", "spotify:track:1301WleyT98MSxVHPZCA6M"),
				position = 0,
			),
		)

		assertThat(snapshot).isEqualTo(EXPECTED_SNAPSHOT_RESPONSE)
	}

	@Test
	fun `addItemsToPlaylist wraps API errors in SpotifyException`() {
		wireMock.stubFor(
			post(urlPathMatching("/v1/playlists/.+/items"))
				.willReturn(aResponse().withStatus(403)),
		)

		assertThatThrownBy {
			client.addItemsToPlaylist(
				playlistId = PLAYLIST_ID,
				request = AddItemsToPlaylistRequest(uris = listOf("spotify:track:4iV5W9uYEdYUVa79Axb7Rh")),
			)
		}
			.isInstanceOf(SpotifyException::class.java)
	}

	@Test
	fun `searchForItems sends Bearer token and returns deserialized search response`() {
		wireMock.stubFor(
			get(urlPathEqualTo(SpotifyClient.SEARCH_URI))
				.withQueryParam(SpotifyClient.QUERY_PARAM, equalTo("Doxy Miles Davis"))
				.withQueryParam(SpotifyClient.TYPE_PARAM, equalTo("track"))
				.withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $ACCESS_TOKEN"))
				.withHeader(HttpHeaders.ACCEPT, containing(MediaType.APPLICATION_JSON_VALUE))
				.willReturn(
					aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(SEARCH_RESPONSE_JSON),
				),
		)

		val response = client.searchForItems(
			q = "Doxy Miles Davis",
			type = setOf(SearchItemType.TRACK),
			market = null,
			limit = null,
			offset = null,
			includeExternal = null,
		)

		assertThat(response).isEqualTo(EXPECTED_SEARCH_RESPONSE)
	}

	@Test
	fun `searchForItems sends optional query parameters`() {
		wireMock.stubFor(
			get(urlPathEqualTo(SpotifyClient.SEARCH_URI))
				.withQueryParam(SpotifyClient.QUERY_PARAM, equalTo("test"))
				.withQueryParam(SpotifyClient.TYPE_PARAM, equalTo("track,album"))
				.withQueryParam(SpotifyClient.MARKET_PARAM, equalTo("US"))
				.withQueryParam(SpotifyClient.LIMIT_PARAM, equalTo("10"))
				.withQueryParam(SpotifyClient.OFFSET_PARAM, equalTo("5"))
				.withQueryParam(SpotifyClient.INCLUDE_EXTERNAL_PARAM, equalTo("audio"))
				.willReturn(
					aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(SEARCH_RESPONSE_JSON),
				),
		)

		val response = client.searchForItems(
			q = "test",
			type = setOf(SearchItemType.TRACK, SearchItemType.ALBUM),
			market = "US",
			limit = 10,
			offset = 5,
			includeExternal = "audio",
		)

		assertThat(response).isEqualTo(EXPECTED_SEARCH_RESPONSE)
	}

	@Test
	fun `searchForItems wraps API errors in SpotifyException`() {
		wireMock.stubFor(
			get(urlPathEqualTo(SpotifyClient.SEARCH_URI))
				.willReturn(aResponse().withStatus(401)),
		)

		assertThatThrownBy {
			client.searchForItems(
				q = "test",
				type = setOf(SearchItemType.TRACK),
				market = null,
				limit = null,
				offset = null,
				includeExternal = null,
			)
		}
			.isInstanceOf(SpotifyException::class.java)
	}

	companion object {
		private const val CLIENT_ID = "test-client-id"
		private const val ACCESS_TOKEN = "test-access-token"
		private const val PRINCIPAL = "test-principal"
		private const val PLAYLIST_ID = "3cEYpjA9oz9GiPac4AsH4n"

		private val OBJECT_MAPPER = jacksonObjectMapper()

		@JvmStatic
		@RegisterExtension
		val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

		private val EXPECTED_SEARCH_RESPONSE = SearchResponse(
			tracks = PaginatedResult(
				href = "https://api.spotify.com/v1/search?query=Doxy+Miles+Davis&type=track&offset=0&limit=5",
				limit = 5,
				next = null,
				offset = 0,
				previous = null,
				total = 1,
				items = listOf(
					TrackItem(
						id = "4iV5W9uYEdYUVa79Axb7Rh",
						name = "Doxy",
						href = URI("https://api.spotify.com/v1/tracks/4iV5W9uYEdYUVa79Axb7Rh"),
						uri = URI("spotify:track:4iV5W9uYEdYUVa79Axb7Rh"),
						type = SearchItemType.TRACK,
						externalUrls = mapOf("spotify" to URI("https://open.spotify.com/track/4iV5W9uYEdYUVa79Axb7Rh")),
						discNumber = 1,
						durationMs = 324000,
						explicit = false,
						externalIds = mapOf("isrc" to "USPR35507295"),
						isPlayable = true,
						trackNumber = 5,
						isLocal = false,
						album = SimplifiedAlbum(
							id = "2cRMVS71c49Pf5SnIlJX3U",
							name = "Bags' Groove",
							href = URI("https://api.spotify.com/v1/albums/2cRMVS71c49Pf5SnIlJX3U"),
							uri = URI("spotify:album:2cRMVS71c49Pf5SnIlJX3U"),
							albumType = "album",
							totalTracks = 5,
							externalUrls = mapOf("spotify" to URI("https://open.spotify.com/album/2cRMVS71c49Pf5SnIlJX3U")),
							releaseDate = "1957-01-01",
							releaseDatePrecision = "day",
							images = listOf(
								SpotifyImage(
									url = URI("https://i.scdn.co/image/ab67616d0000b273example"),
									height = 640,
									width = 640,
								),
							),
							artists = listOf(
								SimplifiedArtist(
									id = "0kbYTNQb4Pb1rY2MnLRbKj",
									name = "Miles Davis",
									href = URI("https://api.spotify.com/v1/artists/0kbYTNQb4Pb1rY2MnLRbKj"),
									uri = URI("spotify:artist:0kbYTNQb4Pb1rY2MnLRbKj"),
									externalUrls = mapOf("spotify" to URI("https://open.spotify.com/artist/0kbYTNQb4Pb1rY2MnLRbKj")),
								),
							),
						),
						artists = listOf(
							SimplifiedArtist(
								id = "0kbYTNQb4Pb1rY2MnLRbKj",
								name = "Miles Davis",
								href = URI("https://api.spotify.com/v1/artists/0kbYTNQb4Pb1rY2MnLRbKj"),
								uri = URI("spotify:artist:0kbYTNQb4Pb1rY2MnLRbKj"),
								externalUrls = mapOf("spotify" to URI("https://open.spotify.com/artist/0kbYTNQb4Pb1rY2MnLRbKj")),
							),
						),
					),
				),
			),
			albums = null,
			artists = null,
			playlists = null,
			shows = null,
			episodes = null,
			audiobooks = null,
		)

		private val SEARCH_RESPONSE_JSON = OBJECT_MAPPER.writeValueAsString(EXPECTED_SEARCH_RESPONSE)

		private val EXPECTED_SNAPSHOT_RESPONSE = SnapshotResponse(
			snapshotId = "JbtmHBDBAYu3/bt8BOXKjzKx3i0b6LCa/wVjyl6qQ2Yf6nFXkbmzuFMs",
		)

		private val EXPECTED_PLAYLIST = Playlist(
			id = "3cEYpjA9oz9GiPac4AsH4n",
			name = "My Setlist",
			description = "Concert prep",
			isPublic = true,
			externalUrls = mapOf("spotify" to URI("https://open.spotify.com/playlist/3cEYpjA9oz9GiPac4AsH4n")),
			href = "https://api.spotify.com/v1/playlists/3cEYpjA9oz9GiPac4AsH4n",
			uri = "spotify:playlist:3cEYpjA9oz9GiPac4AsH4n",
			snapshotId = "MSw0MjI5NzMzNTQ2LDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAw",
		)
	}
}
