package com.github.pfrank13.setlistbridge.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http {
			authorizeHttpRequests {
				authorize("/api/setlist/**", permitAll)
				authorize("/callback", permitAll)
				authorize("/favicon.ico", permitAll)
				authorize("/error", permitAll)
				authorize(anyRequest, authenticated)
			}
			oauth2Client { }
		}
		return http.build()
	}
}
