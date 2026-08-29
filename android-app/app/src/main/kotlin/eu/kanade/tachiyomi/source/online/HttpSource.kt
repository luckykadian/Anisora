package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import tachiyomi.core.common.util.lang.awaitSingle
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

abstract class HttpSource : CatalogueSource {

    protected val network: NetworkHelper by injectLazy()

    abstract val baseUrl: String

    override val supportsLatest: Boolean get() = true

    open val versionId: Int = 1

    override val id: Long by lazy { generateId(name, lang, versionId) }

    val headers: Headers by lazy { headersBuilder().build() }

    open val client: OkHttpClient get() = network.client

    protected fun generateId(name: String, lang: String, versionId: Int): Long {
        val key = "${name.lowercase()}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    protected open fun headersBuilder(): Headers.Builder = Headers.Builder().apply {
        add("User-Agent", network.defaultUserAgentProvider())
    }

    override fun toString(): String = "$name (${lang.uppercase()})"

    @Deprecated("Use the suspend API instead")
    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return client.newCall(popularMangaRequest(page)).asObservableSuccess().map { popularMangaParse(it) }
    }

    protected open fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException()
    protected open fun popularMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()

    @Deprecated("Use the suspend API instead")
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return client.newCall(searchMangaRequest(page, query, filters)).asObservableSuccess().map { searchMangaParse(it) }
    }

    protected open fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        throw UnsupportedOperationException()
    protected open fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()

    @Deprecated("Use the suspend API instead")
    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> {
        return client.newCall(latestUpdatesRequest(page)).asObservableSuccess().map { latestUpdatesParse(it) }
    }

    protected open fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    protected open fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override suspend fun getMangaDetails(manga: SManga): SManga {
        @Suppress("DEPRECATION")
        return fetchMangaDetails(manga).awaitSingle()
    }

    @Deprecated("Use the suspend API instead")
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsRequest(manga)).asObservableSuccess()
            .map { mangaDetailsParse(it).apply { initialized = true } }
    }

    open fun mangaDetailsRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)
    protected open fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        @Suppress("DEPRECATION")
        return fetchChapterList(manga).awaitSingle()
    }

    @Deprecated("Use the suspend API instead")
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return client.newCall(chapterListRequest(manga)).asObservableSuccess().map { chapterListParse(it) }
    }

    protected open fun chapterListRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)
    protected open fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        @Suppress("DEPRECATION")
        return fetchPageList(chapter).awaitSingle()
    }

    @Deprecated("Use the suspend API instead")
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter)).asObservableSuccess().map { pageListParse(it) }
    }

    protected open fun pageListRequest(chapter: SChapter): Request = GET(baseUrl + chapter.url, headers)
    protected open fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException()

    open suspend fun getImageUrl(page: Page): String {
        @Suppress("DEPRECATION")
        return fetchImageUrl(page).awaitSingle()
    }

    @Deprecated("Use the suspend API instead")
    open fun fetchImageUrl(page: Page): Observable<String> {
        return client.newCall(imageUrlRequest(page)).asObservableSuccess().map { imageUrlParse(it) }
    }

    protected open fun imageUrlRequest(page: Page): Request = GET(page.url, headers)
    protected open fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    open suspend fun getImage(page: Page): Response {
        return client.newCachelessCallWithProgress(imageRequest(page), page).awaitSuccess()
    }

    protected open fun imageRequest(page: Page): Request = GET(page.imageUrl!!, headers)

    fun SChapter.setUrlWithoutDomain(url: String) { this.url = getUrlWithoutDomain(url) }
    fun SManga.setUrlWithoutDomain(url: String) { this.url = getUrlWithoutDomain(url) }

    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig)
            var out = uri.path
            if (uri.query != null) out += "?" + uri.query
            if (uri.fragment != null) out += "#" + uri.fragment
            out
        } catch (_: URISyntaxException) {
            orig
        }
    }

    open fun getMangaUrl(manga: SManga): String = mangaDetailsRequest(manga).url.toString()
    open fun getChapterUrl(chapter: SChapter): String = chapter.url

    @Deprecated("All modifications should be done when constructing the chapter")
    open fun prepareNewChapter(chapter: SChapter, manga: SManga) {}
}
