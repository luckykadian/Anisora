package okhttp3.internal.http

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class CompressionInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
