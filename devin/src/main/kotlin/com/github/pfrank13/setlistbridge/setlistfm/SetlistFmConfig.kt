package com.github.pfrank13.setlistbridge.setlistfm

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(SetlistFmProperties::class)
class SetlistFmConfig {

	@Bean
	fun setlistFmRestClient(properties: SetlistFmProperties): RestClient =
		RestClient.builder()
			.baseUrl(properties.baseUrl)
			.defaultHeader("x-api-key", properties.apiKey)
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.build()
}
