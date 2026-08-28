package eu.kanade.tachiyomi.animesource.model

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

open class Hoster(
    val hosterUrl: String = "",
    val hosterName: String = "",
    val videoList: List<Video>? = null,
    val internalData: String = "",
    val lazy: Boolean = false,
    val memo: JsonObject = JsonObject.EMPTY,
) {
    @Transient
    @Volatile
    var status: State = State.IDLE

    enum class State { IDLE, LOADING, READY, ERROR }
}
