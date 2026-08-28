package eu.kanade.tachiyomi.network

import android.content.Context

/** Stub used by a few sources. Real JS eval needs a WebView; return empty for now. */
class JavaScriptEngine(@Suppress("UNUSED_PARAMETER") context: Context) {
    suspend fun evaluate(script: String): String = ""
}
