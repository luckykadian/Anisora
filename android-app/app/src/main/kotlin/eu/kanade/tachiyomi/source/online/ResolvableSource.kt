package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.MangaSource
import eu.kanade.tachiyomi.source.model.SManga

interface ResolvableSource : MangaSource {
    suspend fun getManga(url: String): SManga? = null
}
