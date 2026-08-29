@file:JvmName("SpecificHostRateLimitInterceptorKt")
package eu.kanade.tachiyomi.network.interceptor

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toDuration
import kotlin.time.toDurationUnit

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
    val host = try { url.toHttpUrlOrNull()?.host ?: url } catch (_: Exception) { url }
    return addInterceptor(RateLimitInterceptor(host, permits, period.toDuration(unit.toDurationUnit())))
}

fun OkHttpClient.Builder.rateLimitHost(
    url: String,
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder {
    val host = try { url.toHttpUrlOrNull()?.host ?: url } catch (_: Exception) { url }
    return addInterceptor(RateLimitInterceptor(host, permits, period))
}

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
    val h = try { this.toHttpUrlOrNull()?.host ?: this } catch (_: Exception) { this }
    return builder.addInterceptor(RateLimitInterceptor(h, permits, period.toDuration(unit.toDurationUnit())))
}

fun String.rateLimitHost(
    builder: OkHttpClient.Builder,
    permits: Int,
    period: Duration = 1.seconds,
): OkHttpClient.Builder {
    val h = try { this.toHttpUrlOrNull()?.host ?: this } catch (_: Exception) { this }
    return builder.addInterceptor(RateLimitInterceptor(h, permits, period))
}
