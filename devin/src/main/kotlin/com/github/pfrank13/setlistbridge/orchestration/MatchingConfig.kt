package com.github.pfrank13.setlistbridge.orchestration

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MatchingProperties::class)
class MatchingConfig
