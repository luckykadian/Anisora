package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import eu.kanade.tachiyomi.network.AndroidCookieJar
import okhttp3.Interceptor
import okhttp3.Response

/** Pass-through. A full CF challenge solver needs a WebView; sources that don't trip CF still work. */
class CloudflareInterceptor(
    @Suppress("UNUSED_PARAMETER") context: Context,
    @Suppress("UNUSED_PARAMETER") cookieJar: AndroidCookieJar,
    @Suppress("UNUSED_PARAMETER") userAgentProvider: () -> String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
