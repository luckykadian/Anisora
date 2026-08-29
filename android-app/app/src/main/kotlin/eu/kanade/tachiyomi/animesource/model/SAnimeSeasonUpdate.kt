package eu.kanade.tachiyomi.animesource.model

data class SAnimeSeasonUpdate(
    val anime: SAnime,
    val seasons: List<SAnime>,
)
