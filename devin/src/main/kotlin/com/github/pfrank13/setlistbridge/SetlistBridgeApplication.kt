package com.github.pfrank13.setlistbridge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
class SetlistBridgeApplication

fun main(args: Array<String>) {
	runApplication<SetlistBridgeApplication>(*args)
}
