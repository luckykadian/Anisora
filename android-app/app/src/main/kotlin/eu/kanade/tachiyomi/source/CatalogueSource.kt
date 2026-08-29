package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import rx.Observable
import tachiyomi.core.common.util.lang.awaitSingle

interface CatalogueSource : MangaSource {
    override val lang: String
    val supportsLatest: Boolean

    suspend fun getPopularManga(page: Int): MangasPage {
        @Suppress("DEPRECATION")
        return fetchPopularManga(page).awaitSingle()
    }

    suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        @Suppress("DEPRECATION")
        return fetchSearchManga(page, query, filters).awaitSingle()
    }

    suspend fun getLatestUpdates(page: Int): MangasPage {
        @Suppress("DEPRECATION")
        return fetchLatestUpdates(page).awaitSingle()
    }

    fun getFilterList(): FilterList = FilterList()

    @Deprecated("Use the non-RxJava API instead")
    fun fetchPopularManga(page: Int): Observable<MangasPage>

    @Deprecated("Use the non-RxJava API instead")
    fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage>

    @Deprecated("Use the non-RxJava API instead")
    fun fetchLatestUpdates(page: Int): Observable<MangasPage>
}
