package eu.kanade.tachiyomi.animesource.model

import kotlinx.serialization.Serializable
import okhttp3.Headers

@Serializable
data class Track(val url: String, val lang: String)

@Serializable
enum class ChapterType {
    Opening, Ending, Recap, MixedOp, Other,
}

@Serializable
data class TimeStamp(
    val start: Double,
    val end: Double,
    val name: String = "",
    val type: ChapterType = ChapterType.Other,
)

/**
 * Video stream from an extension. Multiple constructors match the signatures
 * extensions compiled against extensions-lib 14–16 actually call.
 */
open class Video(
    val url: String,
    val quality: String,
    var videoUrl: String? = null,
    val headers: Headers? = null,
    val subtitleTracks: List<Track> = emptyList(),
    val audioTracks: List<Track> = emptyList(),
) {
    var bytesCopied: Long = 0
    var totalBytes: Long = 0
    var internalData: String = ""
    var timestamps: List<TimeStamp> = emptyList()

    constructor(
        url: String,
        quality: String,
        videoUrl: String?,
        headers: Headers?,
    ) : this(url, quality, videoUrl, headers, emptyList(), emptyList())

    constructor(
        url: String,
        quality: String,
        videoUrl: String?,
        headers: Headers?,
        subtitleTracks: List<Track>,
    ) : this(url, quality, videoUrl, headers, subtitleTracks, emptyList())

    fun playUrl(): String = if (!videoUrl.isNullOrBlank()) videoUrl!! else url
}
