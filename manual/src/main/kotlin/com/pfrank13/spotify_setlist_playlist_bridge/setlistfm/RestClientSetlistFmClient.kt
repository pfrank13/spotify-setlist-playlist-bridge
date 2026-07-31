package com.pfrank13.spotify_setlist_playlist_bridge.setlistfm

import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

class RestClientSetlistFmClient(private val baseUrl: URI,
  apiKey: String) : SetlistFmClient {
  private val restClient: RestClient = RestClient.builder()
    .baseUrl(baseUrl)
    .defaultHeader("x-api-key", apiKey)
    .build()

  override fun getSetlistById(setlistFmId: String): Setlist? {
    return restClient
      .get()
      .uri("/1.0/setlist/$setlistFmId")
      .retrieve()
      .body<Setlist>()
  }
}