package com.github.pfrank13.setlistbridge.spotify

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(SpotifyProperties::class)
class SpotifyConfig {

	@Bean
	fun spotifyRestClient(
		authorizedClientManager: OAuth2AuthorizedClientManager,
		properties: SpotifyProperties,
	): RestClient {
		val interceptor = OAuth2ClientHttpRequestInterceptor(authorizedClientManager)
		interceptor.setClientRegistrationIdResolver { "spotify" }

		return RestClient.builder()
			.baseUrl(properties.baseUrl)
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.requestInterceptor(interceptor)
			.build()
	}
}
