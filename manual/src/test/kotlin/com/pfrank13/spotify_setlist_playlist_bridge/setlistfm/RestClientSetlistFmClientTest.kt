package com.pfrank13.spotify_setlist_playlist_bridge.setlistfm

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI

@WireMockTest
class RestClientSetlistFmClientTest {
  companion object {
    val testSetlistId = "63de4613"
    val testApiKey = "myApiKey"
    val artist = Artist("The Beatles", URI("https://www.setlist.fm/setlists/the-beatles-23d6a88b.html"))
    lateinit var restClientSetlistFmClient: RestClientSetlistFmClient
    val expectedSetList: Setlist = Setlist(
      testSetlistId,
      "23-08-1964",
      URI("https://www.setlist.fm/setlist/the-beatles/1964/hollywood-bowl-hollywood-ca-63de4613.html"),
      listOf(Set("...", listOf(Song("Yesterday"))), Set("noNEncore", listOf(Song("withAndCover", artist, artist)))),
      artist,
      Venue(
        "6bd6ca6e",
        "Compaq Center",
        URI("https://www.setlist.fm/venue/compaq-center-san-jose-ca-usa-6bd6ca6e.html"),
        City("Hollywood", "CA")
      ),
      Tour("North American Tour 1964")
    )

    @JvmStatic
    @RegisterExtension
    val wireMock: WireMockExtension = WireMockExtension.newInstance().build()

    @BeforeAll
    @JvmStatic
    fun setUpClass() {
      restClientSetlistFmClient = RestClientSetlistFmClient(URI(wireMock.baseUrl()), testApiKey)
    }
  }

  @Test
  fun getSetlistById() {
    //GIVEN
    wireMock
      .stubFor(
        get(urlPathEqualTo("/1.0/setlist/$testSetlistId"))
          .withHeader("x-api-key", WireMock.equalTo(testApiKey))
          .willReturn(
            WireMock.okJson(
              Companion::class.java.getResourceAsStream("/setlist.json")!!.readAllBytes()
                .toString(Charsets.UTF_8)
            )
          )
      )

    //WHEN
    val setList = restClientSetlistFmClient.getSetlistById(testSetlistId)

    //THEN
    Assertions.assertThat(setList).isNotNull
    if (setList != null) {
      Assertions.assertThat(setList).usingRecursiveComparison().isEqualTo(expectedSetList)
    }
  }
}
