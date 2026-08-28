package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

abstract class ParsedAnimeHttpSource : AnimeHttpSource() {

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val animes = document.select(popularAnimeSelector()).map { popularAnimeFromElement(it) }
        val hasNextPage = popularAnimeNextPageSelector()?.let { document.select(it).first() } != null
        return AnimesPage(animes, hasNextPage)
    }

    protected abstract fun popularAnimeSelector(): String
    protected abstract fun popularAnimeFromElement(element: Element): SAnime
    protected abstract fun popularAnimeNextPageSelector(): String?

    override fun searchAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val animes = document.select(searchAnimeSelector()).map { searchAnimeFromElement(it) }
        val hasNextPage = searchAnimeNextPageSelector()?.let { document.select(it).first() } != null
        return AnimesPage(animes, hasNextPage)
    }

    protected abstract fun searchAnimeSelector(): String
    protected abstract fun searchAnimeFromElement(element: Element): SAnime
    protected abstract fun searchAnimeNextPageSelector(): String?

    override fun latestUpdatesParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val animes = document.select(latestUpdatesSelector()).map { latestUpdatesFromElement(it) }
        val hasNextPage = latestUpdatesNextPageSelector()?.let { document.select(it).first() } != null
        return AnimesPage(animes, hasNextPage)
    }

    protected abstract fun latestUpdatesSelector(): String
    protected abstract fun latestUpdatesFromElement(element: Element): SAnime
    protected abstract fun latestUpdatesNextPageSelector(): String?

    override fun animeDetailsParse(response: Response): SAnime = animeDetailsParse(response.asJsoup())
    protected abstract fun animeDetailsParse(document: Document): SAnime

    override fun episodeListParse(response: Response): List<SEpisode> {
        return response.asJsoup().select(episodeListSelector()).map { episodeFromElement(it) }
    }

    protected abstract fun episodeListSelector(): String
    protected abstract fun episodeFromElement(element: Element): SEpisode

    override fun videoListParse(response: Response): List<Video> = videoListParse(response.asJsoup())
    protected open fun videoListParse(document: Document): List<Video> = throw UnsupportedOperationException()

    override fun videoUrlParse(response: Response): String = videoUrlParse(response.asJsoup())
    protected open fun videoUrlParse(document: Document): String = throw UnsupportedOperationException()
}
