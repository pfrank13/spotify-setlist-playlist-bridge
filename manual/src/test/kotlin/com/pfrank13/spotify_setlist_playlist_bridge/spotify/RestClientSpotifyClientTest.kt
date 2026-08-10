package com.pfrank13.spotify_setlist_playlist_bridge.spotify

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
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
import java.net.URI

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
          matchingJsonPath("$.name", WireMock.equalTo(name))
        )
        .willReturn(
          WireMock.okJson(
            resolveResource("/createPlaylistResponse.json")
          )
        )
    )

    //WHEN
    val createdPlaylist = restClientSpotifyClient.createPlaylist(createPlaylistRequest)

    //THEN
    Assertions.assertThat(createdPlaylist).usingRecursiveComparison().isEqualTo(expectedPlaylistResponse)
  }

  @Test
  fun addItemsToPlaylist() {
    //GIVEN
    val playlistId = "myPlaylistId"
    val expectedItemUri = "http://spotify.com/someId"
    val expectedSnapshotId = "mySnapshotId"
    val addItemsToPlaylistRequest = AddItemsToPlaylistRequest(listOf(URI(expectedItemUri)))

    wireMock.stubFor(
      WireMock.post(urlEqualTo("/v1/playlists/$playlistId/items"))
        .withHeader(HttpHeaders.AUTHORIZATION, WireMock.equalTo("Bearer $bearerToken"))
        .withRequestBody(
          matchingJsonPath("$.uris[0]", WireMock.equalTo(expectedItemUri)),
        )
        .willReturn(
          WireMock.okJson(
            resolveResource("/addItemsToPlaylistResponse.json")
          )
        )
    )

    //WHEN
    val mutatedSnapshotId = restClientSpotifyClient.addItemsToPlaylist(playlistId, addItemsToPlaylistRequest)

    //THEN
    Assertions.assertThat(mutatedSnapshotId.snapshotId).isEqualTo(expectedSnapshotId)
  }

  @Test
  fun searchForItems() {
    //GIVEN
    val q = "Query"
    val type = ItemType.TRACK
    val limit = 6
    val searchForItemsRequest = SearchForItemsRequest(q, type, limit)

    wireMock.stubFor(
      WireMock.get(WireMock.urlPathEqualTo("/v1/search"))
        .withQueryParam(RestClientSpotifyClient.Companion.SearchForItemsParams.Q, WireMock.equalTo(q))
        .withQueryParam(RestClientSpotifyClient.Companion.SearchForItemsParams.TYPE, WireMock.equalTo(type.value))
        .withQueryParam(RestClientSpotifyClient.Companion.SearchForItemsParams.LIMIT, WireMock.equalTo(limit.toString()))
        .withHeader(HttpHeaders.AUTHORIZATION, WireMock.equalTo("Bearer $bearerToken"))
        .willReturn(
          WireMock.okJson(
            resolveResource("/searchForItemsResponse.json")
          )
        )
    )

    //WHEN
    val searchForItemsResponse = restClientSpotifyClient.searchForItems(searchForItemsRequest)

    //THEN
    Assertions.assertThat(searchForItemsResponse.tracks.items).size().isEqualTo(1)
    val track = searchForItemsResponse.tracks.items[0]
    Assertions.assertThat(searchForItemsResponse.tracks.next).isEqualTo(URI("https://api.spotify.com/v1/me/shows?offset=1&limit=1"))
    Assertions.assertThat(searchForItemsResponse.tracks.previous).isEqualTo(URI("https://api.spotify.com/v1/me/shows?offset=1&limit=1"))
    Assertions.assertThat(searchForItemsResponse.tracks.offset).isEqualTo(0)
    Assertions.assertThat(searchForItemsResponse.tracks.total).isEqualTo(searchForItemsResponse.tracks.total)

    Assertions.assertThat(track.id).isEqualTo("myId")
    Assertions.assertThat(track.name).isEqualTo("My Track Name")
    Assertions.assertThat(track.uri).isEqualTo(URI("https://api.spotify.com/someUri"))
  }

  private fun resolveResource(classpathToResource: String): String {
    return Companion::class.java.getResourceAsStream(classpathToResource)!!.readAllBytes()
      .toString(Charsets.UTF_8)
  }
}
