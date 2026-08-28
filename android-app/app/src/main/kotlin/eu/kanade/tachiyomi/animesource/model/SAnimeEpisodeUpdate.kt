package eu.kanade.tachiyomi.animesource.model

data class SAnimeEpisodeUpdate(
    val anime: SAnime,
    val episodes: List<SEpisode>,
)
