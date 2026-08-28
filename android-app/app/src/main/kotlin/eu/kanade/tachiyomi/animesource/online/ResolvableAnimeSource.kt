package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime

interface ResolvableAnimeSource : AnimeSource {
    suspend fun getAnime(url: String): SAnime? = null
}
