package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.awaitSingle
import rx.Observable

interface MangaSource {
    val id: Long
    val name: String
    val lang: String get() = ""

    @Deprecated("Use the non-RxJava API instead")
    fun fetchMangaDetails(manga: SManga): Observable<SManga> = throw IllegalStateException("Not used")

    @Deprecated("Use the non-RxJava API instead")
    fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = throw IllegalStateException("Not used")

    @Deprecated("Use the non-RxJava API instead")
    fun fetchPageList(chapter: SChapter): Observable<List<Page>> = throw IllegalStateException("Not used")

    suspend fun getMangaDetails(manga: SManga): SManga {
        @Suppress("DEPRECATION")
        return fetchMangaDetails(manga).awaitSingle()
    }

    suspend fun getChapterList(manga: SManga): List<SChapter> {
        @Suppress("DEPRECATION")
        return fetchChapterList(manga).awaitSingle()
    }

    suspend fun getPageList(chapter: SChapter): List<Page> {
        @Suppress("DEPRECATION")
        return fetchPageList(chapter).awaitSingle()
    }
}
