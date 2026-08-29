package eu.kanade.tachiyomi.network.interceptor;

import java.util.concurrent.TimeUnit;
import kotlin.time.Duration;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * Shim for extensions compiled against newer extension-lib that expects
 * eu.kanade.tachiyomi.network.interceptor.SpecificHostRateLimitInterceptorKt
 * (without @JvmName). Our Kotlin file uses @JvmName("SpecificHostRateLimitInterceptor"),
 * so we provide this Java forwarder.
 */
public final class SpecificHostRateLimitInterceptorKt {
    private SpecificHostRateLimitInterceptorKt() {}

    public static OkHttpClient.Builder rateLimitHost(OkHttpClient.Builder builder, HttpUrl httpUrl, int permits, long period, TimeUnit unit) {
        return SpecificHostRateLimitInterceptor.rateLimitHost(builder, httpUrl, permits, period, unit);
    }

    public static OkHttpClient.Builder rateLimitHost(OkHttpClient.Builder builder, HttpUrl httpUrl, int permits, Duration period) {
        return SpecificHostRateLimitInterceptor.rateLimitHost(builder, httpUrl, permits, period);
    }

    public static OkHttpClient.Builder rateLimitHost(OkHttpClient.Builder builder, String url, int permits, long period, TimeUnit unit) {
        return SpecificHostRateLimitInterceptor.rateLimitHost(builder, url, permits, period, unit);
    }

    public static OkHttpClient.Builder rateLimitHost(OkHttpClient.Builder builder, String url, int permits, Duration period) {
        return SpecificHostRateLimitInterceptor.rateLimitHost(builder, url, permits, period);
    }

    // Reverse order signatures that some old extensions might expect (HttpUrl first, Builder second)
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
}
