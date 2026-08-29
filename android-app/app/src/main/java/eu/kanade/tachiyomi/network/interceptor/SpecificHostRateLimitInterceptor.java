package eu.kanade.tachiyomi.network.interceptor;

import java.util.concurrent.TimeUnit;
import kotlin.time.Duration;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * Compatibility class for older extensions that expect
 * eu.kanade.tachiyomi.network.interceptor.SpecificHostRateLimitInterceptor
 * to contain static rateLimitHost methods (instead of SpecificHostRateLimitInterceptorKt).
 *
 * This class provides all known signatures, including reverse-order (HttpUrl, Builder)
 * that some extensions compiled against.
 */
public final class SpecificHostRateLimitInterceptor {
    private SpecificHostRateLimitInterceptor() {}

    // Builder first, HttpUrl second (new API)
    public static OkHttpClient.Builder rateLimitHost(OkHttpClient.Builder builder, HttpUrl httpUrl, int permits, long period, TimeUnit unit) {
        return builder.addInterceptor(new RateLimitInterceptor(httpUrl.host(), permits, period, unit));
    }

    public static OkHttpClient.Builder rateLimitHost(OkHttpClient.Builder builder, HttpUrl httpUrl, int permits, Duration period) {
        // Duration is inline value class - try to extract millis via getInWholeMilliseconds()
        long millis = 1000L;
        try {
            millis = period.getInWholeMilliseconds();
        } catch (Throwable ignored) {
            try {
                // fallback via toString parsing or default
                millis = 1000L;
            } catch (Throwable ignored2) {}
        }
        return builder.addInterceptor(new RateLimitInterceptor(httpUrl.host(), permits, millis, TimeUnit.MILLISECONDS));
    }

    public static OkHttpClient.Builder rateLimitHost(OkHttpClient.Builder builder, String url, int permits, long period, TimeUnit unit) {
        String host = url;
        try {
            HttpUrl parsed = HttpUrl.parse(url);
            if (parsed != null) host = parsed.host();
        } catch (Exception ignored) {}
        return builder.addInterceptor(new RateLimitInterceptor(host, permits, period, unit));
    }

    public static OkHttpClient.Builder rateLimitHost(OkHttpClient.Builder builder, String url, int permits, Duration period) {
        String host = url;
        try {
            HttpUrl parsed = HttpUrl.parse(url);
            if (parsed != null) host = parsed.host();
        } catch (Exception ignored) {}
        long millis = 1000L;
        try {
            millis = period.getInWholeMilliseconds();
        } catch (Throwable ignored) {}
        return builder.addInterceptor(new RateLimitInterceptor(host, permits, millis, TimeUnit.MILLISECONDS));
    }

    // Reverse order: HttpUrl first, Builder second (old extensions)
    public static OkHttpClient.Builder rateLimitHost(HttpUrl httpUrl, OkHttpClient.Builder builder, int permits, long period, TimeUnit unit) {
        return rateLimitHost(builder, httpUrl, permits, period, unit);
    }

    public static OkHttpClient.Builder rateLimitHost(HttpUrl httpUrl, OkHttpClient.Builder builder, int permits, Duration period) {
        return rateLimitHost(builder, httpUrl, permits, period);
    }

    public static OkHttpClient.Builder rateLimitHost(String url, OkHttpClient.Builder builder, int permits, long period, TimeUnit unit) {
        return rateLimitHost(builder, url, permits, period, unit);
    }

    public static OkHttpClient.Builder rateLimitHost(String url, OkHttpClient.Builder builder, int permits, Duration period) {
        return rateLimitHost(builder, url, permits, period);
    }

    // Factory versions returning interceptor (some old code called these)
    public static RateLimitInterceptor rateLimitHost(HttpUrl httpUrl, int permits, long period, TimeUnit unit) {
        return new RateLimitInterceptor(httpUrl.host(), permits, period, unit);
    }

    public static RateLimitInterceptor rateLimitHost(HttpUrl httpUrl, int permits, Duration period) {
        long millis = 1000L;
        try { millis = period.getInWholeMilliseconds(); } catch (Throwable ignored) {}
        return new RateLimitInterceptor(httpUrl.host(), permits, millis, TimeUnit.MILLISECONDS);
    }

    public static RateLimitInterceptor rateLimitHost(String host, int permits, long period, TimeUnit unit) {
        return new RateLimitInterceptor(host, permits, period, unit);
    }

    public static RateLimitInterceptor rateLimitHost(String host, int permits, Duration period) {
        long millis = 1000L;
        try { millis = period.getInWholeMilliseconds(); } catch (Throwable ignored) {}
        return new RateLimitInterceptor(host, permits, millis, TimeUnit.MILLISECONDS);
    }
}
