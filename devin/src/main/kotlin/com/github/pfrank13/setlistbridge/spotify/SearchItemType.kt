package com.github.pfrank13.setlistbridge.spotify

import com.fasterxml.jackson.annotation.JsonValue

enum class SearchItemType(@JsonValue val value: String) {
	ALBUM("album"),
	ARTIST("artist"),
	PLAYLIST("playlist"),
	TRACK("track"),
	SHOW("show"),
	EPISODE("episode"),
	AUDIOBOOK("audiobook"),
	;

	override fun toString(): String = value
}
