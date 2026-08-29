package eu.kanade.tachiyomi.network

import android.content.Context
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.RetryInterceptor
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class NetworkHelper(private val context: Context) {

    val cookieJar = AndroidCookieJar()

    private val userAgent =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .cache(Cache(File(context.cacheDir, "network_cache"), 5L * 1024 * 1024))
        .addInterceptor(UncaughtExceptionInterceptor())
        .addInterceptor(UserAgentInterceptor(::defaultUserAgentProvider))
        .addInterceptor(RetryInterceptor())
        .addInterceptor(CloudflareInterceptor(context, cookieJar, ::defaultUserAgentProvider))
        .build()

    @Suppress("unused")
    val cloudflareClient: OkHttpClient = client

    fun defaultUserAgentProvider() = userAgent
}
