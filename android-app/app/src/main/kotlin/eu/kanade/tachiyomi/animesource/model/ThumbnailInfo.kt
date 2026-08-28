package eu.kanade.tachiyomi.animesource.model

data class ThumbnailInfo(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val columns: Int = 0,
    val rows: Int = 0,
    val interval: Double = 0.0,
)
