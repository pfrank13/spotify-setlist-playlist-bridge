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
				authorize(anyRequest, authenticated)
			}
			csrf {
				ignoringRequestMatchers("/api/setlist/**")
			}
			oauth2Client { }
		}
		return http.build()
	}
}
