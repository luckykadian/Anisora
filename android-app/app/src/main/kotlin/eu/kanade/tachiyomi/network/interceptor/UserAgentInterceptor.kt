package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor(private val userAgentProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        return if (original.header("User-Agent").isNullOrEmpty()) {
            chain.proceed(original.newBuilder().header("User-Agent", userAgentProvider()).build())
        } else {
            chain.proceed(original)
        }
    }
}
