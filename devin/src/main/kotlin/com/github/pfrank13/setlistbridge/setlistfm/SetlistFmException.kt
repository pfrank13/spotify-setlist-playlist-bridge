package com.github.pfrank13.setlistbridge.setlistfm

/**
 * Thrown when a call to the setlist.fm API fails.
 */
class SetlistFmException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
