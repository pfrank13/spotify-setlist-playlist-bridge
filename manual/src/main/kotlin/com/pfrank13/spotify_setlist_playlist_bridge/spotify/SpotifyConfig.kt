package com.pfrank13.spotify_setlist_playlist_bridge.spotify

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient

@Configuration
class SpotifyConfig {
  @Bean
  fun spotifyRestClient(authorizedClientManager: OAuth2AuthorizedClientManager,
                        @Value("\${spotify.base.url}") spotifyBaseUrl: String): RestClient {
    val interceptor = OAuth2ClientHttpRequestInterceptor(authorizedClientManager)
    interceptor.setClientRegistrationIdResolver { "spotify" }

    return RestClient.builder()
      .baseUrl(spotifyBaseUrl)
      .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .requestInterceptor(interceptor)
      .build()
  }
}