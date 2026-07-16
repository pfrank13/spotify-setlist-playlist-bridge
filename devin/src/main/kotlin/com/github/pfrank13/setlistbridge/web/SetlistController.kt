package com.github.pfrank13.setlistbridge.web

import com.github.pfrank13.setlistbridge.orchestration.SetlistOrchestration
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.security.Principal
import java.util.Base64

data class Dummy(val value: String)

@RestController
class SetlistController(
	private val setlistOrchestration: SetlistOrchestration,
) {

	companion object {
		private val LOG = LoggerFactory.getLogger(SetlistController::class.java)
	}

	/**
	 * Ingress point that starts a setlist migration.
	 *
	 * Decodes the base64 url encoded setlist.fm id, hands it to the orchestration tier
	 * for safe keeping, stores the returned key in a cookie and redirects into the
	 * Spotify OAuth2 flow so the migration can complete once the user has authorized.
	 */
	@GetMapping("/api/setlist/{externalSetlistId}")
	fun startSetlistMigration(@PathVariable externalSetlistId: String): ResponseEntity<Void> {
		val decoded = String(Base64.getUrlDecoder().decode(externalSetlistId), Charsets.UTF_8)
		val key = setlistOrchestration.startSetlistMigration(decoded)

		val cookie = ResponseCookie.from("externalSetlistId", key)
			.path("/")
			.httpOnly(true)
			.build()

		return ResponseEntity.status(HttpStatus.FOUND)
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.location(URI.create("/oauth2/authorization/spotify"))
			.build()
	}

	@GetMapping("/callback")
	fun getCallback(): ResponseEntity<Dummy> {
		LOG.info("################## Get SetlistCallback #################")
		return ResponseEntity.ok(Dummy("SetlistCallback Called Fine"))
	}

	@ExceptionHandler(IllegalArgumentException::class)
	fun handleInvalidExternalSetlistId(): ResponseEntity<Void> =
		ResponseEntity.badRequest().build()
}
