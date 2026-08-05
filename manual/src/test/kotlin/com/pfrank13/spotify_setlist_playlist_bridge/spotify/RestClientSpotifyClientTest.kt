package com.pfrank13.spotify_setlist_playlist_bridge.spotify

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken

class RestClientSpotifyClientTest {
  companion object {
    private const val bearerToken = "myBearerToken"
    private lateinit var restClientSpotifyClient: RestClientSpotifyClient

    @JvmStatic
    @RegisterExtension
    val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

    @BeforeAll
    @JvmStatic
    fun setUpClass() {
      val spotifyConfig = SpotifyConfig()

      val clientRegistration = ClientRegistration.withRegistrationId("spotify")
        .clientId("clientId")
        .redirectUri("http://localhost:8080")
        .authorizationUri("http://localhost")
        .tokenUri("http://localhost:8080")
        .authorizationGrantType(AuthorizationGrantType("authorization_code")).build()
      val accessToken = OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, bearerToken, null, null)
      val authorizedOauthClient = OAuth2AuthorizedClient(clientRegistration, "spotify", accessToken)

      val authorizedClientManager = Mockito.mock(OAuth2AuthorizedClientManager::class.java)
      whenever(authorizedClientManager.authorize(Mockito.any())).thenReturn(authorizedOauthClient)

      val restClient = spotifyConfig.spotifyRestClient(authorizedClientManager, wireMock.baseUrl())

      restClientSpotifyClient = RestClientSpotifyClient(restClient)
    }
  }

  //@BeforeEach
  fun setUp() {
    TODO("Not yet implemented")
  }

  //@AfterEach
  fun tearDown() {
    TODO("Not yet implemented")
  }

  @Test
  fun createPlaylist() {
    //GIVEN
    val name = "myPlaylist"
    val createPlaylistRequest = CreatePlaylistRequest(name)
    val expectedPlaylistResponse = CreatePlaylistResponse("someId", "someSnapshotName", createPlaylistRequest.name, true, false, null)
    wireMock.stubFor(
      WireMock.post(urlEqualTo(RestClientSpotifyClient.CREATE_PLAYLIST_URI))
        .withHeader(HttpHeaders.AUTHORIZATION, WireMock.equalTo("Bearer $bearerToken"))
        .withRequestBody(
          matchingJsonPath("$.name")
        )
        .willReturn(
          WireMock.okJson(
            Companion::class.java.getResourceAsStream("/createPlaylistResponse.json")!!.readAllBytes()
              .toString(Charsets.UTF_8)
          )
        )
    )

    //WHEN
    val createdPlaylist = restClientSpotifyClient.createPlaylist(createPlaylistRequest)

    //THEN
    Assertions.assertThat(createdPlaylist).usingRecursiveComparison().isEqualTo(expectedPlaylistResponse)
  }
}
