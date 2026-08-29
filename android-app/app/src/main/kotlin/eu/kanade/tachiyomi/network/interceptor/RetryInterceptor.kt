package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: IOException? = null
        var response: Response? = null
        var attempt = 0
        val maxAttempts = 4
        while (attempt < maxAttempts) {
            try {
                response?.close()
                response = chain.proceed(chain.request())
                if (response.code != 429) {
                    return response
                }
                // 429 -> retry with backoff
                val retryAfter = response.header("Retry-After")?.toLongOrNull()
                val backoff = retryAfter?.let { it * 1000L } ?: (500L * (1 shl attempt) + (Math.random() * 500).toLong())
                response.close()
                Thread.sleep(backoff.coerceAtMost(8000L))
                attempt++
                continue
            } catch (e: IOException) {
                lastException = e
                attempt++
                if (attempt >= maxAttempts) break
                Thread.sleep((500L * (1 shl attempt)))
            }
        }
        response?.let { return it }
        throw lastException ?: IOException("Retry failed after $maxAttempts attempts")
    }
}
