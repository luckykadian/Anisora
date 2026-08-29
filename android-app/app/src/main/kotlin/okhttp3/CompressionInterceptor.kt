package okhttp3

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Stub for OkHttp 5.x CompressionInterceptor.
 * Older Aniyomi manga extensions reference this class directly.
 * We provide a no-op pass-through to avoid ClassNotFound / NoClassDefFound.
 * Gzip is already handled by OkHttp's BridgeInterceptor.
 */
class CompressionInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }

    companion object {
        @JvmStatic
        fun create(): CompressionInterceptor = CompressionInterceptor()
    }
}

// Also provide okhttp3.internal.http variant that some extensions may reference via reflection
