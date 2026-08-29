@file:JvmName("SpecificHostRateLimitInterceptor")
package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toDuration
import kotlin.time.toDurationUnit

// New API (Builder receiver) -> generates static methods in class SpecificHostRateLimitInterceptor
fun OkHttpClient.Builder.rateLimitHost(
    httpUrl: HttpUrl,
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
): OkHttpClient.Builder = addInterceptor(RateLimitInterceptor(httpUrl.host, permits, period.toDuration(unit.toDurationUnit())))

fun OkHttpClient.Builder.rateLimitHost(
    httpUrl: HttpUrl,
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder = addInterceptor(RateLimitInterceptor(httpUrl.host, permits, period))

fun OkHttpClient.Builder.rateLimitHost(
    url: String,
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
): OkHttpClient.Builder {
    val host = try { HttpUrl.get(url).host } catch (_: Exception) { url }
    return addInterceptor(RateLimitInterceptor(host, permits, period.toDuration(unit.toDurationUnit())))
}

fun OkHttpClient.Builder.rateLimitHost(
    url: String,
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder {
    val host = try { HttpUrl.get(url).host } catch (_: Exception) { url }
    return addInterceptor(RateLimitInterceptor(host, permits, period))
}

// Reverse order overloads for old extensions that expect (HttpUrl, Builder) order
fun HttpUrl.rateLimitHost(
    builder: OkHttpClient.Builder,
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
): OkHttpClient.Builder = builder.addInterceptor(RateLimitInterceptor(host, permits, period.toDuration(unit.toDurationUnit())))

fun HttpUrl.rateLimitHost(
    builder: OkHttpClient.Builder,
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder = builder.addInterceptor(RateLimitInterceptor(host, permits, period))

fun String.rateLimitHost(
    builder: OkHttpClient.Builder,
    permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
): OkHttpClient.Builder {
    val h = try { HttpUrl.get(this).host } catch (_: Exception) { this }
    return builder.addInterceptor(RateLimitInterceptor(h, permits, period.toDuration(unit.toDurationUnit())))
}

fun String.rateLimitHost(
    builder: OkHttpClient.Builder,
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder {
    val h = try { HttpUrl.get(this).host } catch (_: Exception) { this }
    return builder.addInterceptor(RateLimitInterceptor(h, permits, period))
}

// Provide class-based shims for extensions that call SpecificHostRateLimitInterceptor.rateLimitHost as static factory returning interceptor
object SpecificHostRateLimitInterceptorCompat {
    @JvmStatic
    fun rateLimitHost(host: String, permits: Int, period: Long, unit: TimeUnit): RateLimitInterceptor {
        return RateLimitInterceptor(host, permits, period.toDuration(unit.toDurationUnit()))
    }
    @JvmStatic
    fun rateLimitHost(url: HttpUrl, permits: Int, period: Duration): RateLimitInterceptor {
        return RateLimitInterceptor(url.host, permits, period)
    }
    @JvmStatic
    fun rateLimitHost(httpUrl: HttpUrl, permits: Int, period: Long, unit: TimeUnit): RateLimitInterceptor {
        return RateLimitInterceptor(httpUrl.host, permits, period.toDuration(unit.toDurationUnit()))
    }
    @JvmStatic
    fun rateLimitHost(host: String, permits: Int, period: Duration): RateLimitInterceptor {
        return RateLimitInterceptor(host, permits, period)
    }
}
